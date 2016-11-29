/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui.endpoints;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

public class UiMain
{
	public static void getMain ( DrumlinRequestContext ctx )
	{
		ctx.renderer ().renderTemplate ( "/templates/main.html" );
	}
}
