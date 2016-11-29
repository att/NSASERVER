/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db.simple;

import org.json.JSONObject;

import com.att.nsa.data.json.SaJsonUtil;
import com.att.nsa.security.NsaApiKey;

/**
 * An API key record, which includes the API key, the API secret,
 * and any auxiliary data.
 * 
 * @author peter
 *
 */
public class NsaSimpleApiKey implements NsaApiKey
{
	public static final String kApiKeyField = "key";
	public static final String kApiSecretField = "secret";
	public static final String kApiEnabled = "enabled";

	public static final String kAuxData = "aux";
	public static final String kAuxEmail = "email";
	public static final String kAuxDescription = "description";

	public NsaSimpleApiKey ( JSONObject data )
	{
		fData = data;

		// check for required fields (these throw if not present)
		getKey ();
		getSecret ();

		// make sure we've got an aux data object
		final JSONObject aux = fData.optJSONObject ( kAuxData );
		if ( aux == null )
		{
			fData.put ( kAuxData, new JSONObject () );
		}
	}

	@Override
	public String toString ()
	{
		return getKey() + ": " + fData.toString ();
	}

	@Override
	public String serialize ()
	{
		return sfPrettyStore ? serializeAsJson().toString ( 4 ) : serializeAsJson().toString ();
	}

	public JSONObject serializeAsJson ()
	{
		return SaJsonUtil.clone ( fData );
	}
	
	public JSONObject asJsonObject ()
	{
		// always remove the secret from the generated json
		final JSONObject full = new JSONObject ( fData, JSONObject.getNames ( fData ) );
		full.remove ( kApiSecretField );
		return full;
	}

	public NsaSimpleApiKey ( String apiKey, String sharedSecret )
	{
		fData = new JSONObject ();
		fData.put ( kAuxData, new JSONObject () );

		fData.put ( kApiKeyField, apiKey);
		fData.put ( kApiSecretField, sharedSecret);
	}

	@Override
	public String getKey ()
	{
		return fData.getString ( kApiKeyField );
	}

	@Override
	public String getSecret ()
	{
		return fData.getString ( kApiSecretField );
	}

	/**
	 * This normally shouldn't be used, as clients would normally just create a new
	 * API key. However, accidents happen.
	 * @param sharedSecret
	 */
	public void resetSecret ( String sharedSecret )
	{
		fData.put ( kApiSecretField, sharedSecret);
	}

	public void enable ()
	{
		fData.put ( kApiEnabled, true );
	}

	public void disable ()
	{
		fData.put ( kApiEnabled, false );
	}

	public boolean enabled ()
	{
		return fData.optBoolean ( kApiEnabled, true );
	}

	public void setContactEmail ( String contactEmail )
	{
		set ( kAuxEmail, contactEmail );
	}

	public String getContactEmail ()
	{
		return get ( kAuxEmail, "" );
	}

	public void setDescription ( String description )
	{
		set ( kAuxDescription, description );
	}

	public String getDescription ()
	{
		return get ( kAuxDescription, "" );
	}

	@Override
	public void set ( String key, String val )
	{
		fData.getJSONObject ( kAuxData ).put ( key, val );
	}

	@Override
	public String get ( String key )
	{
		return get ( key, null );
	}

	@Override
	public String get ( String key, String defval )
	{
		return fData.getJSONObject ( kAuxData ).optString ( key, defval );
	}

	private final JSONObject fData;
	private static final boolean sfPrettyStore = Boolean.parseBoolean ( System.getProperty ( "configdb.pretty", "false" ) );
}
