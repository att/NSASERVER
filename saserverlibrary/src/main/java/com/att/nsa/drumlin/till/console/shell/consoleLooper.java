/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console.shell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Vector;

import com.att.nsa.drumlin.till.console.rrConsole;
import com.att.nsa.drumlin.till.console.consoleLineReader;
import com.att.nsa.drumlin.till.console.cmdLinePrefs;
import com.att.nsa.drumlin.till.console.rrConsole.usageException;
import com.att.nsa.drumlin.till.nv.rrNvReadable;

/**
 * implements a looper that repeatedly prompts for a command line and handles it
 * @author peter
 */
public class consoleLooper implements rrConsole.looper
{
	public consoleLooper ( String[] headerLines, String prompt, String secondaryPrompt, commandList cl )
	{
		fHeaderLines = headerLines;
		fInputQueue = new LinkedList<String> ();
		fPrompt = prompt;
		fSecondaryPrompt = secondaryPrompt;
		fEnableHelp = true;
		fWorkspace = new HashMap<String,Object> ();
		fState = inResult.kReady;
		fCommands = cl;
	}

	public enum inResult
	{
		kReady,
		kSecondaryPrompt,
		kQuit
	};

	/**
	 * this key is used for a boolean setting that will suppress the copyright notice 
	 */
	public static final String kSetting_Quiet = "quiet";

	@Override
	public boolean setup ( rrNvReadable p, cmdLinePrefs clp )
	{
		boolean quiet = p.getBoolean ( kSetting_Quiet, false );

		if ( clp != null )
		{
			final String args = clp.getFileArgumentsAsString ();
			if ( args != null && args.length () > 0 )
			{
				queue ( args );
				queue ( "quit" );
				quiet = true;
			}
		}

		if ( !quiet )
		{
			writeHeader ();
		}

		return true;
	}

	@Override
	public void teardown ( rrNvReadable p )
	{
	}

	@Override
	public boolean loop ( rrNvReadable p )
	{
		boolean result = true;
		try
		{
			final String line = getInput ();
			fState = handleInput ( p, line, System.out );
			if ( fState == null )
			{
				fState = inResult.kReady;
			}
		}
		catch ( IOException x )
		{
			// a break in console IO, we're done
			System.err.println ( x.getMessage () );
			result = false;
		}
		return result && !fState.equals ( inResult.kQuit );
	}

	public synchronized void queue ( String input )
	{
		fInputQueue.add ( input );
	}

	public synchronized void queueFromCmdLine ( cmdLinePrefs clp, boolean withQuit )
	{
		final Vector<String> args = clp.getFileArguments ();
		if ( args.size () > 0 )
		{
			final StringBuffer sb = new StringBuffer ();
			for ( String s : args )
			{
				sb.append ( s );
				sb.append ( ' ' );
			}
			queue ( sb.toString () );
			if ( withQuit ) queue ( "quit" );
		}
	}

	protected void writeHeader ()
	{
		if ( fHeaderLines != null )
		{
			for ( String header : fHeaderLines )
			{
				System.out.println ( header );
			}
		}
	}

	/**
	 * override this to handle input before its been parsed in the usual way
	 * @param input
	 * @param outputTo
	 * @return true to continue, false to exit
	 */
	protected inResult handleInput ( rrNvReadable p, String input, PrintStream outputTo )
	{
		inResult result = inResult.kReady;

		final String[] commandLine = splitLine ( input );
		if ( commandLine.length == 0 )
		{
			result = handleEmptyLine ( p, outputTo );
		}
		else
		{
			result = handleCommand ( p, commandLine, outputTo );
		}

		return result;
	}

	/**
	 * default handling for empty lines -- just ignore them
	 * @return true
	 */
	protected inResult handleEmptyLine ( rrNvReadable p, PrintStream outputTo )
	{
		return inResult.kReady;
	}

	/**
	 * consoles can override this to change how command lines are processed
	 * @param commandLine
	 * @param outputTo
	 * @return true to continue, false to exit
	 */
	protected inResult handleCommand ( rrNvReadable p, String[] commandLine, PrintStream outputTo )
	{
		inResult result = inResult.kReady;
		final command m = getHandler ( commandLine ); 
		if ( m != null )
		{
			final int argsLen = commandLine.length - 1;
			final String[] args = new String [ argsLen ];
			System.arraycopy ( commandLine, 1, args, 0, argsLen );

			try
			{
				result = invoke ( m, p, args, outputTo );
			}
			catch ( Exception x )
			{
				result = handleInvocationException ( commandLine, x, outputTo );
			}
		}
		else
		{
			result = handleUnrecognizedCommand ( commandLine, outputTo );
		}
		return result;
	}

	protected inResult invoke ( command m, rrNvReadable p, String[] args, PrintStream outputTo )
	{
		try
		{
			m.checkArgs ( p, args );
			return m.execute ( fWorkspace, outputTo );
		}
		catch ( usageException x )
		{
			outputTo.println ( m.getUsage () );
			outputTo.println ( x.getMessage () );
		}
		return inResult.kReady;
	}
	
	/**
	 * default handling for unrecognized commands
	 * @return inResult.kReady
	 */
	protected inResult handleUnrecognizedCommand ( String[] commandLine, PrintStream outputTo )
	{
		outputTo.println ( "Unrecognized command '" + commandLine[0] + "'." );
		return inResult.kReady;
	}

	/**
	 * default handling for invocation problems
	 * @return inResult.kReady
	 */
	protected inResult handleInvocationException ( String[] commandLine, Exception x, PrintStream outputTo )
	{
		if ( x instanceof InvocationTargetException )
		{
			InvocationTargetException itc = (InvocationTargetException) x;
			Throwable target = itc.getTargetException ();
			outputTo.println ( "ERROR: " + target.getClass ().getName () + ": " + target.getMessage () );
		}
		else
		{
			outputTo.println ( "Error running command '" + commandLine[0] + "'. " + x.getMessage() );
		}
		return inResult.kReady;
	}

	protected HashMap<String,Object> getWorkspace ()
	{
		return fWorkspace;
	}
	
	private String[] fHeaderLines;
	private final LinkedList<String> fInputQueue;
	private final String fPrompt;
	private final String fSecondaryPrompt;
	private boolean fEnableHelp;
	private inResult fState;
	private final commandList fCommands;
	private final HashMap<String,Object> fWorkspace;

	public static final String kCmdPrefix = "__";
	public static final int kCmdPrefixLength = kCmdPrefix.length ();

	private synchronized String getInput () throws IOException
	{
		String input = null;
		if ( fInputQueue.size () > 0 )
		{
			input = fInputQueue.remove ();
		}
		else
		{
			String prompt = fPrompt;
			if ( fState == inResult.kReady )
			{
				System.out.println ();
			}
			else
			{
				prompt = fSecondaryPrompt;
			}

			input = consoleLineReader.getLine ( prompt );
			if ( input == null )
			{
				input = "";
			}
		}
		return input;
	}

	/**
	 * split a string on its whitespace into individual tokens
	 * @param line
	 * @return split array
	 */
	static String[] splitLine ( final String line )
	{
		final LinkedList<String> tokens = new LinkedList<String> ();

		StringBuffer current = new StringBuffer ();
		boolean quoting = false;
		for ( int i=0; i<line.length (); i++ )
		{
			final char c = line.charAt ( i );
			if ( Character.isWhitespace ( c ) && !quoting )
			{
				if ( current.length () > 0 )
				{
					tokens.add ( current.toString () );
				}
				current = new StringBuffer ();
			}
			else if ( c == '"' )
			{
				// if we see "abc"def, that's "abc", then "def"
				if ( current.length () == 0 )
				{
					// starting quoted string. eat it, flip quote flag
					quoting = true;
				}
				else if ( !quoting )
				{
					// abc"def
					tokens.add ( current.toString () );
					current = new StringBuffer ();
					quoting = true;
				}
				else
				{
					// end quoted string
					tokens.add ( current.toString () );
					current = new StringBuffer ();
					quoting = false;
				}
			}
			else
			{
				current.append ( c );
			}
		}
		if ( current.length () > 0 )
		{
			tokens.add ( current.toString () );
		}
		
		return tokens.toArray ( new String[ tokens.size () ] );
	}

	private command getHandler ( String[] cmdLine )
	{
		if ( cmdLine.length > 0 )
		{
			return fCommands.getCommandFor ( cmdLine[0] );
		}
		else
		{
			return null;
		}
	}

	public void __script ( String[] args, PrintStream outTo ) throws rrConsole.usageException, IOException
	{
		if ( args.length != 2 )
		{
			throw new rrConsole.usageException ( "script <file>" );
		}

		LinkedList<String> lines = new LinkedList<String> ();
		final String filename = args[1];
		final BufferedReader bis = new BufferedReader ( new FileReader ( filename ) );
		String input;
		while ( ( input = bis.readLine () ) != null )
		{
			lines.add ( input );
		}
		bis.close ();

		// add to the front of the queue so that script within script runs in
		// correct order.
		synchronized ( this )
		{
			fInputQueue.addAll ( 0, lines );
		}
	}

	public void __help ( String[] args, PrintStream outTo ) throws rrConsole.usageException, IOException
	{
		if ( fEnableHelp )
		{
			TreeSet<String> allMethods = new TreeSet<String> ();

			Class<?> clazz = getClass ();
			while ( !clazz.equals ( Object.class ) )
			{
				Method[] methods = clazz.getDeclaredMethods ();
				for ( Method m : methods )
				{
					final String methodName = m.getName ();
					if ( methodName.startsWith ( kCmdPrefix ) &&
						methodName.length () > kCmdPrefixLength )
					{
						Class<?>[] params = m.getParameterTypes ();
						if ( params.length == 2 && params[0].equals ( String[].class ) && 
							params[1].equals ( PrintStream.class ) )
						{
							allMethods.add ( methodName.substring ( kCmdPrefixLength ) );
						}
					}
				}
				clazz = clazz.getSuperclass ();
			}

			for ( String s : allMethods )
			{
				outTo.println ( "    " + s );
			}
		}
	}
}
