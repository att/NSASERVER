/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.service.framework.routing.playish;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.slf4j.LoggerFactory;

public class DrumlinPlayishRoutingFileSource extends DrumlinPlayishStaticEntryPointRoutingSource
{
	public DrumlinPlayishRoutingFileSource ( File f ) throws IOException
	{
		this ( f, true );
	}

	public DrumlinPlayishRoutingFileSource ( File f, boolean withAutoRefresh ) throws IOException
	{
		super ();

		loadRoutes ( f );
		createRefreshThread ( f, withAutoRefresh );
	}

	public DrumlinPlayishRoutingFileSource ( URL u ) throws IOException
	{
		super ();

		if ( u == null )
		{
			throw new IOException ( "URL for routing file is null in drumlinPlayStyleRoutingFileSource ( URL u )" );
		}
		loadRoutes ( u );
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( DrumlinPlayishRoutingFileSource.class );

	private synchronized void loadRoutes ( URL u ) throws IOException
	{
		loadRoutes ( new InputStreamReader ( u.openStream () ) );
	}

	private synchronized void loadRoutes ( File f ) throws IOException
	{
		loadRoutes ( new FileReader ( f ) );
	}

	private synchronized void loadRoutes ( Reader r ) throws IOException
	{
		clearRoutes ();

		final BufferedReader fr = new BufferedReader ( r );
		
		String line;
		while ( ( line = fr.readLine () ) != null )
		{
			line = line.trim ();
			if ( line.length () > 0 && !line.startsWith ( "#" ) )
			{
				processLine ( line );
			}
		}
	}

	private void processLine ( String line )
	{
		try
		{
			final StringTokenizer st = new StringTokenizer ( line );
			final String verb = st.nextToken ();
			if ( verb.toLowerCase ().equals ( "package" ) )
			{
				final String pkg = st.nextToken ();
				addPackage ( pkg );
			}
			else
			{
				final String path = st.nextToken ();
				final String action = st.nextToken ();
				addRoute ( verb, path, action );
			}
		}
		catch ( NoSuchElementException e )
		{
			log.warn ( "There was an error processing route config line: \"" + line + "\"" );
		}
		catch ( IllegalArgumentException e )
		{
			log.warn ( "There was an error processing route config line: \"" + line + "\": " + e.getMessage () );
		}
	}

	private Thread createRefreshThread ( final File f, boolean routeRefresh )
	{
		Thread result = null;
		if ( routeRefresh )
		{
			result = new Thread ()
			{
				private long fLastMod = f.lastModified ();
	
				@Override
				public void run ()
				{
					try
					{
						sleep ( 2000 );
					}
					catch ( InterruptedException e1 )
					{
						// ignore
					}
	
					final long lastMod = f.lastModified (); 
					if ( lastMod > fLastMod )
					{
						log.info ( "Reloading routes from " + f.getAbsolutePath () );
						try
						{
							fLastMod = lastMod;
							loadRoutes ( f );
						}
						catch ( IOException e )
						{
							log.error ( "Unable to load route file", e );
						}
					}
				}					
			};
			result.setName ( "Route file update watcher for " + f.getName () + "." );
			result.setDaemon ( true );
			result.start (); 	// FIXME: clunky, and not cool to start thread in constructor's scope
		}
		return result;
	}
}
