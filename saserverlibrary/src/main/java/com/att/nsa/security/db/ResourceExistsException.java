/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db;

public class ResourceExistsException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public ResourceExistsException() { super(); }
	public ResourceExistsException(String message) { super(message); }
	public ResourceExistsException(Throwable t) { super(t); }
	public ResourceExistsException(String message, Throwable t) { super(message, t); }
}
