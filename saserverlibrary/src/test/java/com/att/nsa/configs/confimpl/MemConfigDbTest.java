/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.configs.confimpl;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;

import com.att.nsa.configs.ConfigPath;

public class MemConfigDbTest extends TestCase
{
	@Test
	public void testDbStorage ()
	{
		final String data = "this is some data";
		final MemConfigDb db = new MemConfigDb ();
		db.store ( SimplePath.parse("/a/b/c"), data );
		final String readdata = db.load ( SimplePath.parse("/a/b/c/") );
		assertEquals ( data, readdata );

		final SimplePath path = SimplePath.parse ( "/a/b/c" );
		db.clear ( path );
		assertFalse ( db.exists ( path ));
	}

	@Test
	public void testChildLookup ()
	{
		final MemConfigDb db = new MemConfigDb ();
		for ( int i=0; i<10; i++ )
		{
			db.store ( SimplePath.parse("/a/b/c/" + i), "data-"+i );
			db.store ( SimplePath.parse("/a/d/e/" + i), "data-"+(i*10) );
		}

		final Set<ConfigPath> children = db.loadChildrenNames ( SimplePath.parse("/a/b/c") );
		assertEquals ( 10, children.size () );
		assertTrue ( children.contains ( SimplePath.parse ( "/a/b/c/3" ) ) );

		final Map<ConfigPath,String> childData = db.loadChildrenOf ( SimplePath.parse("/a/b/c") );
		assertEquals ( 10, childData.size () );
		assertTrue ( childData.containsKey ( SimplePath.parse ( "/a/b/c/3" ) ) );

		final Map<ConfigPath,String> bChildData = db.loadChildrenOf ( SimplePath.parse("/a/b/") );
		assertEquals ( 1, bChildData.size () );
		assertTrue ( bChildData.containsKey ( SimplePath.parse ( "/a/b/c" ) ) );
		assertNull ( bChildData.get ( SimplePath.parse ( "/a/b/c" ) ) );
	}
}
