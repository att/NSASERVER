/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.timedata.impl.mem;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.att.nsa.timedata.TimeSeriesEntry;

public class MemTsDbTest extends TestCase
{
	@Test
	public void testBasicPutGet ()
	{
		final MemTsDb<String> db = new MemTsDb<String> ();
		db.put ( "foo", 1, "1" );
		db.put ( "foo", 2, "2" );
		db.put ( "foo", 3, "3" );
		final TimeSeriesEntry<String> val = db.get ( "foo", 2 );
		assertNotNull ( val );
		assertEquals ( "2", val.getValue () );
	}

	@Test
	public void testRangeGetWithExactMatches ()
	{
		final MemTsDb<String> db = new MemTsDb<String> ();
		db.put ( "foo", 1, "1" );
		db.put ( "foo", 2, "2" );
		db.put ( "foo", 3, "3" );
		db.put ( "foo", 5, "5" );
		db.put ( "foo", 6, "6" );
		db.put ( "foo", 8, "8" );
		final List<? extends TimeSeriesEntry<String>> range = db.get ( "foo", 2, 6 );
		assertNotNull ( range );
		assertEquals ( 4, range.size () );
	}

	@Test
	public void testRangeGetWithoutMatches ()
	{
		final MemTsDb<String> db = new MemTsDb<String> ();
		db.put ( "foo", 1, "1" );
		db.put ( "foo", 2, "2" );
		db.put ( "foo", 3, "3" );
		db.put ( "foo", 5, "5" );
		db.put ( "foo", 6, "6" );
		db.put ( "foo", 8, "8" );
		final List<? extends TimeSeriesEntry<String>> range = db.get ( "foo", 4, 10 );
		assertNotNull ( range );
		assertEquals ( 3, range.size () );
		final Iterator<? extends TimeSeriesEntry<String>> it = range.iterator ();
		assertTrue ( it.hasNext () );
		assertEquals ( "5", it.next ().getValue () );
		assertTrue ( it.hasNext () );
		assertEquals ( "6", it.next ().getValue () );
		assertTrue ( it.hasNext () );
		assertEquals ( "8", it.next ().getValue () );
		assertFalse ( it.hasNext () );
	}
}
