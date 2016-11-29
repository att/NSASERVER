/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.cmdLine;

import java.util.HashMap;
import java.util.Map;

public class NsaCommandLineUtil
{
	/**
	 * Read a simplified command line format into map.
	 * @param args
	 * @return
	 */
	public static Map<String, String> processCmdLine ( String[] args )
	{
		return processCmdLine ( args, false );
	}

	/**
	 * Read a simplified command line format into map.
	 * @param args
	 * @param stripDashes if true, the returned map for "-foo=bar" would have "foo"="bar" 
	 * @return
	 */
	public static Map<String, String> processCmdLine ( String[] args, boolean stripDashes )
	{
		final HashMap<String,String> map = new HashMap<String,String> ();

		String lastKey = null;
		for ( String arg : args )
		{
			if ( arg.startsWith ( "-" ) )
			{
				if ( lastKey != null )
				{
					map.put ( stripDashes ? lastKey.substring(1) : lastKey, "" );
				}
				lastKey = arg;
			}
			else
			{
				if ( lastKey != null )
				{
					map.put ( stripDashes ? lastKey.substring(1) : lastKey, arg );
				}
				lastKey = null;
			}
		}
		if ( lastKey != null )
		{
			map.put ( stripDashes ? lastKey.substring(1) : lastKey, "" );
		}
		return map;
	}

	/**
	 * get a required setting. if not provided, throw IllegalArgumentException with the 'what' description,
	 * preceded with "You must provide "
	 * @param argMap
	 * @param key
	 * @param what
	 * @return
	 */
	public static String getReqdSetting ( Map<String,String> argMap, String key, String what )
	{
		final String val = argMap.get ( key );
		if ( val == null )
		{
			throw new IllegalArgumentException ( "You must provide " + what );
		}
		return val;
	}

	/**
	 * Get a setting, or, if not provided, return the default value
	 * @param argMap
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String getSetting ( Map<String,String> argMap, String key, String defaultValue )
	{
		final String val = argMap.get ( key );
		if ( val == null )
		{
			return defaultValue;
		}
		return val;
	}

	/**
	 * Get a setting, or, if not provided, return the default value
	 * @param argMap
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static int getSetting ( Map<String,String> argMap, String key, int defaultValue )
	{
		final String s = getSetting ( argMap, key, "" + defaultValue );
		if ( s != null )
		{
			try
			{
				return Integer.parseInt ( s );
			}
			catch ( NumberFormatException x )
			{
				return defaultValue;
			}
		}
		return defaultValue;
	}
}
