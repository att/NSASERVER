/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.security.db.AuthorizationServiceUnavailableException;
import com.att.nsa.security.db.NsaAuthDb;

/**
 * A service for authorizing inbound requests
 * @deprecated we don't want to authorize based on HTTP transaction
 */
@Deprecated
public class NsaAuthorizationService<K extends NsaApiKey> {

	private final NsaAuthDb<K> authDb;
	
	public NsaAuthorizationService(NsaAuthDb<K> authDb) {
		this.authDb = authDb;
	}
	
	public void permitAll(K key, DrumlinRequestContext ctx) throws AuthorizationServiceUnavailableException {
		
		if (key == null) throw new IllegalArgumentException("Key cannot be null");
		
		final String resource = ctx.request().getUrl();
//		final String operation = ctx.request().getMethod();
		
		getAuthDb().permitAll(resource);
	}
	
	public void permit(K key, DrumlinRequestContext ctx) throws AuthorizationServiceUnavailableException {
		
		if (key == null) throw new IllegalArgumentException("Key cannot be null");
		
		final String resource = ctx.request().getUrl();
//		final String operation = ctx.request().getMethod();
		
		getAuthDb().permit(key, resource);
	}
	
	public void denyAll(K key, DrumlinRequestContext ctx) throws AuthorizationServiceUnavailableException {

		if (key == null) throw new IllegalArgumentException("Key cannot be null");
		
		final String resource = ctx.request().getUrl();
//		final String operation = ctx.request().getMethod();
		
		getAuthDb().denyAll(resource);
	}
	
	public void deny(K key, DrumlinRequestContext ctx) throws AuthorizationServiceUnavailableException {

		if (key == null) throw new IllegalArgumentException("Key cannot be null");
		
		final String resource = ctx.request().getUrl();
//		final String operation = ctx.request().getMethod();
		
		getAuthDb().deny(key, resource);
	}
	
	public boolean isAuthorized(K key, DrumlinRequestContext ctx) throws AuthorizationServiceUnavailableException {
		
		if (key == null) throw new IllegalArgumentException("Key cannot be null");
		
		final String resource = ctx.request().getUrl();
		final String operation = ctx.request().getMethod();
		
		return getAuthDb().isAuthorized(key, resource, operation);
	}
	
	private NsaAuthDb<K> getAuthDb() {
		return this.authDb;
	}
}
