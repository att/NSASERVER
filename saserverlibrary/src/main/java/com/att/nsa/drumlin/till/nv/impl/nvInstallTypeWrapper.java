/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.till.nv.rrNvReadable;

/**
 * This class acts as a wrapper around the basic rrNvReadable settings class, and it
 * provides the ability to make settings specific to an "installation type" (e.g.
 * debug, test, production).
 *  
 * @author peter
 *
 */
public class nvInstallTypeWrapper extends nvBaseReadable implements rrNvReadable
{
	public nvInstallTypeWrapper ( rrNvReadable actual )
	{
		fActual = actual;
		fKeys = new TreeSet<String> ();

		fThisUser = System.getProperty ( "user.name" );

		fSystemType = System.getProperty ( "sa.installation", null );
		if ( fSystemType != null )
		{
			LoggerFactory.getLogger ( nvInstallTypeWrapper.class ).info ( "sa.installation: " + fSystemType );
		}

		parseForKeys ();
	}

	@Override
	public int size ()
	{
		return fKeys.size ();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		return fKeys;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		final HashMap<String,String> map = new HashMap<String,String> ();
		for ( String key : fKeys )
		{
			map.put ( key, getString ( key, "" ) );
		}
		return map;
	}

	@Override
	public boolean hasValueFor ( String key )
	{
		return fKeys.contains ( key );
	}

	@Override
	public String getString ( String key ) throws missingReqdSetting
	{
		String result = null;

		// try keys from most specific to least

		if ( fSystemType != null && fThisUser != null )
		{
			final String keyToTry = ( key + "[" + fSystemType + "@" + fThisUser + "]" );
			result = fActual.getString ( keyToTry, null );
		}
		
		if ( result == null && fSystemType != null )
		{
			final String keyToTry = ( key + "[" + fSystemType + "]" );
			result = fActual.getString ( keyToTry, null );
		}

		if ( result == null && fThisUser != null )
		{
			final String keyToTry = ( key + "[@" + fThisUser + "]" );
			result = fActual.getString ( keyToTry, null );
		}

		if ( result == null )
		{
			result = fActual.getString ( key );
		}

		return result;
	}

	@Override
	public void rescan () throws loadException
	{
		super.rescan ();
		parseForKeys ();
	}

	private final rrNvReadable fActual;
	private final TreeSet<String> fKeys;
	private final String fSystemType;
	private final String fThisUser;

	private void parseForKeys ()
	{
		fKeys.clear ();
		for ( String key : fActual.getAllKeys () )
		{
			fKeys.add ( parse ( key ) );
		}
	}

	static String parse ( String key )
	{
		// format:
		//		plain
		//		plain[sysType]
		//		plain[@user]
		//		plain[sysType@user]
		key = key.trim ();
		if ( key.matches ( ".*\\[[^\\[\\]]+\\]" ) )
		{
			final int openBracket = key.indexOf ( '[' );
			key = key.substring ( 0, openBracket );
		}
		return key;
	}
}
