/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console;

import java.util.Vector;

import com.att.nsa.drumlin.till.nv.impl.nvWriteableTable;

public class cmdLinePrefs extends nvWriteableTable
{
	public cmdLinePrefs ( rrCmdLineParser clp )
	{
		super ();

		fParser = clp;
		fLeftovers = new Vector<String> ();
	}

	/**
	 * get remaining arguments after the options are read
	 * @return a vector of args
	 */
	public Vector<String> getFileArguments ()
	{
		return fLeftovers;
	}

	public String getFileArgumentsAsString ()
	{
		final StringBuffer sb = new StringBuffer ();
		for ( String s : fLeftovers )
		{
			sb.append ( " " );
			sb.append ( s );
		}
		return sb.toString().trim ();
	}
	
	/**
	 * find out if an option was explicitly set by the caller
	 * @param optionWord
	 * @return true or false
	 */
	public boolean wasExplicitlySet ( String optionWord )
	{
		return super.hasValueFor ( optionWord );
	}


	public String getString ( String key ) throws missingReqdSetting
	{
		String result = null;
		if ( wasExplicitlySet ( key ) )
		{
			result = super.getString ( key );
		}
		else
		{
			result = fParser.getArgFor ( key );
		}

		if ( result == null )
		{
			throw new missingReqdSetting ( key );
		}
		return result;
	}

	private final rrCmdLineParser fParser;
	private final Vector<String> fLeftovers;

	void addLeftover ( String val )
	{
		fLeftovers.add ( val );
	}
}
