/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

/**
 * An API key record. Note that any changes to the record via set() or enable/disable must
 * be written back to the API key db explicitly. This class doesn't write to an underlying
 * store.
 * 
 * @author peter
 */
public interface NsaApiKey
{
	/**
	 * Get the unique API key value
	 * @return the API key
	 */
	String getKey ();

	/**
	 * Get the shared secret used for signing requests.
	 * @return the API secret for this key
	 */
	String getSecret ();

	/**
	 * Return true if this key is currently enabled.
	 * @return true if enabled
	 */
	boolean enabled ();

	/**
	 * Enable this key.
	 */
	void enable ();

	/**
	 * Disable this key.
	 */
	void disable ();
	
	/**
	 * Set additional data on the key record. For example, a username or email,
	 * or app-level capability information.
	 * @param key
	 * @param val
	 */
	void set ( String key, String val );

	/**
	 * Get data from the key record other than the key and shared secret
	 * @param key
	 * @return a value, or null if none was set
	 */
	String get ( String key );

	/**
	 * Get data from the key record other than the key and shared secret
	 * @param key
	 * @param defVal the value to use if none is present
	 * @return the value, or the default
	 */
	String get ( String key, String defVal );

	/**
	 * serialize this key for the config db
	 * @return a string form of the key (typically JSON)
	 */
	String serialize ();
}
