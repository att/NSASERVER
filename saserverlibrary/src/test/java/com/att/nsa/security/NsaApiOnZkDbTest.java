/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import java.security.Key;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

import org.junit.Ignore;
import org.junit.Test;

import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.configs.confimpl.EncryptingLayer;
import com.att.nsa.configs.confimpl.ZkConfigDb;
import com.att.nsa.security.db.BaseNsaApiDbImpl;
import com.att.nsa.security.db.EncryptingApiDbImpl;
import com.att.nsa.security.db.NsaApiDb.KeyExistsException;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.att.nsa.security.db.simple.NsaSimpleApiKeyFactory;

@Ignore
public class NsaApiOnZkDbTest extends TestCase
{
	@Test
	public void testDbStorage () throws KeyExistsException, ConfigDbException
	{
		final ZkConfigDb cdb = new ZkConfigDb ( "localhost", "/fe3c" );
		final BaseNsaApiDbImpl<NsaSimpleApiKey> apiDb = new BaseNsaApiDbImpl<NsaSimpleApiKey> ( cdb, new NsaSimpleApiKeyFactory () );		

		{
			final NsaSimpleApiKey key = apiDb.createApiKey ( "123", "456" );
			key.set ( "foo", "bar" );
			apiDb.saveApiKey ( key );
		}

		{
			final NsaSimpleApiKey key = apiDb.loadApiKey ( "123" );
			assertEquals ( "bar", key.get ( "foo" ) );
		}

		{
			apiDb.deleteApiKey ( "123" );
			assertNull ( apiDb.loadApiKey ( "123" ) );
		}
	}

	@Test
	public void testDbEncStorage () throws KeyExistsException, ConfigDbException, NoSuchAlgorithmException
	{
		final byte[] kIv = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

		final String base64Key = EncryptingLayer.createSecretKey ();

		final ZkConfigDb cdb = new ZkConfigDb ( "localhost", "/fe3c" );

		final Key dbkey = EncryptingLayer.readSecretKey ( base64Key );
		final BaseNsaApiDbImpl<NsaSimpleApiKey> apiDb = new EncryptingApiDbImpl<NsaSimpleApiKey> ( cdb, new NsaSimpleApiKeyFactory (), dbkey, kIv );		

		{
			final NsaSimpleApiKey key = apiDb.createApiKey ( "123", "456" );
			key.set ( "foo", "bar" );
			apiDb.saveApiKey ( key );
		}

		{
			final NsaSimpleApiKey key = apiDb.loadApiKey ( "123" );
			assertEquals ( "bar", key.get ( "foo" ) );
		}

		{
			apiDb.deleteApiKey ( "123" );
			assertNull ( apiDb.loadApiKey ( "123" ) );
		}
	}
}
