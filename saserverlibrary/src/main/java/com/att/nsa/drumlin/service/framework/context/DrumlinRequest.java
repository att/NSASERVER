/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * A request made to a servlet.
 * 
 * @author peter
 */
public interface DrumlinRequest
{
	/**
	 * Get the request's URL
	 * @return the URL
	 */
	String getUrl ();

	/**
	 * Get the query string from the HTTP request, or null if there's no query
	 * @return the query string or null
	 */
	String getQueryString ();

	/**
	 * Get the HTTP method for this request. 
	 * @return the HTTP method
	 */
	String getMethod ();

	/**
	 * get the request's path within the servlet context
	 * @return the request path within the servlet context
	 */
	String getPathInContext ();

	/**
	 * Get the first value (of 1 or more) for a given header name.
	 * @param header
	 * @return null if the header does not exist, or the first value otherwise
	 */
	String getFirstHeader ( String header );

	/**
	 * Get all values for a given header.
	 * @param header
	 * @return a list of 0 or more values
	 */
	List<String> getHeader ( String header );

	/**
	 * Get the parameter map for this request.
	 * @return a map of name/value pairs.
	 */
	Map<String, String[]> getParameterMap ();

	/**
	 * get a parameter by name
	 * @param key
	 * @return null, or the value of the named parameter
	 */
	String getParameter ( String key );

	/**
	 * get a parameter by name. If the parameter does not exist on this request,
	 * return the default value provided.
	 * 
	 * @param key
	 * @param defVal
	 * @return the value of the parameter, or the default value
	 */
	String getParameter ( String key, String defVal );

	/**
	 * Get a parameter as an integer.
	 * @param key
	 * @param defVal
	 * @return
	 */
	int getIntParameter ( String key, int defVal );

	/**
	 * Change the value of a parameter on this request. (Generally used by validators.)
	 * @param fieldName
	 * @param defVal
	 */
	void changeParameter ( String fieldName, String defVal );

	/**
	 * Get the content type header for this request.
	 * @return
	 */
	String getContentType ();

	/**
	 * Get the content length for this request.
	 * @return the number of bytes of content in this request
	 */
	int getContentLength ();

	/**
	 * get the content as an input stream
	 * @return the body of the request as an input stream
	 * @throws IOException
	 */
	InputStream getBodyStream () throws IOException;

	/**
	 * get the content of the request as text.
	 * @return a buffered reader on the content of this request.
	 * @throws IOException
	 */
	BufferedReader getBodyStreamAsText () throws IOException;

	/**
	 * get the address of the requesting agent
	 * @return the address of the requesting agent
	 */
	String getRemoteAddress ();

	/**
	 * return true if the request (and response) was made over a secure transport
	 * @return true if the request was made over a secure transport
	 */
	boolean isSecure ();
}
