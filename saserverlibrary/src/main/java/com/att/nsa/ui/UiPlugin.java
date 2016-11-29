/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui;

import java.util.List;

import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.till.nv.rrNvReadable;

/**
 * A plug-in for the UI framework
 * @author peter
 */
public interface UiPlugin
{
	/**
	 * Get the name of this plugin
	 * @return
	 */
	public String getUiName ();

	/**
	 * Get the base location for this plugin
	 * @return
	 */
	public String getUiLink ();

	/**
	 * Get the CSS files for this plugin. The system's standard CSS path is applied, so these
	 * would be relative to that path.
	 * @return a list of 0 or more CSS files to load on every page
	 */
	public List<String> getUiCssList ();

	/**
	 * Configure this plugin with the given settings.
	 * @param settings
	 */
	public void configure ( rrNvReadable settings );

	/**
	 * Setup routing into this plugin. Routing paths must start with
	 * the location given by getUiLink()
	 * @param router
	 */
	public void setupRouting ( DrumlinRequestRouter router );

	/**
	 * When the servlet engine creates a new session, this method is called on all plugins.
	 * @param s
	 */
	public void onNewSession ( UiSession s );
}
