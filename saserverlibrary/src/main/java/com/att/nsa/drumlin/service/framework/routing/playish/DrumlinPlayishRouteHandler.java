/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.service.framework.routing.playish;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * A route handler handles a request, given a context. 
 * @author peter
 */
public interface DrumlinPlayishRouteHandler
{
	void handle ( DrumlinRequestContext context, List<String> args ) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException;
	boolean actionMatches ( String fullPath );
}
