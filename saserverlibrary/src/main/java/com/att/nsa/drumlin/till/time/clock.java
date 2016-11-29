/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.time;

import java.util.concurrent.TimeUnit;

/**
 * Basic clock service, replaces System.currentTimeMillis(), but with test access.
 * 
 * @author peter
 */
public class clock
{
	public static long now ()
	{
		return holder.instance.nowMs ();
	}

	/**
	 * Provided for testing only. 
	 * @param c
	 */
	public static void replaceClock ( clock c )
	{
		holder.instance = c;
	}

	/**
	 * Switch to a test clock and return that instance. Equivalent to instantiating
	 * a clock.testClock and calling replaceClock() with it.
	 * 
	 * @return a test clock.
	 */
	public static testClock useNewTestClock ()
	{
		final testClock tc = new testClock ();
		replaceClock ( tc );
		return tc;
	}

	protected long nowMs ()
	{
		return System.currentTimeMillis ();
	}

	private static class holder
	{
		// volatile: harmless in normal runs, as this is a singleton constructed
		// once and shared among threads (all cache the same reference). For test
		// runs (e.g. from JUnit), it ensures that replaceClock() takes effect in
		// all threads immediately.
		static volatile clock instance = new clock ();
	}

	/**
	 * A simple testing clock.
	 * @author peter
	 */
	public static class testClock extends clock
	{
		@Override
		public long nowMs () { return nowMs; }

		public void set ( long ms ) { nowMs = ms; }
		public void add ( long ms ) { nowMs += ms; }
		public void add ( long val, TimeUnit tu )
		{
			add ( TimeUnit.MILLISECONDS.convert ( val, tu ) );
		}

		private long nowMs = 1;
	}
}
