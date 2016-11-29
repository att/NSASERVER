/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.collections;

import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Maps a key to a list (not just a set) of values.
 * @author peter
 *
 * @param <K>
 * @param <V>
 */
public class rrMultiMap<K,V>
{
	public rrMultiMap ()
	{
		fMultiMap = new Hashtable<K,List<V>> ();
	}

	@Deprecated
	public void add ( K k )
	{
		put ( k );
	}

	@Deprecated
	public void add ( K k, V v )
	{
		put ( k, v );
	}

	@Deprecated
	public void add ( K k, Collection<V> v )
	{
		put ( k, v );
	}

	public synchronized void put ( K k )
	{
		getOrCreateFor ( k );
	}

	public synchronized void put ( K k, V v )
	{
		LinkedList<V> list = new LinkedList<V>();
		list.add ( v );
		put ( k, list );
	}

	public synchronized void put ( K k, Collection<V> v )
	{
		List<V> itemList = getOrCreateFor ( k );
		itemList.removeAll ( v );	// only one of a given value allowed
		itemList.addAll ( v );
	}

	public synchronized void putAll ( Map<K,? extends Collection<V>> values )
	{
		for ( Map.Entry<K,? extends Collection<V>> e : values.entrySet () )
		{
			put ( e.getKey (), e.getValue () );
		}
	}
	
	public synchronized boolean containsKey ( K k )
	{
		return fMultiMap.containsKey ( k );
	}

	/**
	 * Get the values for a given key. A list is always returned, but it may be empty.
	 * @param k
	 * @return
	 */
	public synchronized List<V> get ( K k )
	{
		List<V> itemList = new LinkedList<V> ();
		if ( fMultiMap.containsKey ( k ) )
		{
			itemList = getOrCreateFor ( k );
		}
		return itemList;
	}

	/**
	 * Get the first value for the given key, or return null if none exists.
	 * @param k
	 * @return
	 */
	public V getFirst ( K k )
	{
		final List<V> items = get ( k );
		if ( items.size () > 0 )
		{
			return items.get ( 0 );
		}
		return null;
	}

	public synchronized Collection<K> getKeys ()
	{
		return fMultiMap.keySet ();
	}

	public synchronized Map<K,List<V>> getValues ()
	{
		return fMultiMap;
	}

	public synchronized List<V> remove ( K k )
	{
		return fMultiMap.remove ( k );
	}

	public synchronized void remove ( K k, V v )
	{
		List<V> itemList = getOrCreateFor ( k );
		itemList.remove ( v );
	}

	public synchronized void clear ()
	{
		fMultiMap.clear ();
	}

	public synchronized int size ()
	{
		return fMultiMap.size ();
	}

	public synchronized int size ( K k )
	{
		return getOrCreateFor ( k ).size ();
	}

	private final Hashtable<K,List<V>> fMultiMap;

	private synchronized List<V> getOrCreateFor ( K k )
	{
		List<V> itemList = fMultiMap.get ( k );
		if ( itemList == null )
		{
			itemList = new LinkedList<V> ();
			fMultiMap.put ( k, itemList );
		}
		return itemList;
	}
}
