/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

import com.att.nsa.security.NsaApiKey;

/**
 * 
 * @param <K>
 * @deprecated use ReadWriteSecuredResource
 */
@Deprecated
public interface NsaAuthDb<K extends NsaApiKey> {


	/**
	 * 
	 * @param owner
	 * @param resource
	 * @throws AuthorizationServiceUnavailableException
	 */
	void createResource(K owner, String resource) throws AuthorizationServiceUnavailableException, ResourceExistsException;
	
	/**
	 * 
	 * @param key
	 * @param resource
	 */
	void permit(K key, String resource) throws AuthorizationServiceUnavailableException;
	
	/**
	 * 
	 * @param key
	 * @param resource
	 */
	void deny(K key, String resource) throws AuthorizationServiceUnavailableException;
	
	/**
	 * 
	 * @param resource
	 */
	void permitAll(String resource) throws AuthorizationServiceUnavailableException;
	
	/**
	 * 
	 * @param resource
	 */
	void denyAll(String resource) throws AuthorizationServiceUnavailableException;
	
	/**
	 * Determines whether the given key is authorized for the (resource, operation) pair.
	 * @param key  An API Key
	 * @param resource The resource to grant authorization to
	 * @param operation The operation for which the key is permitted to execute on the resource
	 * @return true if the key is permitted to perform the operation on the resource, otherwise false
	 */
	public boolean isAuthorized(K key, String resource, String operation) throws AuthorizationServiceUnavailableException;

}
