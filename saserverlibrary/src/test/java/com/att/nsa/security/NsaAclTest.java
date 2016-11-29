/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import junit.framework.TestCase;

import org.junit.Test;

import com.att.nsa.security.ReadWriteSecuredResource.AccessDeniedException;

public class NsaAclTest extends TestCase
{
	@Test
	public void testEmptyAndNull () throws AccessDeniedException
	{
		assertNull ( NsaAcl.fromJson ( (String)null, false ) );

		NsaAcl acl = NsaAcl.fromJson ( (String)null, true );
		assertNotNull ( acl );
		assertEquals ( 0, acl.getUsers ().size() );

		acl = NsaAcl.fromJson ( "", false );
		assertNotNull ( acl );
		assertEquals ( 0, acl.getUsers ().size() );
	}
}
