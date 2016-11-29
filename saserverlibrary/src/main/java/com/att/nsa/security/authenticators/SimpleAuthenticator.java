/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.authenticators;

import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.security.NsaAuthenticator;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.att.nsa.security.db.simple.NsaSimpleApiKeyFactory;

/**
 * Authenticates an HTTP Basic auth request against explicitly added username/passwords.
 * @author peter
 *
 */
public class SimpleAuthenticator implements NsaAuthenticator<NsaSimpleApiKey>
{
	public SimpleAuthenticator add ( String user, String password )
	{
		fCreds.put ( user, password );
		return this;
	}

	@Override
	public boolean qualify ( DrumlinRequest req )
	{
		final String auth = req.getFirstHeader ( "Authorization" );
		return auth != null && auth.startsWith ( "Basic " );
	}

	@Override
	public NsaSimpleApiKey isAuthentic ( DrumlinRequest req )
	{
		final String auth = req.getFirstHeader ( "Authorization" ).substring ( "Basic ".length () );
		final String decoded = new String ( Base64.decodeBase64 ( auth.getBytes () ) );
		final int colon = decoded.indexOf ( ":" );
		if ( colon > -1 )
		{
			final String user = decoded.substring ( 0, colon );
			final String password = decoded.substring ( colon + 1 );
			final String lookup = fCreds.get ( user );
			if ( lookup != null && lookup.equals ( password ) )
			{
				final NsaSimpleApiKeyFactory f = new NsaSimpleApiKeyFactory ();
				return f.makeNewKey ( user, password );
			}
		}
		return null;
	}

	private final HashMap<String,String> fCreds = new HashMap<String,String> ();
}
