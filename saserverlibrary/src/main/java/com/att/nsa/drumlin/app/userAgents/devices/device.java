/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents.devices;

public interface device
{
	String getName ();
	String getVersion ();

	screenInfo getScreenInfo ();

	String getOsName ();
	String getOsVersion ();

	boolean isMobile ();
}
