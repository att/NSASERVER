/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;

import com.att.nsa.drumlin.till.nv.rrNvReadable;

public class nvReadableTable extends nvBaseReadable implements rrNvReadable
{
	public nvReadableTable ()
	{
		this ( (Map<String,String>)null );
	}

	public nvReadableTable ( Map<String,String> content )
	{
		if ( content != null )
		{
			fTable = content;
		}
		else
		{
			fTable = new HashMap<String,String> ();
		}
	}

	public nvReadableTable ( Properties content )
	{
		fTable = new HashMap<String,String> ();
		for ( Entry<Object, Object> e : content.entrySet () )
		{
			fTable.put ( e.getKey().toString (), e.getValue ().toString () );
		}
	}

	@Override
	public String toString ()
	{
		return fTable.toString ();
	}

	public synchronized void clear ( String key )
	{
		fTable.remove ( key );
	}

	public synchronized void clear ()
	{
		fTable.clear ();
	}

	public synchronized boolean hasValueFor ( String key )
	{
		return fTable.containsKey ( key );
	}

	public synchronized String getString ( String key ) throws missingReqdSetting
	{
		final String result = fTable.get ( key );
		if ( result == null )
		{
			throw new missingReqdSetting ( key );
		}
		return result;
	}

	@Override
	public synchronized int size ()
	{
		return fTable.size ();
	}

	@Override
	public synchronized Collection<String> getAllKeys ()
	{
		final TreeSet<String> list = new TreeSet<String> ();
		for ( Object o : fTable.keySet () )
		{
			list.add ( o.toString () );
		}
		return list;
	}

	@Override
	public synchronized Map<String, String> getCopyAsMap ()
	{
		HashMap<String,String> map = new HashMap<String,String> ();
		for ( Entry<String, String> e : fTable.entrySet () )
		{
			map.put ( e.getKey(), e.getValue() );
		}
		return map;
	}

	protected synchronized void set ( String key, String val )
	{
		fTable.put ( key, val );
	}

	protected synchronized void set ( Map<String,String> map )
	{
		fTable.putAll ( map );
	}

	private final Map<String,String> fTable;
}
