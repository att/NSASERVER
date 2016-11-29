/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

import java.security.Key;

import com.att.nsa.configs.ConfigDb;
import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.configs.confimpl.EncryptingLayer;
import com.att.nsa.security.NsaApiKey;

/**
 * An extension of the base API Key DB implementation that uses an encrypting layer above the 
 * provided config db. The provided db can be any implementation, this layer just adds
 * encryption to the values. Note that keys are visible, just values are encrypted.
 * 
 * @author peter
 *
 * @param <K>
 */
public class EncryptingApiDbImpl<K extends NsaApiKey> extends BaseNsaApiDbImpl<K>
{
	public static final String kEncAlgo = "AES/CBC/PKCS5Padding";
	
	/**
	 * Construct an encrypting wrapper around a given config db. The app's secret key and 
	 * initialization vector must be consistent across instantiations. Loss of either will
	 * prevent recovery of stored data.
	 * 
	 * @param db
	 * @param keyFactory
	 * @param appSecretKey  The secret key for this data. This must be an AES compatible key.
	 * @param iv The initializaton vector for the Cipher used in this implementation.
	 * @throws ConfigDbException
	 */
	public EncryptingApiDbImpl ( ConfigDb db, NsaApiKeyFactory<K> keyFactory, Key appSecretKey, byte[] iv ) throws ConfigDbException
	{
		super ( wrap ( db, appSecretKey, iv ), keyFactory );
	}

	/**
	 * Construct an encrypting wrapper around a given config db. The app's secret key and 
	 * initialization vector must be consistent across instantiations. Loss of either will
	 * prevent recovery of stored data.
	 * 
	 * @param db
	 * @param rootPath
	 * @param keyFactory
	 * @param appSecretKey  The secret key for this data. This must be an AES compatible key.
	 * @param iv The initializaton vector for the Cipher used in this implementation.
	 * @throws ConfigDbException
	 */
	public EncryptingApiDbImpl ( ConfigDb db, String rootPath, NsaApiKeyFactory<K> keyFactory, Key appSecretKey, byte[] iv ) throws ConfigDbException
	{
		super ( wrap ( db, appSecretKey, iv ), rootPath, keyFactory );
	}

	private static ConfigDb wrap ( ConfigDb storage, Key key, byte[] iv )
	{
		return new EncryptingLayer ( storage, kEncAlgo, key, iv );
	}
}
