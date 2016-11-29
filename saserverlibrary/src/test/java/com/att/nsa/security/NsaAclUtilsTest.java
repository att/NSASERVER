/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import junit.framework.TestCase;

import org.junit.Test;

import com.att.nsa.security.ReadWriteSecuredResource.AccessDeniedException;

public class NsaAclUtilsTest extends TestCase
{
	@Test
	public void testDefaultHandling () throws AccessDeniedException
	{
		final NsaAcl empty = new NsaAcl ();
		NsaAclUtils.checkUserAccess ( "", empty, null );
	}
}
