/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * A route invocation is returned by a route source as a match for an incoming route. It's then run()
 * to execute the request handling.
 * 
 * @author peter@rathravane.com
 *
 */
public interface DrumlinRouteInvocation
{
	/**
	 * Get the route's name
	 * @return the route name
	 */
	String getName ();

	/**
	 * Run the request
	 * @param ctx
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	void run ( DrumlinRequestContext ctx ) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException;
}
