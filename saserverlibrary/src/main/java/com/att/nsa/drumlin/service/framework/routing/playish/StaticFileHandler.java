/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing.playish;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.util.StreamTools;

public class StaticFileHandler implements DrumlinPlayishRouteHandler
{
	public static final String kMaxAge = "drumlin.staticFile.cache.maxAgeSeconds";

	public StaticFileHandler ( String routedPath, String staticFile )
	{
		String file = staticFile.endsWith ( "/" ) ? ( staticFile + routedPath ) : staticFile;
		file = file.replaceAll ( "//", "/" );

		fFile = file;
		fContentType = StaticDirHandler.mapToContentType ( fFile );
	}

	@Override
	public void handle ( DrumlinRequestContext context, List<String> args ) throws IOException
	{
		// expiry. currently global.
		final int cacheMaxAge = context.systemSettings ().getInt ( kMaxAge, -1 );
		if ( cacheMaxAge > 0 )
		{
			context.response().writeHeader ( "Cache-Control", "max-age=" + cacheMaxAge, true );
		}

		log.info ( "finding stream [" + fFile + "]" );
		final URL f = context.getServlet ().findStream ( fFile );
		if ( f == null )
		{
			log.warn ( "404 [" + fFile + "] not found" );
			context.response ().sendError ( 404, fFile + " was not found on this server." );
		}
		else
		{
			StreamTools.copyStream (
				f.openStream (),
				context.response ().getStreamForBinaryResponse ( fContentType )
			);
		}
	}

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}

	private final String fFile;
	private final String fContentType;
	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( StaticFileHandler.class );
}
