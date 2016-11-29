/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing;

import java.io.IOException;
import java.util.Map;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * A basic route handler provided for convenience in creating simple handlers.
 * 
 * @author peter@rathravane.com
 */
public abstract class DrumlinSimpleRouteHandler implements DrumlinRouteSource, DrumlinRouteInvocation
{
	public DrumlinRouteInvocation getRouteFor ( String verb, String path )
	{
		return this;
	}

	public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args )
	{
		return null;
	}

	public abstract void run ( DrumlinRequestContext ctx ) throws IOException;
}
