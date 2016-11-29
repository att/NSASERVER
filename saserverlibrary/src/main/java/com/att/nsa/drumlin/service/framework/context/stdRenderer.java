/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.rendering.DrumlinRenderContext;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;

class StdRenderer implements DrumlinRenderContext
{
	StdRenderer ( DrumlinRequestContext hc )
	{
		fContext = hc;
	}

	@Override
	public Object get ( String key )
	{
		return fContext.fLocalContext.get ( key );
	}

	@Override
	public void put ( String key, Object o )
	{
		fContext.fLocalContext.put ( key, o );
	}

	@Override
	public void remove ( String key )
	{
		fContext.fLocalContext.remove ( key );
	}

	@Override
	public void renderTemplate ( String templateName )
	{
		renderTemplate ( templateName, "text/html" );
	}

	@Override
	public void renderTemplate ( String templateName, String contentType )
	{
		try
		{
			if ( fContext.fSession != null )
			{
				final HashMap<String, Object> context = new HashMap<String, Object> ();
				fContext.fSession.buildTemplateContext ( context );
				for ( Map.Entry<String, Object> e : context.entrySet () )
				{
					put ( e.getKey (), e.getValue () );
				}
			}

			final PrintWriter out = fContext.response().getStreamForTextResponse ( contentType );
			fContext.merge ( templateName, fContext.fLocalContext, out );
		}
		catch ( ResourceNotFoundException e )
		{
			fContext.response ().sendError ( HttpStatusCodes.k404_notFound, e.getMessage () );
		}
		catch ( Exception e )
		{
			fContext.response ().sendError ( HttpStatusCodes.k500_internalServerError, e.getMessage () );

			final StringWriter sw = new StringWriter ();
			final PrintWriter pw = new PrintWriter ( sw );
			e.printStackTrace ( pw );
			pw.close ();
			log.error ( sw.toString () );
		}
	}

	private final DrumlinRequestContext fContext;
	static org.slf4j.Logger log = LoggerFactory.getLogger ( StdRenderer.class );
}
