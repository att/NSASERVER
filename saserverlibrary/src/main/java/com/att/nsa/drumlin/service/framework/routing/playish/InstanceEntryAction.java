/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.routing.playish;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

public class InstanceEntryAction implements DrumlinPlayishRouteHandler
{
	public InstanceEntryAction ( Object instance, String action, List<String> args, Collection<String> packages )
	{
		fInstance = instance;
		fAction = action;
		fArgs = args;
		fMethod = null;

		processAction ( packages );
	}

	@Override
	public String toString ()
	{
		return fAction;
	}

	@Override
	public void handle ( DrumlinRequestContext context, List<String> addlArgs ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		final Object[] methodArgs = new Object[addlArgs.size ()+1];
		methodArgs[0] = context;
		int i=1;
		for ( String arg : addlArgs )
		{
			methodArgs[i++] = arg; 
		}
		fMethod.invoke ( fInstance, methodArgs );
	}

	@Override
	public boolean actionMatches ( String fullPath )
	{
		return fAction.equals ( fullPath );
	}

	private final Object fInstance;
	private String fAction;
	private final List<String> fArgs;
	private Method fMethod;

	private void processAction ( Collection<String> packages )
	{
		final int lastDot = fAction.lastIndexOf ( "." );
		if ( lastDot < 0 )
		{
			throw new IllegalArgumentException ( "The action string should have at least \"class.method\"." );
		}

		final String className = fAction.substring ( 0, lastDot );
		final String methodName = fAction.substring ( lastDot + 1 );

		try
		{
			final Class<?> c = locateClass ( className, packages );
			fAction = c.getName () + "." + methodName;

			final Class<?>[] s = new Class<?>[ fArgs.size () + 1 ];
			s[0] = DrumlinRequestContext.class;
			for ( int i=1; i<=fArgs.size(); i++ )
			{
				s[i] = String.class;
			}
			fMethod = c.getMethod ( methodName, s );
			if ( Modifier.isStatic ( fMethod.getModifiers () ) )
			{
				throw new IllegalArgumentException ( methodName + " is static." );
			}
		}
		catch ( ClassNotFoundException e )
		{
			throw new IllegalArgumentException ( e );
		}
		catch ( SecurityException e )
		{
			throw new IllegalArgumentException ( e );
		}
		catch ( NoSuchMethodException e )
		{
			throw new IllegalArgumentException ( e );
		}
	}

	private Class<?> locateClass ( String name, Collection<String> packages ) throws ClassNotFoundException
	{
		// try it straight...
		Class<?> result = tryClass ( name );
		if ( result == null )
		{
			// try the package list
			for ( String pkg : packages )
			{
				result = tryClass ( pkg + "." + name );
				if ( result != null ) break;
			}
		}
		if ( result == null )
		{
			throw new ClassNotFoundException ( name );
		}
		return result;
	}

	private Class<?> tryClass ( String name )
	{
		Class<?> result = null;
		try
		{
			result = Class.forName ( name );
			log.debug ( "class [" + name + "] located" );
		}
		catch ( ClassNotFoundException e )
		{
			log.debug ( "class [" + name + "] not found" );
		}
		return result;
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( InstanceEntryAction.class );
}
