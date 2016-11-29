/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import com.att.nsa.drumlin.till.nv.rrNvReadable;

public class nvReadableStack extends nvBaseReadable implements rrNvReadable
{
	public nvReadableStack ()
	{
		super ();
		fStack = new LinkedList<rrNvReadable> ();
	}

	@Override
	public String toString ()
	{
		return getCopyAsMap().toString ();
	}

	public void push ( rrNvReadable p )
	{
		fStack.addFirst ( p );
	}

	public void pushBelow ( rrNvReadable below, rrNvReadable above )
	{
		int i = fStack.indexOf ( above );
		if ( i < 0 )
		{
			push ( below );
		}
		else
		{
			fStack.add ( i+1, below );
		}
	}

	public String getString ( String key ) throws missingReqdSetting
	{
		String result = null;
		boolean found = false;
		for ( rrNvReadable p : fStack )
		{
			if ( p.hasValueFor ( key ) )
			{
				result = p.getString ( key );
				found = true;
				break;
			}
		}

		if ( !found )
		{
			throw new missingReqdSetting ( key );
		}

		return result;
	}

	public boolean hasValueFor ( String key )
	{
		boolean result = false;
		for ( rrNvReadable p : fStack )
		{
			result = p.hasValueFor ( key );
			if ( result ) break;
		}
		return result;
	}

	public void rescan () throws loadException
	{
		for ( rrNvReadable p : fStack )
		{
			p.rescan ();
		}
	}

	private final LinkedList<rrNvReadable> fStack;

	@Override
	public int size ()
	{
		return getAllKeys().size ();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		final TreeSet<String> set = new TreeSet<String> ();
		for ( rrNvReadable r : fStack )
		{
			set.addAll ( r.getAllKeys () );
		}
		return set;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		// this could be faster, but it's an easy way to get the correct values
		final HashMap<String,String> map = new HashMap<String,String> ();
		for ( String key : getAllKeys () )
		{
			final String val = getString ( key, null );
			if ( val != null )
			{
				map.put ( key, val );
			}
		}
		return map;
	}
}
