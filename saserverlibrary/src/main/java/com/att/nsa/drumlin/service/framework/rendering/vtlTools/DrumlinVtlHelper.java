/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.rendering.vtlTools;

import com.att.nsa.drumlin.till.data.rrConvertor;

public class DrumlinVtlHelper
{
	public String noBreakingSpace ( String e )
	{
		return replace ( e, " ", "&nbsp;" );
	}

	public String replace ( String e, String from, String to )
	{
		return e.replace ( from, to );
	}

	public String encode ( String e )
	{
		return rrConvertor.urlEncode ( e );
	}
}
