/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing.playish;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRouteInvocation;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRouteSource;

/**
 * A static entry point routing source is a collection of routing entries for mapping request
 * paths to static files and directories.
 * 
 * @author peter@rathravane.com
 *
 */
public class DrumlinPlayishStaticEntryPointRoutingSource implements DrumlinRouteSource
{
	public DrumlinPlayishStaticEntryPointRoutingSource ()
	{
		fPathList = new LinkedList<DrumlinPathInfo> ();
		fPackages = new LinkedList<String> ();
	}

	/**
	 * Add a verb and path route with an action string. The action can start with "staticDir:" or
	 * "staticFile:". The remainder of the string is used as a relative filename to the dir (staticDir:), or 
	 * as a filename (staticFile:).
	 * @param verb
	 * @param path
	 * @param action
	 * @return this object (for use in chaining the add calls)
	 */
	public synchronized DrumlinPlayishStaticEntryPointRoutingSource addRoute ( String verb, String path, String action )
	{
		if ( action.startsWith ( kStaticDirTag ) )
		{
			final DrumlinPathInfo pe = DrumlinPathInfo.processPath ( verb, path + ".*" );
			pe.setHandler ( new StaticDirHandler ( path, action.substring ( kStaticDirTag.length () ) ) );
			fPathList.add ( pe );
		}
		else if ( action.startsWith ( kStaticFileTag ) )
		{
			final DrumlinPathInfo pe = DrumlinPathInfo.processPath ( verb, path );
			pe.setHandler ( new StaticFileHandler ( path, action.substring ( kStaticFileTag.length () ) ) );
			fPathList.add ( pe );
		}
		else if ( action.startsWith ( kRedirectTag ) )
		{
			final DrumlinPathInfo pe = DrumlinPathInfo.processPath ( verb, path );
			final String loc = action.substring ( kRedirectTag.length () );
			pe.setHandler ( new RedirectHandler ( loc ) );
			fPathList.add ( pe );
		}
		else if ( action.startsWith ( kTemplateDirTag ) )
		{
			final DrumlinPathInfo pe = DrumlinPathInfo.processPath ( verb, path + ".*" );
			pe.setHandler ( new TemplateDirHandler ( "/" + action.substring ( kTemplateDirTag.length () ) ) );
			fPathList.add ( pe );
		}
		else
		{
			final DrumlinPathInfo pe = DrumlinPathInfo.processPath ( verb, path );
			pe.setHandler ( new StaticJavaEntryAction ( action, pe.getArgs(), fPackages ) );
			fPathList.add ( pe );
		}
		return this;
	}

	/**
	 * Get a route invocation for a given verb+path, or null.
	 */
	@Override
	public synchronized DrumlinRouteInvocation getRouteFor ( String verb, String path )
	{
		DrumlinRouteInvocation selected = null;
		for ( DrumlinPathInfo pe : fPathList )
		{
			final List<String> args = pe.matches ( verb, path );
			if ( args != null )
			{
				selected = getInvocation ( pe, args );
				break;
			}
		}
		return selected;
	}

	/**
	 * Get the URL that reaches a given static method with the given arguments. 
	 */
	@Override
	public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args )
	{
		final String fullname = c.getName() + "." + staticMethodName;
		for ( DrumlinPathInfo pe : fPathList )
		{
			if ( pe.invokes ( fullname ) )
			{
				return pe.makePath ( args );
			}
		}
		return null;
	}

	private final LinkedList<String> fPackages;
	private final LinkedList<DrumlinPathInfo> fPathList;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( DrumlinPlayishStaticEntryPointRoutingSource.class );

	protected invocation getInvocation ( DrumlinPathInfo pe, List<String> args )
	{
		return new invocation ( pe, args );
	}

	protected class invocation implements DrumlinRouteInvocation
	{
		public invocation ( DrumlinPathInfo pe, List<String> args )
		{
			fPe = pe;
			fArgs = args;
		}
	
		@Override
		public String getName ()
		{
			return fPe.getVerb () + "_" + fPe.getPath ();
		}

		@Override
		public void run ( DrumlinRequestContext ctx ) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
		{
			fPe.getHandler ().handle ( ctx, fArgs );
		}
	
		private final DrumlinPathInfo fPe;
		private final List<String> fArgs;
	}

	protected synchronized void clearRoutes ()
	{
		log.debug ( "Clearing routes within this static route source." );
		fPathList.clear ();
	}

	protected synchronized void addPackage ( String pkg )
	{
		fPackages.add ( pkg );
	}

	private static final String kStaticDirTag = "staticDir:";
	private static final String kStaticFileTag = "staticFile:";
	private static final String kRedirectTag = "redirect:";
	private static final String kTemplateDirTag = "templateDir:";
}
