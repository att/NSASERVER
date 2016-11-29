/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework;

import java.util.HashMap;

import javax.servlet.ServletException;

/**
 * The DrumlinConnection represents a session between a client system and the server.
 * 
 * @author peter@rathravane.com
 */
public interface DrumlinConnection
{
	/**
	 * Called when the servlet associates this connection to a client system.
	 * @param ws
	 * @param dcc
	 * @throws ServletException
	 */
	void onSessionCreate ( DrumlinServlet ws, DrumlinConnectionContext dcc ) throws ServletException;

	/**
	 * Called when the connection is closing.
	 */
	void onSessionClose ();

	/**
	 * Called when the session receives client activity.
	 */
	void noteActivity ();

	/**
	 * Called when the servlet requires the connection to build a context for use by
	 * the Velocity renderer.
	 * @param context
	 */
	void buildTemplateContext ( HashMap<String, Object> context );
}
