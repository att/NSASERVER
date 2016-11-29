/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security;

public class NsaSecurityManagerException extends Exception {

	public NsaSecurityManagerException(String msg) { super(msg); }
	public NsaSecurityManagerException(Throwable t) { super(t); }
	public NsaSecurityManagerException(String msg, Throwable t) { super(msg,t); }
	private static final long serialVersionUID = 1L;

}
