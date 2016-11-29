/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing.playish;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.standards.MimeTypes;
import com.att.nsa.util.StreamTools;

public class StaticDirHandler implements DrumlinPlayishRouteHandler
{
	public static final String kMaxAge = StaticFileHandler.kMaxAge;

	public StaticDirHandler ( String routedPath, String staticDirInfo )
	{
		// the format of staticDirInfo is "dir;defaultpage"
		final String[] parts = staticDirInfo.split ( ";" );
		if ( parts.length < 1 ) throw new IllegalArgumentException ( "dir[;defaultpage]" );

		fRoutedPath = routedPath;	// e.g. "/css/"
		fDir = parts[0];			// e.g. "css"
		if ( parts.length > 1 )
		{
			fDefaultPage = parts[1];
		}
		else
		{
			fDefaultPage = null;
		}
	}

	@Override
	public void handle ( DrumlinRequestContext context, List<String> args )
	{
		final String path = context.request ().getPathInContext ();
		if ( path == null || path.length () == 0 )
		{
			log.warn ( "404 [" + path + "] no path provided" );
			context.response ().sendError ( 404, "no path provided" );
			return;
		}

		if ( path.contains ( ".." ) )
		{
			log.warn ( "404 [" + path + "] contains parent directory accessor" );
			context.response ().sendError ( 404, path + " was not found on this server." );
			return;
		}

		// here, the path should start with the "routed path" and we want to replace
		// that with the local dir
		if ( !path.startsWith ( fRoutedPath ) )
		{
			log.warn ( "404 [" + path + "] does not start with routed path [" + fRoutedPath + "]" );
			context.response ().sendError ( 404, path + " is not a matching path" );
			return;
		}

		// use "/" rather than File.separator because when running on windows, we wind
		// up with the wrong path for classpath searches
		final String newPath = fDir + "/" + path.substring ( fRoutedPath.length () );
		log.info ( "finding stream " + newPath );
		URL in = context.getServlet ().findStream ( newPath );
		if ( in == null && fDefaultPage != null )
		{
			String defIn = newPath + "/" + fDefaultPage;
			log.info ( "[" + newPath + "] does not exist, trying [" + defIn + "]." );
			URL defInFile = context.getServlet ().findStream ( defIn );
			if ( defInFile != null )
			{
				log.info ( "[" + defInFile + "] found, using it." );
				in = defInFile;
			}
		}

		log.info ( "Path [" + path + "] ==> [" + ( in == null ? "<not found>" : in.toString () ) + "]." );
		if ( in == null )
		{
			context.response ().sendError ( 404, path + " was not found on this server." );
		}
		else
		{
			final String contentType = mapToContentType ( in.toString () );

			// expiry. currently global.
			final int cacheMaxAge = context.systemSettings ().getInt ( kMaxAge, -1 );
			if ( cacheMaxAge > 0 )
			{
				context.response ().writeHeader ( "Cache-Control", "max-age=" + cacheMaxAge, true );
			}

			try
			{
				final InputStream is = in.openStream ();
				final OutputStream os = context.response ().getStreamForBinaryResponse ( contentType );
				StreamTools.copyStream ( is, os );
			}
			catch ( FileNotFoundException e )
			{
				log.warn ( "404 [" + path + "]==>[" + path + "] (" + in.toString () + ")" );
				context.response ().sendError ( 404, path + " was not found on this server." );
			}
			catch ( IOException e )
			{
				log.warn ( "500 [" + in.toString () + "]: " + e.getMessage () );
				context.response ().sendError ( 500, e.getMessage () );
			}
		}
	}	

	private final String fRoutedPath;
	private final String fDir;
	private final String fDefaultPage;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( StaticDirHandler.class );

	static final HashMap<String,String> sfContentTypes = new HashMap<String,String> ();
	static
	{
		sfContentTypes.put ( "css", MimeTypes.kCss );

		sfContentTypes.put ( "jpg", MimeTypes.kImageJpg );
		sfContentTypes.put ( "gif", MimeTypes.kImageGif );
		sfContentTypes.put ( "png", MimeTypes.kImagePng );
		sfContentTypes.put ( "ico", MimeTypes.kImageIco );

		sfContentTypes.put ( "htm", MimeTypes.kHtml );
		sfContentTypes.put ( "html", MimeTypes.kHtml );

		sfContentTypes.put ( "xml", MimeTypes.kAppXml );

		sfContentTypes.put ( "js", MimeTypes.kAppJavascript );

		sfContentTypes.put ( "eot", MimeTypes.kFontEot );
		sfContentTypes.put ( "woff", MimeTypes.kFontWoff );
		sfContentTypes.put ( "otf", MimeTypes.kFontOtf );
		sfContentTypes.put ( "ttf", MimeTypes.kFontTtf );
		sfContentTypes.put ( "svg", MimeTypes.kSvg );
	}

	public static String mapToContentType ( String name )
	{
		final int dot = name.lastIndexOf ( "." );
		if ( dot != -1 )
		{
			name = name.substring ( dot + 1 );
		}
		String result = sfContentTypes.get ( name );
		if ( result == null )
		{
			log.warn ( "Unknown content type [" + name + "]. Sending text/plain. (See " + StaticDirHandler.class.getSimpleName () + "::sfContentTypes)" );
			result = "text/plain";
		}
		return result;
	}

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}
}

