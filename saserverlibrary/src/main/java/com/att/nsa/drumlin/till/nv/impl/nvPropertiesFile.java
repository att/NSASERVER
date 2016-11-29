/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.att.nsa.drumlin.till.nv.rrNvReadable;

public class nvPropertiesFile extends nvBaseReadable implements rrNvReadable
{
	public nvPropertiesFile ( File f ) throws loadException
	{
		super ();

		fFile = f;
		fUrl = null;
		fPrefs = new Properties ();
		rescan ();
	}

	public nvPropertiesFile ( URL u ) throws loadException
	{
		super ();

		fFile = null;
		fUrl = u;
		fPrefs = new Properties ();
		rescan ();
	}

	public String getString ( String key ) throws missingReqdSetting
	{
		final String result = fPrefs.getProperty ( key );
		if ( result == null )
		{
			throw new missingReqdSetting ( key );
		}
		return result;
	}

	@Override
	public boolean hasValueFor ( String key )
	{
		return fPrefs.containsKey ( key );
	}

	@Override
	public void rescan () throws loadException
	{
		try
		{
			fPrefs.clear ();
			if ( fFile != null )
			{
				read ( new FileInputStream ( fFile ) );
			}
			else if ( fUrl != null )
			{
				read ( fUrl.openStream () );
			}
			else
			{
				log.warning ( "Rescanning a preferences table does not have a backing file or URL." );
			}
		}
		catch ( FileNotFoundException e )
		{
			throw new loadException ( e );
		}
		catch ( IOException e )
		{
			throw new loadException ( e );
		}
	}

	@Override
	public int size ()
	{
		return fPrefs.size ();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		final TreeSet<String> list = new TreeSet<String> ();
		for ( Object o : fPrefs.keySet () )
		{
			list.add ( o.toString () );
		}
		return list;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		HashMap<String,String> map = new HashMap<String,String> ();
		for ( Entry<Object, Object> e : fPrefs.entrySet () )
		{
			map.put ( e.getKey().toString(), e.getValue().toString () );
		}
		return map;
	}

	private final File fFile;
	private final URL fUrl;
	private final Properties fPrefs;

	private static final Logger log = Logger.getLogger ( nvPropertiesFile.class.getName() );

	private void read ( InputStream is ) throws loadException
	{
		try
		{
			fPrefs.load ( is );
		}
		catch ( IOException e )
		{
			throw new loadException ( e );
		}
	}
	
	public String getFirstMatchingProperty(String key) throws missingReqdSetting {
		for ( Object o : fPrefs.keySet () )
		{
			//Change the asterisk to
			if (Pattern.matches(o.toString().replace("*", "[^.]+"), key)) {
				return fPrefs.getProperty(o.toString());
			}
		}
		
		throw new missingReqdSetting ( key );
	}
}
