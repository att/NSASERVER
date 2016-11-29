/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv;

import java.util.Map;

/**
 * Write interface for a name/value pair container.
 * 
 * @author peter@rathravane.com
 */
public interface rrNvWriteable extends rrNvReadable
{
	void clear ();
	void unset ( String key );

	void set ( String key, String value );
	void set ( String key, char value );
	void set ( String key, boolean value );
	void set ( String key, int value );
	void set ( String key, long value );
	void set ( String key, double value );
	void set ( String key, byte[] value );
	void set ( String key, byte[] value, int offset, int length );
	void set ( String key, String[] value );
	void set ( Map<String,String> map );
}
