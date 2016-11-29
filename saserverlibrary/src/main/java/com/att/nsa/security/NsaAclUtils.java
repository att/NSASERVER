/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import java.util.Collection;
import java.util.LinkedList;

import com.att.nsa.security.ReadWriteSecuredResource.AccessDeniedException;

public class NsaAclUtils
{
	public static void checkUserAccess ( String owner, NsaAcl acl, NsaApiKey user ) throws AccessDeniedException
	{
		final LinkedList<String> owners = new LinkedList<String> ();
		owners.add ( owner );
		checkUserAccess ( owners, acl, user );
	}

	/**
	 * Throw an exception if the user is not authorized. If the ACL is null,
	 * all users are authorized. Otherwise, if the user is null, or the ACL
	 * does not allow the given user, an exception is thrown.
	 * @param owners
	 * @param acl
	 * @param user
	 * @throws AccessDeniedException
	 */
	public static void checkUserAccess ( Collection<String> owners, NsaAcl acl, NsaApiKey user ) throws AccessDeniedException
	{
		// no acl = open
		if ( acl == null ) return;

		// UEB's topic ownership records sometimes exist but with an empty (single) owner
		// these are equivalent to empty ACLs
		if ( owners.size () == 1 && owners.iterator ().next ().length () == 0 )
		{
			return;
		}

		// we have an acl. we must have a user that either matches the owner or is allowed by the ACL
		if ( user == null )
		{
			throw new AccessDeniedException ( "(no user)" );
		}

		// check the owner list
		final String userKey = user.getKey ();
		if ( owners.contains ( userKey ) )
		{
			// user is an owner, continue
			return;
		}
		
		// we have an acl. we must have a user that either matches the owner or is allowed by the ACL
		if ( !acl.canUser ( user.getKey () ) )
		{
			throw new AccessDeniedException ( user.getKey() );
		}
	}

	/**
	 * Update a resource's ACLs with the given API key either as a reader or writer, and either adding or removing access.
	 * @param resource the resource whose ACLs to change
	 * @param asUser the user making the change
	 * @param theApiKey the API key to add/remove
	 * @param forReadAccess true if this is for read access, false for write access
	 * @param add true if this is an add (grant), false if removing (revoking)
	 * @return the updated ACL (caller needs to know which it is based on forReadAccess)
	 * @throws AccessDeniedException if the user is not authorized to make this change
	 */
	public static NsaAcl updateAcl ( ReadWriteSecuredResource resource, NsaApiKey asUser, String theApiKey, boolean forReadAccess, boolean add ) throws AccessDeniedException
	{
		if ( !resource.getOwners().contains ( asUser.getKey() ) )
		{
			throw new AccessDeniedException ( "User " + asUser.getKey() + " does not own topic " + resource.getName() );
		}

		NsaAcl acl = null;
		if ( forReadAccess )
		{
			acl = resource.getReaderAcl();
		}
		else
		{
			acl = resource.getWriterAcl();
		}
	
		if ( acl == null )
		{
			acl = new NsaAcl ();
		}

		if ( add )
		{
			acl.add ( theApiKey );
		}
		else
		{
			acl.remove ( theApiKey );
		}

		// some older ACL writes incorrectly left the ACL as inactive. This system doesn't
		// ever deactivate an ACL, but explicitly activate them all on writes.
		acl.activate ();

		return acl;
	}
}
