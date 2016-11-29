/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.context;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * A response to a DrumlinRequest.
 * 
 * @author peter@rathravane.com
 *
 */
public interface DrumlinResponse
{
	/**
	 * send an error using the servlet container's error page system
	 * 
	 * @param err
	 * @param msg
	 */
	void sendError ( int err, String msg );

	/**
	 * set the status code for the reply
	 * 
	 * @param code
	 */
	DrumlinResponse setStatus ( int code );
	int getStatusCode ();

	DrumlinResponse setContentType ( String mimeType );

	DrumlinResponse send ( String content ) throws IOException;

	/**
	 * send an error response with the given body
	 * 
	 * @param err
	 * @param content
	 * @param mimeType
	 * @throws IOException
	 */
	void sendErrorAndBody ( int err, String content, String mimeType );

	PrintWriter getStreamForTextResponse ()
		throws IOException;

	PrintWriter getStreamForTextResponse ( String contentType )
		throws IOException;

	OutputStream getStreamForBinaryResponse ()
		throws IOException;

	OutputStream getStreamForBinaryResponse ( String contentType )
		throws IOException;

	void writeHeader ( String headerName, String headerValue );

	void writeHeader ( String headerName, String headerValue, boolean overwrite );

	/**
	 * redirect the to the app-relative url
	 * 
	 * @param url
	 */
	void redirect ( String url );

	void redirect ( Class<?> cls, String method );

	void redirect ( Class<?> cls, String method, Map<String, Object> args );

	/**
	 * redirect to the exact url
	 * 
	 * @param url
	 */
	void redirectExactly ( String url );
}
