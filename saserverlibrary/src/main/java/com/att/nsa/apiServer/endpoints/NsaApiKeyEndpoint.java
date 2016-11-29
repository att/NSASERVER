/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer.endpoints;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.att.nsa.apiServer.util.Emailer;
import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import com.att.nsa.drumlin.till.data.uniqueStringGenerator;
import com.att.nsa.drumlin.util.JsonBodyReader;
import com.att.nsa.security.ReadWriteSecuredResource.AccessDeniedException;
import com.att.nsa.security.db.NsaApiDb;
import com.att.nsa.security.db.NsaApiDb.KeyExistsException;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;

public class NsaApiKeyEndpoint extends NsaBaseEndpoint
{
	/**
	 * Get all the API keys, with some descriptive information. This is an open
	 * query, so we don't want to expose anything confidential (e.g. the secret key)
	 * @param ctx
	 * @throws IOException
	 * @throws AccessDeniedException 
	 */
	public static void getAllApiKeys(DrumlinRequestContext ctx) throws IOException, ConfigDbException, AccessDeniedException
	{
		// there's no reason for anyone but an admin to do this. It's also potentially
		// taxing on the back-end system (e.g. ZK)
		NsaBaseEndpoint.adminAuthenticate ( ctx );

		final NsaApiDb<NsaSimpleApiKey> apiDb = getApiKeyDb ( ctx );

		final JSONObject result = new JSONObject ();
		final JSONArray keys = new JSONArray ();
		result.put ( "apiKeys", keys );

		for ( String key : apiDb.loadAllKeys () )
		{
			keys.put ( key );
		}
		
		respondOk ( ctx, result );
	}

	public static void getApiKey ( DrumlinRequestContext ctx, String apiKeyName ) throws IOException, ConfigDbException
	{
		final NsaApiDb<NsaSimpleApiKey> apiDb = getApiKeyDb ( ctx );

		final NsaSimpleApiKey key = apiDb.loadApiKey ( apiKeyName );
		if ( key == null )
		{
			respondWithErrorInJson ( ctx, HttpStatusCodes.k404_notFound, "No API key " + apiKeyName + "." );
			return;
		}

		respondOk ( ctx, key.asJsonObject () );
	}

	private static final String kSetting_AllowAnonymousKeys = "apiKeys.allowAnonymous";

	public static void createApiKey ( DrumlinRequestContext ctx ) throws IOException, AccessDeniedException, ConfigDbException
	{
		try
		{
			final JSONObject dataIn = readJsonBody ( ctx );
			final String contactEmail = dataIn.optString ( "email" );
			final String description = dataIn.optString ( "description" );

			final boolean emailProvided = contactEmail != null && contactEmail.length() > 0;
			if ( !ctx.getServlet ().getSettings ().getBoolean ( kSetting_AllowAnonymousKeys, false ) &&
				!emailProvided )
			{
				respondWithErrorInJson ( ctx, HttpStatusCodes.k400_badRequest, "You must provide an email address." );
				return;
			}

			final String keyString = generateKey ( 16 );
			final String sharedSecret = generateKey ( 24 );

			try
			{
				final NsaApiDb<NsaSimpleApiKey> apiDb = getApiKeyDb ( ctx );
				final NsaSimpleApiKey key = apiDb.createApiKey ( keyString, sharedSecret );

				if ( contactEmail != null ) key.setContactEmail ( contactEmail );
				if ( description != null ) key.setDescription ( description );
				apiDb.saveApiKey ( key );

				// email out the secret to validate the email address
				if ( emailProvided )
				{
					final String body = new StringBuilder ()
						.append ( "\n" )
						.append ( "Your email address was provided as the creator of new API key \"" )
						.append ( keyString )
						.append ( "\".\n" )
						.append ( "\n" )
						.append ( "If you did not make this request, please let us know. See http://sa2020.it.att.com:8888 for contact information, " )
						.append ( "but don't worry - the API key is useless without the information below, which has been provided only to you.\n" )
						.append ( "\n\n" )
						.append ( "For API key \"" )
						.append ( keyString )
						.append ( "\", use API key secret:\n\n\t" )
						.append ( sharedSecret )
						.append ( "\n\n" )
						.append ( "Note that it's normal to share the API key (" )
						.append ( keyString )
						.append ( "). This is how you are granted access to resources " )
						.append ( "like a UEB topic or Flatiron scope. However, you should NOT share the API key's secret. " )
						.append ( "The API key is associated with your email alone. ALL access to data made with this " )
						.append ( "key will be your responsibility. If you share the secret, someone else can use the API key " )
						.append ( "to access proprietary data with your identity.\n" )
						.append ( "\n" )
						.append ( "Enjoy!\n" )
						.append ( "\n" )
						.append ( "The GFP/SA-2020 Team" )
						.toString ();
					
					final Emailer em = getServlet ( ctx ).getSystemEmailer ();
					em.send ( contactEmail, "New API Key", body );
				}

				final JSONObject o = key.asJsonObject ();
				o.put ( NsaSimpleApiKey.kApiSecretField,
					emailProvided ?
						"Emailed to " + contactEmail + "." :
						key.getSecret ()
				);
				respondOk ( ctx, o );
			}
			catch ( KeyExistsException e )
			{
				// holy smokes. go play the lottery.
				respondWithErrorInJson ( ctx, HttpStatusCodes.k500_internalServerError, "Randomly created API key conflicts with existing key." );
			}
		}
		catch ( JSONException x )
		{
			respondWithErrorInJson ( ctx, HttpStatusCodes.k400_badRequest, "Couldn't parse your JSON" );
		}
	}

	public static void updateApiKey ( DrumlinRequestContext ctx, String apiKeyName ) throws IOException, AccessDeniedException, ConfigDbException
	{
		final NsaApiDb<NsaSimpleApiKey> apiDb = getApiKeyDb ( ctx );

		final NsaSimpleApiKey key = apiDb.loadApiKey ( apiKeyName );
		if ( key == null )
		{
			respondWithErrorInJson ( ctx, HttpStatusCodes.k404_notFound, "No API key named " + apiKeyName );
			return;
		}

		// update the existing api key if allowed
		final NsaSimpleApiKey user = getAuthenticatedUser ( ctx );
		if ( user == null || !user.getKey().equals ( key.getKey () ) )
		{
			throw new AccessDeniedException ( "You must authenticate with the key you'd like to update." );
		}

		// this user is okay to update the key. get the posted content.
		final JSONObject dataIn = JsonBodyReader.readBody ( ctx.request () );

		boolean updates = false;

		// NOTE! We cannot allow an email update to an API key, because the email address is 
		// validated at creation time and actions taken with the API key are associated
		// with the email address.

		final String description = dataIn.optString ( "description" );
		if ( description != null )
		{
			key.setDescription ( description );
			updates = true;
		}

		// other updates? perhaps last ack time?
		
		if ( updates )
		{
			apiDb.saveApiKey ( key );
		}
		
		respondOkNoContent ( ctx );
	}

	public static void deleteApiKey ( DrumlinRequestContext ctx, String apiKeyName ) throws IOException, AccessDeniedException, ConfigDbException
	{
		final NsaApiDb<NsaSimpleApiKey> apiDb = getApiKeyDb ( ctx );

		final NsaSimpleApiKey key = apiDb.loadApiKey ( apiKeyName );
		if ( key == null )
		{
			respondWithErrorInJson ( ctx, HttpStatusCodes.k404_notFound, "No API key named " + apiKeyName );
			return;
		}

		// delete the existing api key if allowed
		final NsaSimpleApiKey user = getAuthenticatedUser ( ctx );
		if ( user == null || !user.getKey().equals ( key.getKey () ) )
		{
			throw new AccessDeniedException ( "You don't own the API key." );
		}

		// delete it
		apiDb.deleteApiKey ( key );

		respondOkNoContent ( ctx );
	}

	private static String kKeyChars = "ABCDEFGHJIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static String generateKey ( int length  )
	{
		return uniqueStringGenerator.createKeyUsingAlphabet ( kKeyChars, length );
	}

	/**
	 * Get the API db
	 * @param ctx
	 * @return
	 */
	public static NsaApiDb<NsaSimpleApiKey> getApiKeyDb ( DrumlinRequestContext ctx )
	{
		return getServlet(ctx).getApiKeyDb ();
	}
}
