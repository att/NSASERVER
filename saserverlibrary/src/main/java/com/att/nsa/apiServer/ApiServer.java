/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;

public class ApiServer {

	private final Tomcat tomcat;
	private final Servlet servlet;
	private final String name;
	
	private ApiServer(Builder builder) throws IOException {
		
		if (builder.encodeSlashes()) {
			// Tell Tomcat that we want encoded slashes in our paths. This isn't normally
			// used in the Cambria system, but some metrics require it.
			System.setProperty (
				"org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH",
				"true" );
		}

		this.servlet = builder.servlet();
		this.name = builder.name();
		
		tomcat = new Tomcat();

		final String servletName = name + "ApiServlet";
		final Context rootCtx = tomcat.addContext("", makeTmpDir(name + "Context").getAbsolutePath());
		Tomcat.addServlet(rootCtx, servletName, servlet);
		rootCtx.addServletMapping ( "/*", servletName);
		
		final List<ApiServerConnector> userConnectors = builder.connectors ();
		if ( userConnectors.size () > 0 )
		{
			// remove the default connector, which isn't actually created yet, but will be,
			// because tomcat is a pretty sloppy piece of software. to trigger the creation
			// of the default connector now, we need to get it once.

			boolean doneOne = false;
			for (ApiServerConnector connector : userConnectors ) {

				final Connector conn = new Connector ( builder.protocolClassName );
				if ( connector.isSecure () )
				{
					conn.setScheme ( "https" );
					conn.setSecure ( true );
					conn.setAttribute ( "keystorePass", connector.keystorePassword () );
					conn.setAttribute ( "keystoreFile", connector.keystoreFile () );
					conn.setAttribute ( "keyAlias", connector.keyAlias () );
					conn.setAttribute ( "clientAuth", "false" );
					conn.setAttribute ( "sslProtocol", "TLS" );
					conn.setAttribute ( "SSLEnabled", true );
				}
				conn.setPort ( connector.port () );

				// connection timeouts. NOTE: tomcat has some code (commented as hacky)
				// that decides to use the keepAlive timeout instead of the connection
				// timeout for reading the initial HTTP line. that defeats our connection
				// timeout for bogus connections, so we have to use a keepAlive that matches.
				conn.setAttribute ( "socket.soTimeout", connector.socketTimeoutMs () );
				conn.setAttribute ( "connectionTimeout", connector.socketTimeoutMs () );
//				conn.setAttribute ( "keepAliveTimeout", connector.keepAliveTimeoutMs () );
//				conn.setAttribute ( "socket.soKeepAlive", connector.keepAliveTimeoutMs () );
				conn.setAttribute ( "keepAliveTimeout", connector.socketTimeoutMs () );
				conn.setAttribute ( "socket.soKeepAlive", connector.socketTimeoutMs () );

				if (connector.maxThreads() > 0) conn.setAttribute("maxThreads", connector.maxThreads());

				tomcat.getService().addConnector(conn);
				if ( !doneOne )
				{
					// replace tomcat's default connector
					tomcat.setConnector ( conn );
				}
				doneOne = true;
			}
		}
		// else: leave default connector on 8080
	}
	
	public static class Builder {
		
		private final List<ApiServerConnector> connectors;
		private final Servlet servlet;
		private String protocolClassName = Http11NioProtocol.class.getName ();
		private String name = "apiServer";
		private boolean encodeSlashes = false;

		public Builder(Servlet servlet) {
			this ( new LinkedList<ApiServerConnector> (), servlet );
		}

		public Builder(List<ApiServerConnector> connectors, Servlet servlet) {
			if (connectors == null) {
				throw new IllegalArgumentException("You must provide a connector list");
			}
			
			this.connectors = connectors;
			this.servlet = servlet;
		}

		public Builder name(String name) { this.name = name; return this; }
		public Builder encodeSlashes(boolean encodeSlashes) { this.encodeSlashes = encodeSlashes; return this; }
		public Builder withConnector ( ApiServerConnector c ) { this.connectors.add ( c ); return this; }
		public Builder usingProtocol ( String cn ) { this.protocolClassName = cn; return this; }

		public String name() { return name; }
		public List<ApiServerConnector> connectors() { return connectors; }
		public Servlet servlet() { return servlet; }
		public boolean encodeSlashes() { return encodeSlashes; }
		
		public ApiServer build() throws IOException {
			if ( connectors.size () < 1 ) throw new IllegalArgumentException("At least one connector must be provided to build an API Server");
			return new ApiServer(this);
		}
	}
	
	public void await() {
		tomcat.getServer().await();
	}
	
	public void start() throws LifecycleException, IOException {

		tomcat.start();
		
		try {
			waitForTomcatLifecycleState(LifecycleState.STARTED);
		} catch(InterruptedException e) {
			//Ignore
		}
	}
	
	public void stop() throws LifecycleException {

		tomcat.stop();
		
		try {
			waitForTomcatLifecycleState(LifecycleState.STOPPED);
		} catch(InterruptedException e) {
			//Ignore
		}
		
		destroy();
	}
	
	public void destroy() throws LifecycleException {
		tomcat.destroy();
		
		try {
			waitForTomcatLifecycleState(LifecycleState.DESTROYED);
		} catch(InterruptedException e) {
			//Ignore
		}
	}

	public static void removeLeadingDashes ( Map<String, String> argMap )
	{
		final HashMap<String,String> replace = new HashMap<String,String> ();
		for ( Entry<String, String> e : argMap.entrySet () )
		{
			replace.put (
				e.getKey().startsWith("-")? e.getKey().substring(1) : e.getKey(),
				e.getValue() );
		}
		argMap.clear ();
		argMap.putAll ( replace );
	}

	private void waitForTomcatLifecycleState(LifecycleState s) throws InterruptedException {
		final Object mutex = new Object();
		final LifecycleState state = s;
		
		tomcat.getServer().addLifecycleListener(new LifecycleListener() {
			@Override
			public void lifecycleEvent(LifecycleEvent event) {
				if (event.getLifecycle().getState() == state) {
					synchronized (mutex) {
						tomcat.getServer().removeLifecycleListener(this);
						mutex.notify();
					}
				}
			}
		});

		synchronized (mutex) {
			while (tomcat.getServer().getState() != state)
				mutex.wait();
		}
	}
	
	/**
	 * make a tmp dir using the temp file facility to create a file, then replace it with a dir
	 * @param name
	 * @return
	 * @throws IOException
	 */
	private File makeTmpDir ( String name ) throws IOException
	{
		final File temp = File.createTempFile (name + ".", Long.toString ( System.nanoTime() ) );

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
