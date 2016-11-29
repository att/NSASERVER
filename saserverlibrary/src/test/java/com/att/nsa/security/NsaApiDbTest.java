/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import junit.framework.TestCase;

import org.junit.Test;

import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.configs.confimpl.MemConfigDb;
import com.att.nsa.security.db.BaseNsaApiDbImpl;
import com.att.nsa.security.db.NsaApiDb.KeyExistsException;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.att.nsa.security.db.simple.NsaSimpleApiKeyFactory;

public class NsaApiDbTest extends TestCase
{
	@Test
	public void testDbStorage () throws KeyExistsException, ConfigDbException
	{
		final MemConfigDb cdb = new MemConfigDb ();
		final BaseNsaApiDbImpl<NsaSimpleApiKey> apiDb = new BaseNsaApiDbImpl<NsaSimpleApiKey> ( cdb, new NsaSimpleApiKeyFactory () );		

		{
			final NsaSimpleApiKey key = apiDb.createApiKey ( "123", "456" );
			key.set ( "foo", "bar" );
			apiDb.saveApiKey ( key );
		}

		{
			final NsaApiKey key = apiDb.loadApiKey ( "123" );
			assertEquals ( "bar", key.get ( "foo" ) );
		}

		{
			apiDb.deleteApiKey ( "123" );
			assertNull ( apiDb.loadApiKey ( "123" ) );
		}
	}
}
