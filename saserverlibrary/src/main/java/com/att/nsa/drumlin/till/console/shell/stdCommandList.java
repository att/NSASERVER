/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console.shell;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import com.att.nsa.drumlin.till.console.rrCmdLineParser;
import com.att.nsa.drumlin.till.console.cmdLinePrefs;

public class stdCommandList implements commandList
{
	public stdCommandList ()
	{
		this ( true );
	}

	public stdCommandList ( boolean withStdCommands )
	{
		fCommands = new HashMap<String,command> ();
		if ( withStdCommands )
		{
			addStandardCommands ();
		}
	}

	public void registerCommand ( command c )
	{
		fCommands.put ( c.getCommand (), c );
	}

	public void removeCommand ( String key )
	{
		fCommands.remove ( key );
	}

	public void clearCommands ()
	{
		fCommands.clear ();
	}

	public void addStandardCommands ()
	{
		registerCommand ( new simpleCommand ( "exit" )
		{
			@Override
			public consoleLooper.inResult execute ( HashMap<String,Object> workspace, cmdLinePrefs prefs, PrintStream outTo ) { return consoleLooper.inResult.kQuit; }
		} );
		registerCommand ( new simpleCommand ( "quit" )
		{
			@Override
			public consoleLooper.inResult execute ( HashMap<String,Object> workspace, cmdLinePrefs prefs, PrintStream outTo ) { return consoleLooper.inResult.kQuit; }
		} );
		registerCommand ( new simpleCommand ( "help", "help [<command>]" )
		{
			@Override
			protected void setupParser ( rrCmdLineParser clp )
			{
				clp.requireFileArguments ( 0, 1 );
			}

			@Override
			public consoleLooper.inResult execute ( HashMap<String,Object> workspace, cmdLinePrefs prefs, PrintStream outTo )
			{
				if ( prefs.getFileArguments ().size() == 1 )
				{
					final String cmdText = prefs.getFileArguments ().firstElement ();
					final command cmd = fCommands.get ( cmdText );
					if ( cmd != null )
					{
						outTo.println ( "    " + cmd.getUsage () );
						final String help = cmd.getHelp ();
						if ( help != null )
						{
							outTo.println ();
							outTo.println ( help );
						}
					}
					else
					{
						outTo.println ( "Unknown command: " + cmdText + "." );
					}
				}
				else
				{
					outTo.println  ( "The available commands are:" );
					outTo.println  ();
					
					final LinkedList<String> commands = new LinkedList<String> ();
					commands.addAll ( getAllCommands () );
					Collections.sort ( commands );
					for ( String cmd : commands )
					{
						outTo.println ( "    " + cmd );
					}

					outTo.println  ();
					outTo.println  ( "You can type 'help <command>' to get more info on that command." );
				}
				return consoleLooper.inResult.kReady;
			}
		} );
	}

	public Collection<String> getAllCommands ()
	{
		return fCommands.keySet ();
	}

	@Override
	public command getCommandFor ( String cmd )
	{
		return fCommands.get ( cmd );
	}

	private final HashMap<String,command> fCommands;
}
