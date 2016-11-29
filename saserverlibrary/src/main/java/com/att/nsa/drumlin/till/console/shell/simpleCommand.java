/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console.shell;

import java.io.PrintStream;
import java.util.HashMap;

import com.att.nsa.drumlin.till.console.rrCmdLineParser;
import com.att.nsa.drumlin.till.console.cmdLinePrefs;
import com.att.nsa.drumlin.till.console.rrConsole.usageException;
import com.att.nsa.drumlin.till.nv.rrNvReadable;

public abstract class simpleCommand implements command
{
	protected simpleCommand ( String cmd )
	{
		this ( cmd, cmd, null );
	}

	protected simpleCommand ( String cmd, String usage )
	{
		this ( cmd, usage, null );
	}

	protected simpleCommand ( String cmd, String usage, String help )
	{
		fCmd = cmd;
		fUsage = usage;
		fHelp = help;
		fArgsParser = new rrCmdLineParser ();
		fPrefs = null;
		fEnabled = true;
	}

	public void enable ( boolean e )
	{
		fEnabled = e;
	}

	public boolean enabled ()
	{
		return fEnabled;
	}

	@Override
	public final void checkArgs ( rrNvReadable basePrefs, String[] args ) throws usageException
	{
		setupParser ( fArgsParser );
		fPrefs = fArgsParser.processArgs ( args );
	}

	@Override
	public String getCommand () { return fCmd; }

	@Override
	public String getUsage () { return fUsage; }

	@Override
	public String getHelp () { return fHelp; }

	@Override
	public final consoleLooper.inResult execute ( HashMap<String,Object> workspace, PrintStream outTo ) throws usageException
	{
		try
		{
			return execute ( workspace, fPrefs, outTo );
		}
		catch ( rrNvReadable.missingReqdSetting e )
		{
			throw new usageException ( e );
		}
	}

	/**
	 * Override this to run the command. 
	 * @param prefs
	 * @param outTo
	 * @return true to continue, false to exit
	 * @throws com.rathravane.rrConsole.ui.console2.console.usageException 
	 * @throws missingReqdSetting 
	 */
	protected abstract consoleLooper.inResult execute ( HashMap<String,Object> workspace, cmdLinePrefs p, PrintStream outTo ) throws usageException, rrNvReadable.missingReqdSetting;

	/**
	 * override this to specify arguments for the command
	 * @param clp
	 */
	protected void setupParser ( rrCmdLineParser clp ) {}

	private final String fCmd;
	private final String fUsage;
	private final String fHelp;

	private final rrCmdLineParser fArgsParser;
	private cmdLinePrefs fPrefs;
	private boolean fEnabled;
}
