/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.util;

import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeloJsonObject
{
	public VeloJsonObject ( JSONObject o )
	{
		fObject = o;
	}

	public Object get ( String key )
	{
		try
		{
			final Object o = fObject.get ( key );
			if ( o instanceof JSONObject )
			{
				return new VeloJsonObject ( (JSONObject) o ); 
			}
			else if ( o != null )
			{
				return o.toString ();
			}
		}
		catch ( JSONException e )
		{
			log.info ( e.getMessage(), e );
		}
		return null;
	}

	public String getString ( String key, String defValue )
	{
		return fObject.optString ( key, defValue );
	}

	public boolean hasValueFor ( String key )
	{
		return fObject.has ( key );
	}

	@SuppressWarnings("unchecked")
	public Collection<String> getAllKeys ()
	{
		return fObject.keySet ();
	}

	private final JSONObject fObject;
	private static final Logger log = LoggerFactory.getLogger ( VeloJsonObject.class );
}
