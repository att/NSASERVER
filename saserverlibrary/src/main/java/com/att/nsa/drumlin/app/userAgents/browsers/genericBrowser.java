/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents.browsers;

public class genericBrowser implements browser
{
	public genericBrowser ()
	{
		this ( "generic", "" );
	}

	public genericBrowser ( String name, String version )
	{
		fName = name;
		fVersion = version;
	}
	
	@Override
	public String getName ()
	{
		return fName;
	}

	@Override
	public String getVersion ()
	{
		return fVersion;
	}

	private final String fName;
	private final String fVersion;
}
