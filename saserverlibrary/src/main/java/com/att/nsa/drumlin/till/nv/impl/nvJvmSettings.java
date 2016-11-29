/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

public class nvJvmSettings extends nvBaseReadable
{
	public nvJvmSettings ()
	{
		super ();
	}

	public String getString ( String key ) throws missingReqdSetting
	{
		final String result = System.getProperty ( key );
		if ( result == null )
		{
			throw new missingReqdSetting ( key );
		}
		return result;
	}

	@Override
	public boolean hasValueFor ( String key )
	{
		return System.getProperties ().containsKey ( key );
	}

	@Override
	public int size ()
	{
		return System.getProperties ().size ();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		final TreeSet<String> list = new TreeSet<String> ();
		for ( Object o : System.getProperties ().keySet () )
		{
			list.add ( o.toString () );
		}
		return list;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		HashMap<String,String> map = new HashMap<String,String> ();
		for ( Entry<Object, Object> e : System.getProperties ().entrySet () )
		{
			map.put ( e.getKey().toString(), e.getValue().toString () );
		}
		return map;
	}
}
