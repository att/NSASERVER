/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents.devices.android;

import com.att.nsa.drumlin.app.userAgents.devices.genericDevice;
import com.att.nsa.drumlin.app.userAgents.devices.screenInfo;
import com.att.nsa.drumlin.app.userAgents.devices.unknownFixedScreen;

public class androidDevice extends genericDevice
{
	public androidDevice ()
	{
		super ( new unknownFixedScreen (), true );
	}

	public androidDevice ( screenInfo si )
	{
		super ( si, true );
	}

	@Override
	public String getOsName ()
	{
		return "Android";
	}
}
