/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import com.att.nsa.drumlin.till.data.rrConvertor;
import com.att.nsa.drumlin.till.nv.rrNvWriteable;

public abstract class nvBaseWriteable extends nvBaseReadable implements rrNvWriteable
{
	@Override
	public void set ( String key, char value )
	{
		set ( key, "" + value );
	}

	@Override
	public void set ( String key, boolean value )
	{
		set ( key, new Boolean ( value ).toString () );
	}

	@Override
	public void set ( String key, int value )
	{
		set ( key, new Integer ( value ).toString () );
	}

	@Override
	public void set ( String key, long value )
	{
		set ( key, new Long ( value ).toString () );
	}

	@Override
	public void set ( String key, double value )
	{
		set ( key, new Double ( value ).toString () );
	}

	@Override
	public void set ( String key, byte[] value )
	{
		set ( key, rrConvertor.bytesToHex ( value ) );
	}

	@Override
	public void set ( String key, byte[] value, int offset, int length )
	{
		set ( key, rrConvertor.bytesToHex ( value, offset, length ) );
	}

	@Override
	public void set ( String key, String[] values )
	{
		final StringBuffer sb = new StringBuffer ();
		boolean some = false;
		for ( String val : values )
		{
			if ( some ) sb.append ( "," );
			sb.append ( val );
			some = true;
		}
		set ( key, sb.toString () );
	}
}
