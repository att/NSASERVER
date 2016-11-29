/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.DrumlinErrorHandler;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * A Drumlin request router is configured with route sources and error handlers, then
 * used to route an incoming request to a request handler.
 * 
 * @author peter@rathravane.com
 *
 */
public class DrumlinRequestRouter
{
	/**
	 * no matching route exception
	 */
	public static class noMatchingRoute extends Exception
	{
		public noMatchingRoute ( String route ) { super ( "No route for '" + route + "'" ); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * construct a request router
	 */
	public DrumlinRequestRouter () 
	{
		fSources = new LinkedList<DrumlinRouteSource> ();
		fErrorHandlers = new HashMap<Class<? extends Throwable>,DrumlinErrorHandler> ();
	}

	/**
	 * add a route source
	 * @param src
	 */
	public synchronized void addRouteSource ( DrumlinRouteSource src )
	{
		fSources.add ( src );
	}
	
	/**
	 * Provide a URL for redirects when there's no specific error handler for the exception.
	 * @param url
	 */
	public synchronized void setGeneralErrorRedirectUrl ( final String url )
	{
		fErrorHandlers.put ( Throwable.class, new DrumlinErrorHandler ()
		{
			@Override
			public void handle ( DrumlinRequestContext ctx, Throwable cause )
			{
				log.info ( "General error handler invoked, redirect to " + url );
				ctx.response ().redirect ( url );
			}
		} );
	}

	/**
	 * Set an error handler for a specific class of throwable. 
	 * @param x
	 * @param eh
	 */
	public synchronized void setHandlerForException ( Class<? extends Throwable> x, DrumlinErrorHandler eh )
	{
		fErrorHandlers.put ( x, eh );
	}

	/**
	 * Given an incoming request, check each route source (in order) for a match. If the route source
	 * has a match, it's used to handle the request.
	 * 
	 * @param req
	 * @return a matching handler
	 * @throws noMatchingRoute
	 */
	public synchronized DrumlinRouteInvocation route ( DrumlinRequest req ) throws noMatchingRoute
	{
		final String verbIn = req.getMethod ();
		final String verb = verbIn.equalsIgnoreCase("HEAD")?"GET":verbIn;	// HEAD is GET without an entity response

		final String path = req.getPathInContext ();

		DrumlinRouteInvocation route = null;
		for ( DrumlinRouteSource src : fSources )
		{
			route = src.getRouteFor ( verb, path );
			if ( route != null )
			{
				break;
			}
		}

		if ( route == null )
		{
			log.warn ( "No match for " + verb + " " + path );
			throw new noMatchingRoute ( path );
		}

		return route;
	}

	/**
	 * Find the proper handler for a throwable.
	 * @param cause
	 * @return an error handler, or null if none are applicable
	 */
	public synchronized DrumlinErrorHandler route ( Throwable cause )
	{
		DrumlinErrorHandler h = null;
		Class<?> c = cause.getClass ();
		while ( h == null && c != null )
		{
			h = fErrorHandlers.get ( c );
			if ( h == null )
			{
				c = c.getSuperclass ();
			}
		}
		return h;
	}

	/**
	 * Given a handler class and the name of one of its static methods, return a
	 * registered route to it.
	 * 
	 * @param c
	 * @param staticMethodName
	 * @return
	 */
	public synchronized String reverseRoute ( Class<?> c, String staticMethodName )
	{
		return reverseRoute ( c, staticMethodName, new HashMap<String,Object> () );
	}

	/**
	 * Given a handler class, the name of one of its static methods, and some arguments,
	 * return a registered route to the handler method.
	 * 
	 * @param c
	 * @param staticMethodName
	 * @param args
	 * @return
	 */
	public synchronized String reverseRoute ( Class<?> c, String staticMethodName, Map<String,Object> args )
	{
		String route = null;
		for ( DrumlinRouteSource src : fSources )
		{
			route = src.getRouteTo ( c, staticMethodName, args );
			if ( route != null )
			{
				break;
			}
		}
		return route;
	}

	private final LinkedList<DrumlinRouteSource> fSources;
	private final HashMap<Class<? extends Throwable>,DrumlinErrorHandler> fErrorHandlers;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( DrumlinRequestRouter.class );
}
