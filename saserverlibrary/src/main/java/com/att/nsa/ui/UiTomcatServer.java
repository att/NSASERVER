/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.ui;

import java.io.File;
import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.att.nsa.drumlin.till.nv.impl.nvWriteableTable;

public class UiTomcatServer
{
	public UiTomcatServer () throws IOException
	{
		this ( 8080 );
	}

	public UiTomcatServer ( int port ) throws IOException
	{
		// create a tomcat instance. Note that the base dir must be set before anything
		// else is done.
		fTomcat = new Tomcat ();
		fTomcat.setBaseDir ( makeTmpDir ( "tomcatBase" ).getAbsolutePath () );
		
		fServlet = new UiServlet ( new nvWriteableTable () );

		fPort = port;
	}

	public void addPlugin ( UiPlugin p )
	{
		fServlet.register ( p );
	}

	public void setPort ( int port )
	{
		fPort = port;
	}
	
	public void start () throws IOException
	{
		// create a servlet and context
		final Context rootCtx = fTomcat.addContext ( "", makeTmpDir ( "uiContext" ).getAbsolutePath () );
		Tomcat.addServlet ( rootCtx, kServletName, fServlet );
		rootCtx.addServletMapping ( "/*", kServletName );

		// determine the port
		fTomcat.setPort ( fPort );

		try
		{
			fTomcat.start ();
			fTomcat.getServer().await ();
		}
		catch ( Exception e )
		{
			throw new RuntimeException ( e );
		}
	}

	public void stop ()
	{
		try
		{
			fTomcat.stop ();
		}
		catch ( LifecycleException e )
		{
			// ignore
		}
	}

	private final Tomcat fTomcat;
	private final UiServlet fServlet;
	private int fPort;
	
	private static final String kServletName = "UiServlet";

	public static void main ( String[] args ) throws IOException, loadException, missingReqdSetting
	{
		int port = 8080;
		if ( args.length > 0 )
		{
			port = Integer.parseInt ( args[0] );
		}

		final UiTomcatServer t = new UiTomcatServer ( port );
		t.start ();
	}

	private static File makeTmpDir ( String name ) throws IOException
	{
		final File temp = File.createTempFile ("nsaui." + name + ".", Long.toString ( System.nanoTime() ) );
		if ( !temp.delete () )
		{
			throw new IOException ( "Couldn't delete tmp file " + temp.getAbsolutePath () );
		}
		if ( !temp.mkdir () )
		{
			throw new IOException ( "Couldn't create tmp dir " + temp.getAbsolutePath () );
		}
		temp.deleteOnExit ();
		return temp;
	}
}
