/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvWriteable;
import com.att.nsa.drumlin.till.nv.impl.nvInstallTypeWrapper;
import com.att.nsa.drumlin.till.nv.impl.nvReadableStack;
import com.att.nsa.drumlin.till.nv.impl.nvWriteableTable;

/**
 * A console program runs on the command line.
 * <p>
 * The console class expects the program's main() routine to call its
 * runFromMain() method to start the system. 
 * 
 * @author peter
 */
public class rrConsole
{
	public static class usageException extends Exception
	{
		public usageException ( String correctUsage ) { super(correctUsage); }
		public usageException ( Exception cause ) { super(cause); }
		private static final long serialVersionUID = 1L;
	}

	public static class startupFailureException extends Exception
	{
		public startupFailureException ( Exception x ) { super(x); }
		public startupFailureException ( String msg ) { super(msg); }
		public startupFailureException ( String msg, Exception x ) { super(msg,x); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * A looper is an object that is run repeatedly (in a loop). This class is
	 * what the main thread of the program does between startup and exit.
	 * 
	 * @author peter
	 *
	 */
	public interface looper
	{
		/**
		 * setup the looper and return true to continue. Called once.
		 * @param p
		 * @param cmdLine
		 * @return true/false
		 */
		boolean setup ( rrNvReadable prefs, cmdLinePrefs cmdLine );

		/**
		 * run a loop iteration, return true to continue, false to exit
		 * @return true to continue, false to exit
		 */
		boolean loop ( rrNvReadable prefs );

		/**
		 * teardown the looper. called once.
		 * @param p
		 */
		void teardown ( rrNvReadable prefs );
	}
	
	protected rrCmdLineParser getCmdLineParser ()
	{
		return fCmdLineParser;
	}

	protected rrConsole ()
	{
		fDefaults = new nvWriteableTable ();
		fCmdLineParser = new rrCmdLineParser ();
	}

	public void runFromMain ( String[] args ) throws Exception
	{
		// get setup
		installShutdownHook ();
		setupDefaults ( fDefaults );
		setupOptions ( fCmdLineParser );

		// parse the command line
		final cmdLinePrefs cmdLine = fCmdLineParser.processArgs ( args );

		// build a preferences stack
		final nvReadableStack stack = new nvReadableStack ();
		stack.push ( fDefaults );
		stack.push ( cmdLine );

		// optionally load more configuration
		final nvInstallTypeWrapper wrapper = new nvInstallTypeWrapper ( stack ); 
		final rrNvReadable config = loadAdditionalConfig ( wrapper );
		if ( config != null )
		{
			stack.pushBelow ( config, cmdLine );
		}

		// init and get the run loop
		final looper l = init ( wrapper, cmdLine );
		if ( l != null )
		{
			if ( l.setup ( wrapper, cmdLine ) )
			{
				while ( l.loop ( wrapper ) ) {}
				l.teardown ( wrapper );
			}
		}

		cleanup ();
	}

	/**
	 * Override this to handle an abrupt shutdown. This method is called when the system exits.
	 */
	protected void onAbruptShutdown () { }

	/**
	 * Override this to setup default settings for the program. 
	 * @param pt
	 */
	protected void setupDefaults ( rrNvWriteable pt ) {}

	/**
	 * Override this to setup recognized command line options.
	 * @param p
	 */
	protected void setupOptions ( rrCmdLineParser p ) {}

	/**
	 * Override this to load additional configuration. If a non-null config is returned,
	 * it's inserted into the preferences stack between the default settings and the command line
	 * settings. That way, the command line arguments have precedence.
	 * @param currentPrefs
	 * @throws loadException 
	 * @throws missingReqdSetting 
	 */
	protected rrNvReadable loadAdditionalConfig ( rrNvReadable currentPrefs ) throws rrNvReadable.loadException, rrNvReadable.missingReqdSetting { return null; }

	/**
	 * Init the program and return a loop instance if the program should continue. The base
	 * class returns null, so you have to override this to do anything beyond init.
	 * @param p
	 * @return non-null to continue, null to exit
	 * @throws missingReqdSetting
	 * @throws invalidSettingValue 
	 */
	protected looper init ( rrNvReadable p, cmdLinePrefs cmdLine ) throws rrNvReadable.missingReqdSetting, rrNvReadable.invalidSettingValue, startupFailureException { return null; }

	/**
	 * Override this to run any cleanup code after the main loop.
	 */
	protected void cleanup () {}

	/**
	 * expand a file argument ("*" matches, etc.)
	 * @param arg
	 * @return
	 * @throws FileNotFoundException
	 */
	protected List<File> expandFileArg ( String arg ) throws FileNotFoundException
	{
		final LinkedList<File> fileList=  new LinkedList<File> ();

		final File file = new File ( arg );
		final File parentDir = file.getParentFile ();
		if ( parentDir != null )
		{
			final String matchPart = file.getName ().replace ( "*", ".*" );	// cmd line regex to java regex
			final Pattern p = Pattern.compile ( matchPart );
	
			final File[] files = parentDir.listFiles ( new FilenameFilter ()
			{
				@Override
				public boolean accept ( File dir, String name )
				{
					return p.matcher ( name ).matches ();
				}
			} );
	
			if ( files != null )
			{
				for ( File f : files )
				{
					fileList.add ( f );
				}
			}
		}
		return fileList;
	}
	
	private final rrCmdLineParser fCmdLineParser;
	private final nvWriteableTable fDefaults;

	private void installShutdownHook ()
	{
		Runtime.getRuntime ().addShutdownHook (
			new Thread ()
			{
				@Override
				public void run ()
				{
					onAbruptShutdown ();
				}
			}
		);
	}
}
