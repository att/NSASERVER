/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import javax.servlet.ServletException;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.DrumlinConnection;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.service.framework.routing.playish.DrumlinPlayishRoutingFileSource;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.ui.velocity.UiVelocityEventHandler;
import com.att.nsa.ui.velocity.UiVelocityResourceLoader;

/**
 * A servlet for UI sessions that are composed of plugins.
 * @author peter
 *
 */
public class UiServlet extends DrumlinServlet
{
	public UiServlet ( rrNvReadable settings )
	{
		super ( settings, null, sessionLifeCycle.FULL_SESSION );
		fPlugins = new LinkedList<UiPlugin> ();

		// this has to come before the servletSetup() call due to Drumlin setting
		// velocity prior to that call 
		fVeloLoader = new UiVelocityResourceLoader ();
	}

	/**
	 * Plugins must be registered before servletSetup() is called, so likely
	 * at some time between construction and "start" on the servlet container.
	 * @param p
	 */
	public void register ( UiPlugin p )
	{
		fPlugins.add ( p );
	}

	@Override
	public final DrumlinConnection createSession () throws rrNvReadable.missingReqdSetting
	{
		final UiSession s = new UiSession ();
		for ( UiPlugin p : fPlugins )
		{
			p.onNewSession ( s );
		}
		return s;
	}

	@Override
	protected final void servletSetup () throws rrNvReadable.missingReqdSetting, rrNvReadable.invalidSettingValue, ServletException
	{
		try
		{
			final rrNvReadable settings = super.getSettings ();
			for ( UiPlugin p : fPlugins )
			{
				p.configure ( settings );
			}

			final DrumlinRequestRouter router = getRequestRouter();

			// let plugins setup routes first so that they can't override the main servlet routes
			for ( UiPlugin p : fPlugins )
			{
				p.setupRouting ( router );
			}

			// put the list of plugins into the base context for menu rendering
			addToBaseContext ( "plugins", fPlugins );

			// now do main routes
			final URL url = UiServlet.class.getResource ( "/uiRoutes.conf" );
			router.addRouteSource ( new DrumlinPlayishRoutingFileSource ( url ) );

			// all set
			log.info ( "UI Servlet setup" );
		}
		catch ( IOException e )
		{
			throw new rrNvReadable.invalidSettingValue ( "<route file>", e );
		}
	}

	@Override
	protected void setupResourceLoader ( VelocityEngine ve, rrNvReadable p )
	{
		// use a custom resource loader that knows about apps
		ve.setProperty ( RuntimeConstants.RESOURCE_LOADER, "uiloader" );
		ve.setProperty ( "uiloader.resource.loader.instance", fVeloLoader );

		// also register an event handler
		ve.setProperty ( "eventhandler.include.class", UiVelocityEventHandler.class.getName() );
	}

	private final LinkedList<UiPlugin> fPlugins;
	private UiVelocityResourceLoader fVeloLoader;
	
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger ( UiServlet.class );
}
