/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing;

import java.util.Map;

/**
 * A route source is a collection of routes that are requested by verb (e.g. GET) and
 * a path. A Drumlin app can have any number of route sources. During request handling,
 * each route source is tested in order via getRouteFor(). If the route source returns
 * a {@link DrumlinRouteInvocation}, it's used to handle the request.
 * 
 * @author peter@rathravane.com
 *
 */
public interface DrumlinRouteSource
{
	/**
	 * Return the route handler for a given verb and path or null.
	 * @param verb
	 * @param path
	 * @return
	 */
	DrumlinRouteInvocation getRouteFor ( String verb, String path );

	/**
	 * Code in this system can create a URL to get to a specific class + method by asking
	 * the router to find a reverse-route. If this route source has routes that point to
	 * static entry points, it should implement an override that returns the correct URL.
	 * 
	 * @param c
	 * @param staticMethodName
	 * @param args
	 * @return null, or a URL to get to the entry point
	 */
	String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args );
}
