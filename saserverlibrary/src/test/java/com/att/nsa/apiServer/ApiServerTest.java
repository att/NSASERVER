/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.apache.catalina.LifecycleException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.util.StreamTools;

public class ApiServerTest extends TestCase
{
	@Test
	public void testHtpsOnly () throws IOException, LifecycleException
	{
		final File tmpFile = File.createTempFile ( "keyfile", "" );
		try
		(
			final InputStream is = ApiServerTest.class.getClassLoader().getResourceAsStream ( "keystore.dummy" );
			final FileOutputStream os = new FileOutputStream ( tmpFile );
		)
		{
			StreamTools.copyStream ( is, os );
		}

		final ApiServerConnector conn = new ApiServerConnector.Builder ( 7443 )
			.secure ( true )
			.keystoreFile ( tmpFile.getAbsolutePath () )
			.keystorePassword ( "foobar" )
			.keyAlias ( "tomcat" )
			.build ()
		;

		final ApiServer server = new ApiServer.Builder ( new MyServlet () )
			.withConnector ( conn )
			.build ()
		;

		server.start ();

		server.stop ();
		tmpFile.delete ();
	}

	private class MyServlet implements Servlet
	{
		@Override
		public void init ( ServletConfig config ) throws ServletException {}

		@Override
		public ServletConfig getServletConfig ()
		{
			return new ServletConfig ()
			{
				@Override
				public String getServletName ()
				{
					return "test";
				}

				@Override
				public ServletContext getServletContext ()
				{
					return null;
				}

				@Override
				public String getInitParameter ( String name )
				{
					return null;
				}

				@Override
				public Enumeration<String> getInitParameterNames ()
				{
					return new Enumeration<String> () 
					{
						@Override
						public boolean hasMoreElements () { return false; }

						@Override
						public String nextElement () { return null; }
					};
				}
				
			};
		}
 
		@Override
		public void service ( ServletRequest req, ServletResponse res ) throws ServletException, IOException
		{
			log.info ( "service ... " );
		}

		@Override
		public String getServletInfo () { return null; }

		@Override
		public void destroy () {}
	}

	private static final Logger log = LoggerFactory.getLogger ( ApiServerTest.class );
}
