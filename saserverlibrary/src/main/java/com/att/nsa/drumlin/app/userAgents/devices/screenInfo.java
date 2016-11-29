/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents.devices;

public class screenInfo
{
	public screenInfo ()
	{
		this ( -1, -1, -1 );
	}

	public screenInfo ( int width, int height, int dpi )
	{
		fWidth = width;
		fHeight = height;
		fDpi = dpi;
	}

	public boolean isFixedSize () { return fWidth != -1 || fHeight != -1; };
	public int getWidth () { return fWidth; }
	public int getHeight () { return fHeight; }
	public int getDpi () { return fDpi; }

	private final int fWidth;
	private final int fHeight;
	private final int fDpi;
}
