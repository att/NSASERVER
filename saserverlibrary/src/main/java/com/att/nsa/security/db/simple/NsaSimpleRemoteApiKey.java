/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.db.simple;

import org.json.JSONObject;

public class NsaSimpleRemoteApiKey extends NsaSimpleApiKey
{
	public NsaSimpleRemoteApiKey ( JSONObject data )
	{
		super ( data );
	}

	@Override
	public String getSecret ()
	{
		throw new IllegalStateException ( "This is a read-only API key that does not carry a secret." );
	}

	@Override
	public void enable ()
	{
		throw new IllegalStateException ( "This is a read-only API key." );
	}

	@Override
	public void disable ()
	{
		throw new IllegalStateException ( "This is a read-only API key." );
	}

	@Override
	public void set ( String key, String val )
	{
		throw new IllegalStateException ( "This is a read-only API key." );
	}
}
