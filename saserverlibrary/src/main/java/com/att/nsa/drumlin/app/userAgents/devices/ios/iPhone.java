/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents.devices.ios;

import com.att.nsa.drumlin.app.userAgents.devices.genericDevice;
import com.att.nsa.drumlin.app.userAgents.devices.screenInfo;
import com.att.nsa.drumlin.app.userAgents.devices.unknownFixedScreen;

public class iPhone extends genericDevice
{
	public iPhone ()
	{
		super ( new unknownFixedScreen (), true );
	}

	public iPhone ( screenInfo si )
	{
		super ( si, true );
	}

	@Override
	public String getName ()
	{
		return "iPhone";
	}

	@Override
	public String getOsName ()
	{
		return "iOS";
	}
}
