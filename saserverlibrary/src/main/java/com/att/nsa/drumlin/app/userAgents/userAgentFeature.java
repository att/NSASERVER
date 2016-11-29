/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents;

public class userAgentFeature
{
	public String getName () { return fName; }
	public String getVersion () { return fVersion; }
	public String getComment () { return fComment; }

	private final String fName;
	private final String fVersion;
	private final String fComment;
	
	userAgentFeature ( String name )
	{
		this ( name, "", "" );
	}

	userAgentFeature ( String name, String version )
	{
		this ( name, version, "" );
	}

	userAgentFeature ( String name, String version, String comment )
	{
		fName = name == null ? "" : name;
		fVersion = version == null ? "" : version;
		fComment = comment == null ? "" : comment;
	}
}
