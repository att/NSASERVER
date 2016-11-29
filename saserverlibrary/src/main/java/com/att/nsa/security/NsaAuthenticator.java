/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;

/**
 * An interface for authenticating an inbound request.
 * @author peter
 */
public interface NsaAuthenticator<K extends NsaApiKey>
{
	/**
	 * Qualify a request as possibly using the authentication method that this class implements.
	 * @param req
	 * @return true if the request might be authenticated by this class
	 */
	boolean qualify ( DrumlinRequest req );
	
	/**
	 * Check for a request being authentic. If it is, return the API key. If not, return null.
	 * @param req An inbound web request
	 * @return the API key for an authentic request, or null
	 */
	K isAuthentic ( DrumlinRequest req );
}
