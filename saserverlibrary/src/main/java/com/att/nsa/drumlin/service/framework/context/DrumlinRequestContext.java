/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.context;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.DrumlinConnection;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.service.framework.rendering.DrumlinRenderContext;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.till.nv.rrNvReadable;

/**
 * The DrumlinRequestContext provides the servlet, the inbound HTTP request, the outbound
 * HTTP response, the Connection, and other information to the request handlers provided by
 * the web application.
 * 
 * @author peter@rathravane.com
 *
 */
public class DrumlinRequestContext
{
	public DrumlinRequestContext ( DrumlinServlet webServlet, HttpServletRequest req, HttpServletResponse resp,
		DrumlinConnection s, Map<String, Object> objects, DrumlinRequestRouter router )
	{
		fRequest = req;
		fResponse = resp;
		fSession = s;
		fServlet = webServlet;
		fBaseRenderContext = webServlet.getBaseContext ();
		fLocalContext = new HashMap<String,Object> ();
		fObjects = objects;
		fRouter = router;

		fRequestImpl = new StdRequest ( fRequest );
	}

	/**
	 * Get the connection being serviced.
	 * @return a connection
	 */
	public DrumlinConnection session ()
	{
		return fSession;
	}

	public rrNvReadable systemSettings ()
	{
		return fServlet.getSettings ();
	}

	public DrumlinRequestRouter router ()
	{
		return fRouter;
	}

	public DrumlinServlet getServlet ()
	{
		return fServlet;
	}

	public String servletPathToFullUrl ( String contentUrl )
	{
		final StringBuffer url = new StringBuffer ();

		final String scheme = fRequest.getScheme ().toLowerCase ();
		url.append ( scheme );
		url.append ( "://" );
		url.append ( fRequest.getServerName () );

		final int serverPort = fRequest.getServerPort ();
		if ( !( ( scheme.equals ( "http" ) && serverPort == 80 ) ||
			( scheme.equals ( "https" ) && serverPort == 443 ) ) )
		{
			url.append ( ":" );
			url.append ( serverPort );
		}

		final String path = servletPathToFullPath ( contentUrl );
		url.append ( path );

		log.info ( "calculated full URL for [" + contentUrl + "]: [" + url + "]" );
		return url.toString ();
	}

	public String servletPathToFullPath ( String contentUrl )
	{
		return servletPathToFullPath ( contentUrl, fRequest );
	}

	public static String servletPathToFullPath ( String contentUrl, HttpServletRequest req )
	{
		final StringBuffer sb = new StringBuffer ();

		final String contextPart = req.getContextPath ();
		sb.append ( contextPart );

		final String servletPart = req.getServletPath ();
		sb.append ( servletPart );

		sb.append ( contentUrl );

		log.info ( "calculated full path for [" + contentUrl + "]: context=[" + contextPart + "], servlet=["
			+ servletPart + "], result=[" + sb.toString () + "]" );
		return sb.toString ();
	}

	public Object object ( String key )
	{
		return fObjects.get ( key );
	}

	public DrumlinRequest request ()
	{
		return fRequestImpl;
	}

	public DrumlinResponse response ()
	{
		return new StdResponse ( fRequest, fResponse, fRouter );
	}

	public DrumlinRenderContext renderer ()
	{
		return new StdRenderer (this);
	}

	public void merge ( String templateName, Map<String,Object> data, PrintWriter to )
	{
		final VelocityContext ctx = new VelocityContext ( fBaseRenderContext );
		for ( Entry<String, Object> e : data.entrySet () )
		{
			ctx.put ( e.getKey(), e.getValue() );
		}
		fServlet.getVelocity ().mergeTemplate ( templateName, "UTF-8", ctx, to );
	}
	
	private final HttpServletRequest fRequest;
	private final HttpServletResponse fResponse;
	final DrumlinConnection fSession;
	private final DrumlinServlet fServlet;
	private final VelocityContext fBaseRenderContext;
	final HashMap<String,Object> fLocalContext;
	private final Map<String, Object> fObjects;
	private final DrumlinRequestRouter fRouter;

	private final DrumlinRequest fRequestImpl;
	
	static org.slf4j.Logger log = LoggerFactory.getLogger ( DrumlinRequestContext.class );
}
