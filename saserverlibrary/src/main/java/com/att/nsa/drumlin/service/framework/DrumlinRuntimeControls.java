/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework;

import java.util.Collection;
import java.util.Map;

import com.att.nsa.drumlin.till.nv.impl.nvBaseReadable;
import com.att.nsa.drumlin.till.nv.impl.nvWriteableTable;

/**
 * The runtime controls are read like regular settings in the system, but are not
 * expected to be cached.
 * 
 * @author peter
 *
 */
public class DrumlinRuntimeControls extends nvBaseReadable
{
	public static final String kSetting_LogHeaders = "drumlin.logging.requestHeaders";

	public DrumlinRuntimeControls ()
	{
		fTable = new nvWriteableTable ();
	}

	public void setLogHeaders ( boolean b )
	{
		fTable.set ( kSetting_LogHeaders, b );
	}
	
	@Override
	public int size ()
	{
		return fTable.size();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		return fTable.getAllKeys();
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		return fTable.getCopyAsMap();
	}

	@Override
	public boolean hasValueFor ( String key )
	{
		return fTable.hasValueFor( key );
	}

	@Override
	public String getString ( String key )
		throws missingReqdSetting
	{
		return fTable.getString ( key );
	}
	
	private final nvWriteableTable fTable;
}
