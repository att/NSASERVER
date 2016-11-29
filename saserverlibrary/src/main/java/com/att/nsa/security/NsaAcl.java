/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ACL record. When active, a user (API key) must have an explicit entries
 * to be allowed access.
 * @author peter
 *
 */
public class NsaAcl
{
	/**
	 * Construct an ACL from a JSON string
	 * @param s
	 * @param nullReturnsEmpty
	 * @return an ACL
	 */
	public static NsaAcl fromJson ( String s, boolean nullReturnsEmpty )
	{
		final JSONObject o = s == null ? null : new JSONObject ( s.length() == 0 ? "{}" : s );
		return fromJson ( o, nullReturnsEmpty );
	}

	/**
	 * Construct an ACL from a JSON object. If the JSON is null,
	 * null is returned. This indicates the lack of an ACL.
	 * @param o the object
	 * @param nullReturnsEmpty If true and the object is null, an empty ACL is returned. Otherwise, if the object is null, null is returned
	 * @return an ACL or null
	 */
	public static NsaAcl fromJson ( JSONObject o, boolean nullReturnsEmpty ) 
	{
		if ( o == null && !nullReturnsEmpty ) return null;

		final NsaAcl acl = new NsaAcl ();
		if ( o != null )
		{
			final JSONArray a = o.optJSONArray ( "allowed" );
			if ( a != null )
			{
				for ( int i=0; i<a.length (); i++ )
				{
					final String user = a.getString ( i );
					acl.add ( user );
				}
			}
		}
		return acl;
	}

	/**
	 * Construct a new ACL. The ACL's owner must be noted during construction
	 * because owners always have permission to access.
	 * @param owner
	 */
	public NsaAcl ()
	{
		fActive = true;
		fAllowed = new TreeSet<String> ();
	}

	/**
	 * Activate this ACL
	 */
	public void activate ()
	{
		fActive = true;
	}

	/**
	 * is this ACL active?
	 * @return
	 */
	public boolean isActive ()
	{
		return fActive;
	}

	/**
	 * Can the user access this resource?
	 * @param user
	 * @param perm
	 * @return true if the user is explicitly allowed
	 */
	public boolean canUser ( String user )
	{
		return !fActive || fAllowed.contains ( user );
	}

	/**
	 * Add an entry to the end of the list.
	 * @param userApiKey
	 */
	public void add ( String userApiKey )
	{
		fAllowed.add ( userApiKey );
	}

	/**
	 * Remove a user's access
	 * @param userApiKey
	 */
	public void remove ( String userApiKey )
	{
		fAllowed.remove ( userApiKey );
	}
	
	/**
	 * Get the users on this ACL
	 * @return the user set
	 */
	public Set<String> getUsers ()
	{
		return new TreeSet<String> ( fAllowed );
	}

	/**
	 * serialize to a json string
	 * @return a serialized JSON string
	 */
	public JSONObject serialize ()
	{
		if ( !fActive )
		{
			log.warn ( "Serializing an inactive ACL. (Inactive ACLs are deprecated.)" );
		}

		final JSONObject o = new JSONObject ();
		o.put ( "active", fActive );
		final JSONArray a = new JSONArray ();
		for ( String u : fAllowed )
		{
			a.put ( u );
		}
		o.put ( "allowed", a );
		return o;
	}

	private boolean fActive;
	private final TreeSet<String> fAllowed;

	private static final Logger log = LoggerFactory.getLogger ( NsaAcl.class );
}
