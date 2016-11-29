/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import java.util.Set;

import com.att.nsa.configs.ConfigDbException;

/**
 * An interface for resources that have read and write ACLs.
 * @author peter
 *
 */
public interface ReadWriteSecuredResource
{
	/**
	 * Access denied exception
	 */
	public class AccessDeniedException extends Exception
	{
		public AccessDeniedException () { super ( "Access denied." ); } 
		public AccessDeniedException ( String user ) { super ( "Access denied for " + user ); } 
		private static final long serialVersionUID = 1L;
	}

	/**
	 * a name for this resource
	 * @return a name
	 */
	String getName ();
	
	/**
	 * Get the set of owner API keys for this resource. Do not return null.
	 * @return a set of API keys
	 */
	Set<String> getOwners ();
	
	/**
	 * Get the ACL for reading on this topic. Can be null.
	 * @return an ACL or null
	 */
	// FIXME: we may not want to dictate the use of separate reader/writer ACLs
	NsaAcl getReaderAcl ();

	/**
	 * Get the ACL for writing on this topic.  Can be null.
	 * @return an ACL or null
	 */
	// FIXME: we may not want to dictate the use of separate reader/writer ACLs
	NsaAcl getWriterAcl ();

	/**
	 * Check if this user can read the topic. Throw otherwise. Note that
	 * user may be null.
	 * @param user
	 */
	void checkUserRead ( NsaApiKey user ) throws AccessDeniedException;

	/**
	 * Check if this user can write to the topic. Throw otherwise. Note
	 * that user may be null.
	 * @param user
	 */
	void checkUserWrite ( NsaApiKey user ) throws AccessDeniedException;

	/**
	 * allow the given user to publish
	 * @param apiKey 
	 * @param asUser the user making this change, who must be authorized to do so
	 */
	void permitWritesFromUser ( String apiKey, NsaApiKey asUser ) throws AccessDeniedException, ConfigDbException;

	/**
	 * deny the given user from publishing
	 * @param apiKey
	 * @param asUser the user making this change, who must be authorized to do so
	 */
	void denyWritesFromUser ( String apiKey, NsaApiKey asUser ) throws AccessDeniedException, ConfigDbException;

	/**
	 * allow the given user to read the topic
	 * @param apiKey
	 * @param asUser the user making this change, who must be authorized to do so
	 */
	void permitReadsByUser ( String apiKey, NsaApiKey asUser ) throws AccessDeniedException, ConfigDbException;

	/**
	 * deny the given user from reading the topic
	 * @param apiKey
	 * @param asUser the user making this change, who must be authorized to do so
	 * @throws ConfigDbException 
	 */
	void denyReadsByUser ( String apiKey, NsaApiKey asUser ) throws AccessDeniedException, ConfigDbException;
}
