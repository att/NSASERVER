/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * A service for authenticating inbound requests.
 * @author peter
 *
 * @param <K>
 */
public class NsaAuthenticatorService<K extends NsaApiKey>
{
	/**
	 * Construct the security manager against an API key database with a specific request time window size
	 * @param requireSecureChannel if true, requests must be over a secure HTTPS channel
	 */
	public NsaAuthenticatorService ( boolean requireSecureChannel )
	{
		fAuthenticators = new LinkedList<NsaAuthenticator<K>> ();
		fRequireSecureChannel = requireSecureChannel;
	}

	/**
	 * Add an authenticator to this service.
	 * @param a
	 */
	public void addAuthenticator ( NsaAuthenticator<K> a )
	{
		fAuthenticators.add ( a );
	}
	
	/**
	 * Authenticate a user's request. This method returns the API key if the user is authentic, null otherwise.
	 * 
	 * @param ctx
	 * @return an api key record, or null
	 */
	public K authenticate ( DrumlinRequestContext ctx )
	{
		final DrumlinRequest req = ctx.request();

		// NOTE: this is important... if a user attempts to authenticate over an insecure channel,
		// the authentication request can potentially be spied on and replayed. Therefore, every
		// authentication must come in over a secure channel.
		if ( fRequireSecureChannel && !req.isSecure ()  )
		{
			log.debug ( "Authentication request over insecure channel automatically fails." );
			return null;
		}

		for ( NsaAuthenticator<K> a : fAuthenticators )
		{
			if ( a.qualify ( req ) )
			{
				final K k = a.isAuthentic ( req );
				if ( k != null ) return k;
			}
			// else: this request doesn't look right to any authenticator
		}
		return null;
	}

	// ultimately, this can go away and always be considered 'true', but we need 
	// some transition time on existing cambria systems
	private final boolean fRequireSecureChannel;
	private final LinkedList<NsaAuthenticator<K>> fAuthenticators;

	private static final Logger log = LoggerFactory.getLogger ( NsaAuthenticatorService.class );
}
