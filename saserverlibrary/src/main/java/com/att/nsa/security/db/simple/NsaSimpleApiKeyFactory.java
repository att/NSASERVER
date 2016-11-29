/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db.simple;

import org.json.JSONObject;

import com.att.nsa.security.db.NsaApiKeyFactory;

/**
 * A factory for the simple API key implementation
 * @author peter
 *
 */
public class NsaSimpleApiKeyFactory implements NsaApiKeyFactory<NsaSimpleApiKey>
{
	@Override
	public NsaSimpleApiKey makeNewKey ( String key, String sharedSecret )
	{
		return new NsaSimpleApiKey ( key, sharedSecret );
	}

	@Override
	public NsaSimpleApiKey makeNewKey ( String serializedForm )
	{
		return new NsaSimpleApiKey ( new JSONObject ( serializedForm ) );
	}
}
