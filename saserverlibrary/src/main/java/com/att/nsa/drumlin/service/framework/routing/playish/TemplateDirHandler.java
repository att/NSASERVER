/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing.playish;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

public class TemplateDirHandler implements DrumlinPlayishRouteHandler
{
	public TemplateDirHandler ( String dirInfo )
	{
	}

	@Override
	public void handle ( DrumlinRequestContext context, List<String> args )
		throws IOException,
			IllegalArgumentException,
			IllegalAccessException,
			InvocationTargetException
	{
		final String path = context.request ().getPathInContext ();
		if ( path != null && path.length() > 0 )
		{
			final String file = path.substring ( 1 );
			context.renderer ().renderTemplate ( file );
		}
		else
		{
			throw new IOException ( "Couldn't render path." );
		}
	}

	@Override
	public boolean actionMatches ( String fullPath )
	{
		return false;
	}
}
