/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import java.util.Map;
import java.util.Map.Entry;

import com.att.nsa.drumlin.till.data.rrConvertor;
import com.att.nsa.drumlin.till.data.rrConvertor.conversionError;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvWriteable;

public abstract class nvBaseReadable implements rrNvReadable
{
	public abstract boolean hasValueFor ( String key );

	public abstract String getString ( String key ) throws missingReqdSetting;

	protected nvBaseReadable ()
	{
	}

	@Override
	public String getString ( String key, String defValue )
	{
		try
		{
			return getString ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
	}

	@Override
	public boolean getBoolean ( String key ) throws missingReqdSetting
	{
		return rrConvertor.convertToBoolean ( getString ( key ) );
	}

	@Override
	public boolean getBoolean ( String key, boolean defValue )
	{
		try
		{
			return getBoolean ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
	}

	@Override
	public int getInt ( String key ) throws missingReqdSetting
	{
		try
		{
			return rrConvertor.convertToInt ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new missingReqdSetting ( key, e );
		}
	}

	@Override
	public int getInt ( String key, int defValue )
	{
		try
		{
			return getInt ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
	}

	@Override
	public double getDouble ( String key ) throws missingReqdSetting
	{
		try
		{
			return rrConvertor.convertToDouble ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new missingReqdSetting ( key, e );
		}
	}

	@Override
	public double getDouble ( String key, double defValue )
	{
		try
		{
			return getDouble ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
	}

	public String[] getStrings ( String key ) throws missingReqdSetting
	{
		final String fullset = getString ( key );
		return fullset.split ( ",", -1 );
	}

	public String[] getStrings ( String key, String[] defValue )
	{
		try
		{
			return getStrings ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
	}

	@Override
	public char getCharacter ( String key ) throws missingReqdSetting
	{
		try
		{
			return rrConvertor.convertToCharacter ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new missingReqdSetting ( key, e );
		}
	}

	@Override
	public char getCharacter ( String key, char defValue )
	{
		try
		{
			return getCharacter ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
	}

	@Override
	public long getLong ( String key ) throws missingReqdSetting
	{
		try
		{
			return rrConvertor.convertToLong ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new missingReqdSetting ( key, e );
		}
	}

	@Override
	public long getLong ( String key, long defValue )
	{
		try
		{
			return getLong ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
	}

	@Override
	public byte[] getBytes ( String key ) throws missingReqdSetting, invalidSettingValue
	{
		try
		{
			return rrConvertor.hexToBytes ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new invalidSettingValue ( key, e );
		}
	}

	@Override
	public byte[] getBytes ( String key, byte[] defValue )
	{
		try
		{
			return getBytes ( key );
		}
		catch ( missingReqdSetting e )
		{
			return defValue;
		}
		catch ( invalidSettingValue e )
		{
			return defValue;
		}
	}
	
	@Override
	public void copyInto ( rrNvWriteable writeable )
	{
		for ( Entry<String, String> e : getCopyAsMap ().entrySet () )
		{
			writeable.set ( e.getKey(), e.getValue () );
		}
	}

	@Override
	public void copyInto ( Map<String, String> writeable )
	{
		for ( Entry<String, String> e : getCopyAsMap ().entrySet () )
		{
			writeable.put ( e.getKey(), e.getValue () );
		}
	}

	@Override
	public void rescan () throws loadException
	{
	}
}
