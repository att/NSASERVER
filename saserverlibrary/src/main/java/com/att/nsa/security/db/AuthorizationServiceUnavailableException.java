/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

public class AuthorizationServiceUnavailableException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public AuthorizationServiceUnavailableException() { super(); }
	public AuthorizationServiceUnavailableException(String message) { super(message); }
	public AuthorizationServiceUnavailableException(Throwable t) { super(t); };
	public AuthorizationServiceUnavailableException(String message, Throwable t) { super(message, t); }
}