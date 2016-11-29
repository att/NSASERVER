/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console.shell;

import java.io.PrintStream;
import java.util.HashMap;

import com.att.nsa.drumlin.till.console.rrConsole.usageException;
import com.att.nsa.drumlin.till.nv.rrNvReadable;

public interface command
{
	String getCommand ();

	/**
	 * check the arguments provided
	 * @param args
	 */
	void checkArgs ( rrNvReadable p, String[] args ) throws usageException;

	/**
	 * @return a string used for the help command to show simple usage
	 */
	String getUsage ();

	/**
	 * @return a string used for the help command to show detail info
	 */
	String getHelp ();

	/**
	 * @param outTo
	 * @return true to continue, false to exit
	 * @throws com.rathravane.rrConsole.ui.console2.console.usageException 
	 */
	consoleLooper.inResult execute ( HashMap<String,Object> workspace, PrintStream outTo ) throws usageException;
}
