/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer.util;

public class NsaTestClock extends NsaClock
{
	/**
	 * Create a test clock starting time 1 and set as the system clock.
	 */
	public NsaTestClock ()
	{
		this ( 1 );
	}

	/**
	 * Create a test clock starting at the given time and set as the system clock.
	 * @param nowMs
	 */
	public NsaTestClock ( long nowMs )
	{
		this ( nowMs, true );
	}

	/**
	 * Create a test clock starting time 1 and possibly set as the system clock.
	 * @param nowMs
	 * @param setAsSystemClock
	 */
	public NsaTestClock ( long nowMs, boolean setAsSystemClock )
	{
		fNowMs = nowMs;
		if ( setAsSystemClock )
		{
			NsaClock.setSystemClock ( this );
		}
	}

	@Override
	public synchronized long getCurrentMs ()
	{
		return fNowMs;
	}

	public void tick ()
	{
		addMs ( 1 );
	}

	public synchronized void setTo ( long ms )
	{
		fNowMs = ms;
	}

	public synchronized void addMs ( long ms )
	{
		fNowMs += ms;
	}

	private long fNowMs;
}
