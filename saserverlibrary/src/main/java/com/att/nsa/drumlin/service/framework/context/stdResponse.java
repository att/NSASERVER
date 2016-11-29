/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.context;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.service.standards.HttpMethods;
import com.att.nsa.drumlin.service.standards.MimeTypes;

class StdResponse implements DrumlinResponse
{
	public StdResponse ( HttpServletRequest req, HttpServletResponse r, DrumlinRequestRouter rr )
	{
		fRequest = req;
		fResponseEntityAllowed = !(req.getMethod ().equalsIgnoreCase ( HttpMethods.HEAD ));
		fResponse = r;
		fRouter = rr;
		writeHeader ( "X-Rathravane", "~ software is craft ~", true );
	}

	@Override
	public void sendErrorAndBody ( int err, String content, String mimeType )
	{
		try
		{
			setStatus ( err );
			getStreamForTextResponse ( mimeType ).println ( content );
		}
		catch ( IOException e )
		{
			log.warn ( "Error sending error response: " + e.getMessage () );
		}
	}

	@Override
	public void sendError ( int err, String msg )
	{
		try
		{
			fResponse.sendError ( err, msg );
		}
		catch ( IOException e )
		{
			log.error ( "Error sending response: " + e.getMessage () );
		}
	}

	@Override
	public DrumlinResponse setStatus ( int code )
	{
		fResponse.setStatus ( code );
		return this;
	}

	@Override
	public int getStatusCode ()
	{
		return fResponse.getStatus ();
	}
	
	@Override
	public DrumlinResponse setContentType ( String mimeType )
	{
		fResponse.setContentType ( mimeType );
		return this;
	}

	@Override
	public DrumlinResponse send ( String content ) throws IOException
	{
		final PrintWriter pw = new PrintWriter ( fResponse.getWriter () );
		pw.print ( content );
		pw.close ();
		return this;
	}

	@Override
	public void writeHeader ( String headerName, String headerValue )
	{
		writeHeader ( headerName, headerValue, false );
	}

	@Override
	public void writeHeader ( String headerName, String headerValue, boolean overwrite )
	{
		if ( overwrite )
		{
			fResponse.setHeader ( headerName, headerValue );
		}
		else
		{
			fResponse.addHeader ( headerName, headerValue );
		}
	}

	@Override
	public OutputStream getStreamForBinaryResponse () throws IOException
	{
		return getStreamForBinaryResponse ( MimeTypes.kAppGenericBinary );
	}

	@Override
	public OutputStream getStreamForBinaryResponse ( String contentType ) throws IOException
	{
		fResponse.setContentType ( contentType );

		OutputStream os = null;
		if ( fResponseEntityAllowed )
		{
			os = fResponse.getOutputStream ();
		}
		else
		{
			os = new NullStream ();
		}
		return os;
	}

	@Override
	public PrintWriter getStreamForTextResponse ()
		throws IOException
	{
		return getStreamForTextResponse ( "text/html" );
	}

	@Override
	public PrintWriter getStreamForTextResponse ( String contentType ) throws IOException
	{
		fResponse.setContentType ( contentType );

		PrintWriter pw = null;
		if ( fResponseEntityAllowed )
		{
			pw = fResponse.getWriter ();
		}
		else
		{
			pw = new PrintWriter ( new NullWriter () );
		}
		return pw;
	}

	@Override
	public void redirect ( String url )
	{
		redirectExactly ( DrumlinRequestContext.servletPathToFullPath ( url, fRequest ) );
	}

	@Override
	public void redirect ( Class<?> cls, String method )
	{
		redirect ( cls, method, new HashMap<String, Object> () );
	}

	@Override
	public void redirect ( Class<?> cls, String method, Map<String, Object> args )
	{
		String localUrl = fRouter.reverseRoute ( cls, method, args );
		if ( localUrl == null )
		{
			log.error ( "No reverse route for " + cls.getName () + "::" + method + " with " + (args == null ? 0 : args.size () ) + " args." );
			localUrl = "/";
		}
		redirect ( localUrl );
	}

	@Override
	public void redirectExactly ( String url )
	{
		try
		{
			fResponse.sendRedirect ( url );
		}
		catch ( IOException e )
		{
			log.error ( "Error sending redirect: " + e.getMessage () );
		}
	}

	private final HttpServletRequest fRequest;
	private final boolean fResponseEntityAllowed;
	private final HttpServletResponse fResponse;
	private final DrumlinRequestRouter fRouter;

	private static org.slf4j.Logger log = LoggerFactory.getLogger ( StdResponse.class );

	private static class NullWriter extends Writer
	{
		@Override
		public void write ( char[] cbuf, int off, int len )
		{
		}

		@Override
		public void flush ()
		{
		}

		@Override
		public void close ()
		{
		}
	}

	private static class NullStream extends OutputStream
	{
		@Override
		public void write ( int b ) {}
	}
}
