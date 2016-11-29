/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents.devices;

public class genericDevice implements device
{
	public genericDevice ()
	{
		this ( false );
	}
	
	public genericDevice ( boolean isMobile )
	{
		this ( new screenInfo (), isMobile );
	}

	public genericDevice ( screenInfo si, boolean isMobile )
	{
		fScreen = si;
		fIsMobile = isMobile;
	}

	@Override
	public String getName ()
	{
		return "generic";
	}

	@Override
	public String getVersion ()
	{
		return "";
	}

	@Override
	public screenInfo getScreenInfo ()
	{
		return fScreen;
	}

	@Override
	public String getOsName ()
	{
		return "generic";
	}

	@Override
	public String getOsVersion ()
	{
		return "";
	}

	@Override
	public boolean isMobile ()
	{
		return fIsMobile;
	}

	private final screenInfo fScreen;
	private final boolean fIsMobile;
}
