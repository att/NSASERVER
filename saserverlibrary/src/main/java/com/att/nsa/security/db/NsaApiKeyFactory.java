/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

import com.att.nsa.security.NsaApiKey;

public interface NsaApiKeyFactory<K extends NsaApiKey>
{
	/**
	 * Create a key using a key and secret
	 * @param key
	 * @param sharedSecret
	 * @return the key instance
	 */
	K makeNewKey ( String key, String sharedSecret );

	/**
	 * Create a key from its serialized form
	 * @param serializedForm
	 * @return the key instance
	 */
	K makeNewKey ( String serializedForm );
}
