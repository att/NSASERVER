/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.till.nv.impl;

import junit.framework.TestCase;

import org.junit.Test;

public class nvInstallTypeWrapperTest extends TestCase
{
	@Test
	public void testParsing ()
	{
		assertEquals ( "foo", nvInstallTypeWrapper.parse ( "foo" ) );
		assertEquals ( "foo", nvInstallTypeWrapper.parse ( "foo[sys]" ) );
		assertEquals ( "foo", nvInstallTypeWrapper.parse ( "foo[sys@user]" ) );
		assertEquals ( "foo", nvInstallTypeWrapper.parse ( "foo[@user]" ) );
	}
}
