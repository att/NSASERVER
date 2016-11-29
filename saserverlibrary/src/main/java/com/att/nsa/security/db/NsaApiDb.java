/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

import java.util.Map;
import java.util.Set;

import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.security.NsaApiKey;
import com.att.nsa.security.NsaSecurityManagerException;

/**
 * Persistent storage for API keys and secrets built over an abstract config db. Instances
 * of this DB must support concurrent access.
 */
public interface NsaApiDb<K extends NsaApiKey>
{
	/**
	 * Load all keys known to this database. (This could be expensive.)
	 * @return a set of all API keys
	 * @throws ConfigDbException 
	 */
	Set<String> loadAllKeys () throws ConfigDbException;

	/**
	 * Load all keys known to this database. (This could be expensive.)
	 * @return a map of api key to the api key record
	 * @throws ConfigDbException 
	 */
	Map<String,K> loadAllKeyRecords () throws ConfigDbException;
	
	/**
	 * Load an API key record based on the API key value
	 * @param apiKey
	 * @return an API key record or null
	 * @throws ConfigDbException 
	 */
	K loadApiKey ( String apiKey ) throws ConfigDbException;

	/**
	 * Save an API key record. This must be used after changing auxiliary data on the record.
	 * Note that the key must exist (via createApiKey). 
	 * @param key
	 * @throws ConfigDbException 
	 */
	void saveApiKey ( K apiKey ) throws ConfigDbException;

	/**
	 * Create a new API key. If one exists, 
	 * @param key
	 * @param sharedSecret
	 * @return the new API key record
	 * @throws ConfigDbException 
	 */
	K createApiKey ( String key, String sharedSecret ) throws KeyExistsException, ConfigDbException;

	/**
	 * Delete an API key; equivalent to deleteApiKey ( key.getKey() )
	 * @param key
	 * @return true if the key existed
	 * @throws ConfigDbException 
	 */
	boolean deleteApiKey ( K key ) throws ConfigDbException;

	/**
	 * Delete an API key from storage.
	 * @param key
	 * @return true if the key existed
	 * @throws ConfigDbException 
	 */
	boolean deleteApiKey ( String key ) throws ConfigDbException;

	/**
	 * An exception to signal a key already exists 
	 */
	public static class KeyExistsException extends NsaSecurityManagerException
	{
		public KeyExistsException ( String key ) { super ( "API key " + key + " exists" ); }
		private static final long serialVersionUID = 1L;
	}
}
