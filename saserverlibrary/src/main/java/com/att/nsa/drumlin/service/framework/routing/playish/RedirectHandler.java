/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.service.framework.routing.playish;

import java.util.List;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

public class RedirectHandler implements DrumlinPlayishRouteHandler
{
	public static final String kMaxAge = "drumlin.staticDir.cache.maxAgeSeconds";
	
	public RedirectHandler ( String loc )
	{
		fTargetLocation = loc;
	}

	@Override
	public void handle ( DrumlinRequestContext context, List<String> args )
	{
		context.response ().redirect ( fTargetLocation );
	}

	private final String fTargetLocation;

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}
}
