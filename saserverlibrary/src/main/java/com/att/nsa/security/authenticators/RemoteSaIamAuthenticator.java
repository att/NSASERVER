/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.authenticators;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.apiClient.http.CacheUse;
import com.att.nsa.apiClient.http.HttpClient;
import com.att.nsa.apiClient.http.HttpClient.ConnectionType;
import com.att.nsa.apiClient.http.HttpException;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.att.nsa.security.NsaApiKey;
import com.att.nsa.security.NsaAuthenticator;

/**
 * This authenticator handles contacts an authentication server for validation.
 * 
 * @author peter
 *
 * @param <K>
 */
public class RemoteSaIamAuthenticator<K extends NsaApiKey> implements NsaAuthenticator<K>
{
	private static final int kStandardIamPort = 33333;

	private static final String kSetting_IamServers = "iam.remote.servers";

	private static final String kSetting_IamCacheSize = "iam.remote.cacheSize";
	private static final int kDefault_IamCacheSize = 1024;

	private static final String kSetting_IamCacheMaxAgeSeconds = "iam.remote.cacheMaxAgeSeconds";
	private static final int kDefault_IamCacheMaxAgeSeconds = 60 * 15;

	private static final String kSetting_IamDisableCertCheck = "iam.remote.disableCertificateValidation";
	private static final boolean kDefault_IamDisableCertCheck = false;

	public interface ApiKeyFactory<K extends NsaApiKey>
	{
		K createApiKey ( JSONObject data );
	}

	public RemoteSaIamAuthenticator ( rrNvReadable settings, ApiKeyFactory<K> factory ) throws missingReqdSetting, MalformedURLException, GeneralSecurityException
	{
		fSettings = settings;
		fFactory = factory;

		final String[] iamServerList = fSettings.getStrings ( kSetting_IamServers );
		fServers = new LinkedList<String> ();
		fServers.addAll ( Arrays.asList ( iamServerList ) );

		ConnectionType ct = ConnectionType.HTTPS;
		if ( settings.getBoolean ( kSetting_IamDisableCertCheck, kDefault_IamDisableCertCheck ) )
		{
			ct = ConnectionType.HTTPS_NO_VALIDATION;
			log.warn ( "RemoteSaIamAuthenticator is running with HTTPS certificate validation disabled." );
		}

		fClient = new HttpClient ( ct, fServers, kStandardIamPort,
			UUID.randomUUID ().toString (),		// no session stickiness required, just load distribution
			CacheUse.FULL,
			fSettings.getInt ( kSetting_IamCacheSize, kDefault_IamCacheSize ),
			fSettings.getInt ( kSetting_IamCacheMaxAgeSeconds, kDefault_IamCacheMaxAgeSeconds ),
			TimeUnit.SECONDS
		);
	}

	@Override
	public boolean qualify ( DrumlinRequest req )
	{
		// accept anything that comes in with X-(Cambria)Auth in the header
		final String xAuth = getFirstHeader ( req, new String[]{ "X-CambriaAuth", "X-Auth" } );
		return xAuth != null;
	}

	@Override
	public K isAuthentic ( DrumlinRequest req )
	{
		try
		{
			final String remoteAddr = req.getRemoteAddress ();
			
			// Cambria originally used "Cambria..." headers, but as the API key system is now more
			// general, we take either form.
			final String xAuth = getFirstHeader ( req, new String[]{ "X-CambriaAuth", "X-Auth" } );
			final String xDate = getFirstHeader ( req, new String[]{ "X-CambriaDate", "X-Date" } );

			final String httpDate = req.getFirstHeader ( "Date" );

			final String xNonce = getFirstHeader ( req, new String[]{ "X-Nonce" } );
			return authenticate ( remoteAddr, xAuth, xDate, httpDate, xNonce );
		}
		catch ( GeneralSecurityException | IOException e )
		{
			log.warn ( "Authentication service problem: " + e.getMessage (), e );
			return null;
		}
	}

	/**
	 * Authenticate a user's request. This method returns the API key if the user is authentic, null otherwise.
	 * 
	 * @param ctx
	 * @return an api key record, or null
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	public K authenticate ( String remoteAddr, String xAuth, String xDate, String httpDate, String nonce ) throws GeneralSecurityException, IOException
	{
		final JSONObject authData = new JSONObject ();
		
		if ( xAuth == null )
		{
			authLog ( "No X-Auth header on request", remoteAddr );
			return null;
		}

		final String[] xAuthParts = xAuth.split ( ":");
		if ( xAuthParts.length != 2 )
		{
			authLog ( "Bad X-Auth header format (" + xAuth + ")", remoteAddr );
			return null;
		}

		// get the api key and signature
		authData.put ( "apiKey", xAuthParts[0] );
		authData.put ( "apiSignature", xAuthParts[1] );

		if ( xAuthParts[0].length () == 0 || xAuthParts[1].length() == 0 )
		{
			authLog ( "Bad X-Auth header format (" + xAuth + ")", remoteAddr );
			return null;
		}

		// if the user provided X-Date, use that. Otherwise, go for Date
		final String dateString = xDate != null ? xDate : httpDate;
		authData.put ( "date", dateString );

		if ( nonce != null )
		{
			authData.put ( "nonce", nonce );
		}

		try
		{
			final JSONObject response = fClient.post ( "/v1/iam/authenticate", authData, true );
			final K key = fFactory.createApiKey ( response );
			authLog ( "Remote IAM authenticated " + key.getKey(), remoteAddr );
			return key;
		}
		catch ( HttpException e )
		{
			authLog ( "Error with remote authentication: " + e.getMessage(), remoteAddr );
			return null;
		}
	}

	/**
	 * Get the first value of the first existing header from the headers list
	 * @param req
	 * @param headers
	 * @return a header value, or null if none exist
	 */
	private static String getFirstHeader ( DrumlinRequest req, String[] headers )
	{
		for ( String header : headers )
		{
			final String result = req.getFirstHeader ( header );
			if ( result != null ) return result;
		}
		return null;
	}

	private static void authLog ( String msg, String remoteAddr )
	{
		log.info ( "AUTH-LOG(" + remoteAddr + "): " + msg );
	}

	private final rrNvReadable fSettings;
	private final ApiKeyFactory<K> fFactory;
	private final LinkedList<String> fServers;
	private final HttpClient fClient;

	private static final Logger log = LoggerFactory.getLogger ( RemoteSaIamAuthenticator.class );
}
