/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.authenticators;

import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

import org.junit.Test;

import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.configs.confimpl.MemConfigDb;
import com.att.nsa.drumlin.till.data.sha1HmacSigner;
import com.att.nsa.security.NsaApiKey;
import com.att.nsa.security.db.BaseNsaApiDbImpl;
import com.att.nsa.security.db.NsaApiDb.KeyExistsException;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.att.nsa.security.db.simple.NsaSimpleApiKeyFactory;

public class OriginalUebAuthenticatorTest extends TestCase
{
	@Test
	public void testAuth () throws KeyExistsException, ConfigDbException
	{
		final MemConfigDb cdb = new MemConfigDb ();
		final BaseNsaApiDbImpl<NsaSimpleApiKey> apiDb = new BaseNsaApiDbImpl<NsaSimpleApiKey> ( cdb, new NsaSimpleApiKeyFactory () );		

		{
			final NsaSimpleApiKey key = apiDb.createApiKey ( "123", "456" );
			apiDb.saveApiKey ( key );
		}

		final OriginalUebAuthenticator<NsaSimpleApiKey> mgr = new OriginalUebAuthenticator<NsaSimpleApiKey> ( apiDb, 1000*60*10 );

		NsaApiKey key = mgr.authenticate ( "localhost", null, "badXDate", "badDate", null );
		assertNull ( "no xauth", key );

		key = mgr.authenticate ( "localhost", "badXauth", "badXDate", "badDate", null );
		assertNull ( "bad xauth", key );

		key = mgr.authenticate ( "localhost", "nosuch:key", "badXDate", "badDate", null );
		assertNull ( "bad date", key );

		key = mgr.authenticate ( "localhost", "nosuch:key", sdf.format ( new Date() ), null, null );
		assertNull ( "no api key", key );

		key = mgr.authenticate ( "localhost", "123:xxx", sdf.format ( new Date() ), null, null );
		assertNull ( "bad signature", key );

		final String xDate = sdf.format(new Date());
		final String sig = sha1HmacSigner.sign ( xDate, "456" );
		key = mgr.authenticate ( "localhost", "123:" + sig, xDate, null, null );
		assertNotNull ( key );
	}

	@Test
	public void testDateParse () throws KeyExistsException, ConfigDbException
	{
		assertEquals ( 1434552360000L, parseDate ( "2015-06-17T10:46:00-0400" ) );
		assertEquals ( 1434552360000L, parseDate ( "2015-06-17T10:46:00-04:00" ) );
	}

	private long parseDate ( String s )
	{
		final Date d = OriginalUebAuthenticator.getClientDate ( s );
		return d == null ? -1 : d.getTime ();
	}
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd'T'HH:mm:ssz" );
}
