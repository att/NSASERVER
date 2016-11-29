/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing.playish;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author peter
 *
 */
public class DrumlinPathInfo
{
	public DrumlinPathInfo ( String verb, String path, List<String> args, Pattern pattern )
	{
		fVerb = verb;
		fPath = path; 
		fHandler = null;
		fArgs = args;
		fPathPattern = pattern;
	}

	@Override
	public String toString ()
	{
		return fVerb + " " + fPath + " ==> " + ( fHandler != null ? fHandler.toString () : "(null)" );
	}
	
	public void setHandler ( DrumlinPlayishRouteHandler handler )
	{
		fHandler = handler;
	}

	public DrumlinPlayishRouteHandler getHandler ()
	{
		return fHandler;
	}

	/**
	 * make a path to this handler given a set of named arguments
	 * @param args
	 * @return a path that will invoke this handler
	 */
	public String makePath ( Map<String,Object> args )
	{
		final StringBuffer result = new StringBuffer ();

		if ( fPath.contains ( "{" ) )
		{
			String remains = fPath;
			while ( remains.length () > 0 )
			{
				final int brace = remains.indexOf ( '{' );
				if ( brace > -1 )
				{
					final int close = remains.indexOf ( '}', brace );
					if ( close == -1 )
					{
						throw new IllegalArgumentException ( "Opened brace but didn't close it." );
					}

					result.append ( remains.substring ( 0, brace ) );

					String name = remains.substring ( brace + 1, close );
					if ( name.startsWith ( "<" ) )
					{
						int closeBracket = name.indexOf ( '>' );
						if ( closeBracket > -1 )
						{
							name = name.substring ( closeBracket+1 );
						}
					}
					
					final Object o = args == null ? null : args.get ( name );
					if ( o != null )
					{
						result.append ( args.get ( name ) );
					}
					else
					{
						result.append ( "{" );
						result.append ( name );
						result.append ( "}" );
					}

					remains = remains.substring ( close + 1 );
				}
				else
				{
					result.append ( remains );
					remains = "";
				}
			}
		}
		else
		{
			result.append ( fPath );
		}
		return result.toString ();
	}

	public List<String> getArgs ()
	{
		return new LinkedList<String> ( fArgs );
	}

	private final String fVerb;
	private final String fPath;
	private DrumlinPlayishRouteHandler fHandler;
	private final Pattern fPathPattern;
	private final List<String> fArgs;

	public List<String> matches ( String verb, String path )
	{
		LinkedList<String> result = null;
		if ( verb != null && path != null && verb.equalsIgnoreCase ( fVerb ) )
		{
			final Matcher m = fPathPattern.matcher ( path );
			final int argCount = fArgs.size ();
			if ( m.matches () )// && m.groupCount () == argCount )
			{
				result = new LinkedList<String> ();
				for ( int i=1; i<=argCount; i++ )
				{
					final String part = m.group ( i );
					final String decode = decode ( part );
					result.add ( decode );
				}
			}
		}
		return result;
	}

	public static DrumlinPathInfo processPath ( String verb, String path )
	{
		final LinkedList<String> args = new LinkedList<String> ();

		String fullPathRegex = path;
		if ( path.contains ( "{" ) )
		{
			// the path needs processing

			fullPathRegex = "";
			String remains = path;
			while ( remains.length () > 0 )
			{
				final int brace = remains.indexOf ( '{' );
				if ( brace > -1 )
				{
					final String preVar = remains.substring ( 0, brace );
					final int close = remains.indexOf ( '}', brace );
					if ( close == -1 )
					{
						throw new IllegalArgumentException ( "Opened brace but didn't close it." );
					}

					final String inner = remains.substring ( brace + 1, close );
					String regex = "([^/]+)";
					String argName = inner;
					if ( inner.startsWith ( "<" ) )
					{
						final int bracketClose = inner.indexOf ( '>' );
						if ( bracketClose == -1 )
						{
							throw new IllegalArgumentException ( "Opened bracket but didn't close it." );
						}

						argName = inner.substring ( bracketClose + 1 );
						regex = "(" + inner.substring ( 1, bracketClose ) + ")";
					}

					args.add ( argName );
					
					fullPathRegex += preVar;
					fullPathRegex += regex;
					remains = remains.substring ( close + 1 );
				}
				else
				{
					fullPathRegex += remains;
					remains = "";
				}
			}
		}
		final Pattern pathPattern = Pattern.compile ( fullPathRegex );
		return new DrumlinPathInfo ( verb, path, args, pathPattern );
	}

	public boolean invokes ( String fullName )
	{
		return fHandler.actionMatches ( fullName );
	}

	private static String decode ( String s )
	{
		try
		{
			return URLDecoder.decode ( s, "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new RuntimeException ( e );
		}
	}

	public String getVerb ()
	{
		return fVerb;
	}

	public String getPath ()
	{
		return fPath;
	}
}
