/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents.devices;

public class unknownFixedScreen extends screenInfo
{
	public unknownFixedScreen ()
	{
		super ( -1, -1, -1 );
	}

	@Override
	public boolean isFixedSize () { return true; };
}
