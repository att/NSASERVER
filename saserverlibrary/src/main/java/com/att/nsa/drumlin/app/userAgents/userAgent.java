/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents;

public interface userAgent
{
	String getDeviceName ();
	String getOsName ();
	String getOsVersion ();

	String getBrowserCanonicalName ();
	String getBrowserCanonicalVersion ();

	boolean getIsMobile ();

	boolean getIsFixedScreenSize ();
	int getScreenWidth ();
	int getScreenHeight ();
	int getScreenDpi ();
}
