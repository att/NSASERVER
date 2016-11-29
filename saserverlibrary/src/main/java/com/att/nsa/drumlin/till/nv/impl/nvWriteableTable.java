/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv.impl;

import java.util.Map;

import com.att.nsa.drumlin.till.data.rrConvertor;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvWriteable;

public class nvWriteableTable extends nvReadableTable implements rrNvWriteable
{
	public nvWriteableTable ()
	{
		super ();
	}

	public nvWriteableTable ( rrNvReadable that )
	{
		super ( that == null ? null : that.getCopyAsMap () );
		if ( that != null )
		{
			for ( String key : that.getAllKeys () )
			{
				set ( key, that.getString ( key, null ) );
			}
		}
	}

	@Override
	public synchronized void set ( String key, String value )
	{
		super.set ( key, value );
	}

	@Override
	public void set ( String key, char value )
	{
		super.set ( key, "" + value );
	}

	public synchronized void set ( String key, int value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( String key, long value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( String key, double value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( String key, boolean value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( Map<String,String> map )
	{
		super.set ( map );
	}

	@Override
	public synchronized void unset ( String key )
	{
		super.clear ( key );
	}

	@Override
	public synchronized void set ( String key, byte[] value )
	{
		set ( key, value, 0, value.length );
	}

	@Override
	public synchronized void set ( String key, byte[] value, int offset, int length )
	{
		set ( key, rrConvertor.bytesToHex ( value, offset, length ) );
	}

	@Override
	public void set ( String key, String[] values )
	{
		final StringBuffer sb = new StringBuffer ();
		boolean one = false;
		for ( String value : values )
		{
			if ( one ) sb.append ( "," );
			sb.append ( value );
			one = true;
		}
		set ( key, sb.toString () );
	}
}
