/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.att.nsa.configs.ConfigDb;
import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.configs.ConfigPath;
import com.att.nsa.configs.JsonConfigDb;
import com.att.nsa.security.NsaApiKey;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;

/**
 * Persistent storage for API keys and secrets built over an abstract config db.
 */
public class BaseNsaApiDbImpl<K extends NsaApiKey> implements NsaApiDb<K>
{
	/**
	 * Construct an API db over the given config db at the standard location
	 * @param db
	 * @throws ConfigDbException 
	 */
	public BaseNsaApiDbImpl ( ConfigDb db, NsaApiKeyFactory<K> keyFactory ) throws ConfigDbException
	{
		this ( db, kStdRootPath, keyFactory );
	}

	/**
	 * Construct an API db over the given config db using the given root location
	 * @param db
	 * @param rootPath
	 * @throws ConfigDbException 
	 */
	public BaseNsaApiDbImpl ( ConfigDb db, String rootPath, NsaApiKeyFactory<K> keyFactory ) throws ConfigDbException
	{
		fDb = new JsonConfigDb ( db );
		fBasePath = db.parse ( rootPath );
		fKeyFactory = keyFactory;

		if ( !db.exists ( fBasePath ) )
		{
			db.store ( fBasePath, "" );
		}
	}

	/**
	 * Load all keys known to this database. (This could be expensive.)
	 * @return a set of all API keys
	 * @throws ConfigDbException 
	 */
	public synchronized Set<String> loadAllKeys () throws ConfigDbException
	{
		final TreeSet<String> result = new TreeSet<String> ();
		for ( ConfigPath cp : fDb.loadChildrenNames ( fBasePath ) )
		{
			result.add ( cp.getName () );
		}
		return result;
	}

	/**
	 * Load all keys known to this database. (This could be expensive.)
	 * @return a map of api key to the api key record
	 * @throws ConfigDbException 
	 */
	public synchronized Map<String,K> loadAllKeyRecords () throws ConfigDbException
	{
		final HashMap<String,K> result = new HashMap<String,K> ();
		
		for ( Entry<ConfigPath, String> e : fDb.loadChildrenOf ( fBasePath ).entrySet () )
		{
			final String val = e.getValue ();
			if ( val != null )
			{
				result.put ( e.getKey ().getName(), fKeyFactory.makeNewKey ( val ) );
			}
		}
		return result;
	}
	
	/**
	 * Load an API key record based on the API key value
	 * @param apiKey
	 * @return an API key record or null
	 * @throws ConfigDbException 
	 */
	public synchronized K loadApiKey ( String apiKey ) throws ConfigDbException
	{
		final String data = fDb.load ( makePath(apiKey) );
		if ( data != null )
		{
			return fKeyFactory.makeNewKey ( data );
		}
		return null;
	}

	/**
	 * Save an API key record. This must be used after changing auxiliary data on the record.
	 * Note that the key must exist (via createApiKey). 
	 * @param key
	 * @throws ConfigDbException 
	 */
	public synchronized void saveApiKey ( K apiKey ) throws ConfigDbException
	{
		final ConfigPath path = makePath ( apiKey.getKey() );
		if ( !fDb.exists ( path ) || !(apiKey instanceof NsaSimpleApiKey) )
		{
			throw new IllegalStateException ( apiKey.getKey() + " is not known to this database" );
		}
		fDb.storeJson ( path, ((NsaSimpleApiKey)apiKey).serializeAsJson () );
	}

	/**
	 * Create a new API key. If one exists, 
	 * @param key
	 * @param sharedSecret
	 * @return the new API key record
	 * @throws ConfigDbException 
	 */
	public synchronized K createApiKey ( String key, String sharedSecret ) throws KeyExistsException, ConfigDbException
	{
		final ConfigPath path = makePath ( key );
		if ( fDb.exists ( path ) )
		{
			throw new KeyExistsException ( key );
		}

		// make one, store it, return it
		final K newKey = fKeyFactory.makeNewKey ( key, sharedSecret );
		fDb.store ( path, newKey.serialize () );
		return newKey;
	}

	/**
	 * Delete an API key; equivalent to deleteApiKey ( key.getKey() )
	 * @param key
	 * @return true if the key existed
	 * @throws ConfigDbException 
	 */
	public synchronized boolean deleteApiKey ( K key ) throws ConfigDbException
	{
		return deleteApiKey ( key.getKey () );
	}

	/**
	 * Delete an API key from storage.
	 * @param key
	 * @return true if the key existed
	 * @throws ConfigDbException 
	 */
	public synchronized boolean deleteApiKey ( String key ) throws ConfigDbException
	{
		return fDb.clear ( makePath ( key ) );
	}

	private final JsonConfigDb fDb;
	private final ConfigPath fBasePath;
	private final NsaApiKeyFactory<K> fKeyFactory;
	
	private static final String kStdRootPath = "/apikeys";

	private ConfigPath makePath ( String key )
	{
		return fBasePath.getChild ( key );
	}
}
