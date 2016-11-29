/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.configs.confimpl;

import junit.framework.TestCase;

import org.junit.Test;

public class SimplePathTest extends TestCase
{
	@Test
	public void testParseSimple ()
	{
		final SimplePath p = SimplePath.parse ( "/a/b/c" );
		assertEquals ( "c", p.getName () );
		assertEquals ( "b", p.getParent().getName () );
		assertEquals ( "a", p.getParent ().getParent().getName () );
		assertEquals ( "/a/b/c", p.toString () );
	}

	@Test
	public void testRootName ()
	{
		final SimplePath p = SimplePath.getRootPath ();
		assertEquals ( "/", p.toString () );
		assertNull ( p.getParent () );
	}

	@Test
	public void testNaming ()
	{
		for ( String path : kPaths )
		{
			final SimplePath p = SimplePath.parse ( path );
			assertEquals ( path, p.toString () );
		}
	}

	@Test
	public void testParseRootEquivalent ()
	{
		final SimplePath p = SimplePath.parse ( "/" );
		assertEquals ( p, SimplePath.getRootPath () );
	}

	@Test
	public void testTrailingSlashTrim ()
	{
		final SimplePath p = SimplePath.parse ( "/a/b/" );
		assertEquals ( "/a/b", p.toString () );
	}

	private static final String[] kPaths =
	{
		"/",
		"/a",
		"/a/b"
	};
}
