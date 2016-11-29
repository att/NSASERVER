/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.service.framework;

import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletConfig;

import com.att.nsa.drumlin.till.nv.impl.nvReadableTable;

/**
 * Wraps a ServletConfig in the settings class used throughout the Drumlin
 * framework.
 */
public class DrumlinServletSettings extends nvReadableTable
{
	public DrumlinServletSettings ( ServletConfig sc )
	{
		super ();

		final HashMap<String,String> loaded = new HashMap<String,String> ();

		final Enumeration<String> e = sc.getInitParameterNames ();
		while ( e.hasMoreElements () )
		{
			final String name = e.nextElement ();
			final String val = sc.getInitParameter ( name );
			loaded.put ( name, val );
		}

		set ( loaded );
	}
}
