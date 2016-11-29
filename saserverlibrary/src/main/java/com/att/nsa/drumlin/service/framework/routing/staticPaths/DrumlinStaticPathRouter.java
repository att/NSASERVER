/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing.staticPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRouteInvocation;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRouteSource;
import com.att.nsa.drumlin.service.framework.routing.playish.StaticDirHandler;
import com.att.nsa.drumlin.service.framework.routing.playish.StaticFileHandler;
import com.att.nsa.drumlin.service.standards.HttpMethods;
import com.att.nsa.util.StreamTools;

/**
 * A static entry point routing source is a collection of routing entries for
 * mapping request paths to static files and directories.
 * 
 * @author peter@rathravane.com
 */
public class DrumlinStaticPathRouter implements DrumlinRouteSource
{
	public static String kMaxAge = StaticFileHandler.kMaxAge;

	public DrumlinStaticPathRouter ( File baseDir ) throws IOException
	{
		fBaseDir = baseDir.getCanonicalFile ();
		if ( !fBaseDir.exists () || !fBaseDir.isDirectory () )
		{
			throw new IllegalArgumentException ( baseDir + " is not a directory." );
		}
	}

	/**
	 * This router will attempt to serve any path, assuming it's under the base
	 * directory. It handles GET/HEAD only, and rejects paths that are outside the base directory.
	 */
	@Override
	public synchronized DrumlinRouteInvocation getRouteFor ( String verb, final String path )
	{
		// only support GET (and HEAD)
		if ( !verb.equalsIgnoreCase ( HttpMethods.GET ) && !verb.equalsIgnoreCase ( HttpMethods.HEAD ) )
		{
			return null;
		}

		final File toServe = new File ( fBaseDir, path );
		return new DrumlinRouteInvocation ()
		{
			@Override
			public void run ( DrumlinRequestContext context )
				throws IOException,
					IllegalArgumentException,
					IllegalAccessException,
					InvocationTargetException
			{
				File in = toServe;
				if ( in.isDirectory () )
				{
					in = new File ( in, "index.html" );
				}

				final File canonical = in.getCanonicalFile ();
				if ( !canonical.getAbsolutePath ().startsWith ( fBaseDir.getAbsolutePath () ))
				{
					log.debug ( "ignoring [" + path + "] because it is outside of the base directory." );
					log.warn ( "404 [" + path + "]==>[" + path + "] (" + in.getAbsolutePath () + ")" );
					context.response ().sendError ( 404, path + " was not found on this server." );
					return;
				}
				
				// expiry. currently global.
				final int cacheMaxAge = context.systemSettings ().getInt ( kMaxAge, -1 );
				if ( cacheMaxAge > 0 )
				{
					context.response ().writeHeader ( "Cache-Control", "max-age=" + cacheMaxAge, true );
				}

				final String contentType = StaticDirHandler.mapToContentType ( in.getName () );

				try
				{
					final FileInputStream is = new FileInputStream ( in );
					final OutputStream os = context.response ().getStreamForBinaryResponse ( contentType );
					StreamTools.copyStream ( is, os );
				}
				catch ( FileNotFoundException e )
				{
					log.warn ( "404 [" + path + "]==>[" + path + "] (" + in.getAbsolutePath () + ")" );
					context.response ().sendError ( 404, path + " was not found on this server." );
				}
				catch ( IOException e )
				{
					log.warn ( "500 [" + toServe.getAbsolutePath () + "]: " + e.getMessage () );
					context.response ().sendError ( 500, e.getMessage () );
				}
			}

			@Override
			public String getName ()
			{
				return toServe.getPath ();
			}
		};
	}

	/**
	 * Reverse routing to entry points doesn't apply here. Always returns null.
	 */
	@Override
	public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args )
	{
		return null;
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( DrumlinStaticPathRouter.class );

	private final File fBaseDir;
}
