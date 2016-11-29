/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.service.standards;

/**
 * HTTP methods as used in Drumlin. They're plain strings (rather than an
 * enumeration). The HTTP spec allows for extension, so Drumlin does too.
 */
public class HttpMethods
{
	public static final String OPTIONS = "OPTIONS";
	public static final String GET = "GET";
	public static final String HEAD = "HEAD";
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String DELETE = "DELETE";
	public static final String TRACE = "TRACE";
	public static final String CONNECT = "CONNECT";
}
