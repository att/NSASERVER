/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents;

import com.att.nsa.drumlin.app.userAgents.browsers.browser;
import com.att.nsa.drumlin.app.userAgents.browsers.genericBrowser;
import com.att.nsa.drumlin.app.userAgents.devices.device;
import com.att.nsa.drumlin.app.userAgents.devices.genericDevice;

/**
 * Used when the user agent is a completely generic/unknown system.
 * @author peter
 */
public class genericAgent implements userAgent
{
	public genericAgent ()
	{
		this ( new genericDevice(), new genericBrowser () );
	}

	public genericAgent ( device d, browser b )
	{
		fDevice = d;
		fBrowser = b;
	}

	@Override
	public String getDeviceName ()
	{
		return fDevice.getName ();
	}

	@Override
	public String getOsName ()
	{
		return fDevice.getOsName ();
	}

	@Override
	public String getOsVersion ()
	{
		return fDevice.getOsVersion ();
	}

	@Override
	public String getBrowserCanonicalName ()
	{
		return fBrowser.getName ();
	}

	@Override
	public String getBrowserCanonicalVersion ()
	{
		return fBrowser.getVersion ();
	}

	@Override
	public boolean getIsMobile ()
	{
		return fDevice.isMobile ();
	}

	@Override
	public boolean getIsFixedScreenSize ()
	{
		return fDevice.getScreenInfo ().isFixedSize ();
	}

	@Override
	public int getScreenWidth ()
	{
		return fDevice.getScreenInfo ().getWidth ();
	}

	@Override
	public int getScreenHeight ()
	{
		return fDevice.getScreenInfo ().getHeight ();
	}

	@Override
	public int getScreenDpi ()
	{
		return fDevice.getScreenInfo ().getDpi ();
	}

	private final device fDevice;
	private final browser fBrowser;
}
