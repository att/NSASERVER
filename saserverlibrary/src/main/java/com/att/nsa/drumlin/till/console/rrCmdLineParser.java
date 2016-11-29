/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console;

import java.util.HashMap;
import java.util.TreeSet;

import com.att.nsa.drumlin.till.console.rrConsole.usageException;
import com.att.nsa.drumlin.till.data.rrConvertor;

/**
 * Assists in reading command line settings.
 */
public class rrCmdLineParser
{
	public rrCmdLineParser ()
	{
		fSingleToWord = new HashMap<Character,String> ();
		fWordsNeedingValues = new TreeSet<String> ();
		fOptions = new HashMap<String,option> ();
		fMinFiles = 0;
		fMaxFiles = Integer.MAX_VALUE;
	}

	/**
	 * Register a boolean option. 
	 * @param word e.g. "force"
	 * @param singleChar e.g. "f". 
	 * @param defValue
	 */
	public void registerOnOffOption ( String word, Character singleChar, boolean defValue )
	{
		if ( word == null )
		{
			throw new IllegalArgumentException ( "An option 'word' is required." );
		}

		if ( singleChar != null )
		{
			fSingleToWord.put ( singleChar, word );
		}

		fOptions.put ( word, new onOff ( defValue ) );
	}

	public void registerOptionWithValue ( String word )
	{
		registerOptionWithValue ( word, null, null, null );
	}

	/**
	 * register an option that takes a value
	 * @param word the full word for this option, e.g. "verbose"
	 * @param singleChar a single char representation of this option, e.g. "v". Can be null.
	 * @param defValue the default value for the option if none is provided
	 * @param allowed if not null, a limited range of values for the option
	 */
	public void registerOptionWithValue ( String word, String singleChar, String defValue, String[] allowed )
	{
		if ( word == null )
		{
			throw new IllegalArgumentException ( "An option 'word' is required." );
		}

		if ( singleChar != null )
		{
			if ( singleChar.length () > 1 )
			{
				throw new IllegalArgumentException ( singleChar + " is not a single character." );
			}
			fSingleToWord.put ( singleChar.charAt ( 0 ), word );
		}

		fWordsNeedingValues.add ( word );
		fOptions.put ( word, new setting ( word, defValue, allowed ) );
	}

	/**
	 * allows no file arguments
	 */
	public void requireNoFileArguments ()
	{
		requireFileArguments ( 0, 0 );
	}

	/**
	 * allows exactly one file argument
	 */
	public void requireOneFileArgument ()
	{
		requireFileArguments ( 1, 1 );
	}

	/**
	 * sets the range for required file args from the given min to no max
	 * @param min
	 */
	public void requireMinFileArguments ( int min )
	{
		requireFileArguments ( min, Integer.MAX_VALUE );
	}

	/**
	 * require a specific number of file arguments
	 * @param exactly
	 */
	public void requireFileArguments ( int exactly )
	{
		requireFileArguments ( exactly, exactly );
	}

	/**
	 * set a range for file arg count for the parser 
	 * @param min 0 or higher
	 * @param max 0 or higher, use Integer.MAX_VALUE for no max
	 */
	public void requireFileArguments ( int min, int max )
	{
		fMinFiles = min;
		fMaxFiles = max;
	}
	
	/**
	 * find out if an option has a value (or is just on/off)
	 * @param optionWord
	 * @return the value for the option
	 */
	public boolean hasArg ( String optionWord )
	{
		return ( fOptions.get ( optionWord ) != null );
	}

	/**
	 * find out if a boolean option has been set
	 * @param optionWord
	 * @return true if the value for the option is set
	 */
	public boolean isSet ( String optionWord )
	{
		final option o = fOptions.get ( optionWord );
		return ( o != null ) ? rrConvertor.convertToBoolean (o.getDefault()) : false;
	}

	/**
	 * get the default value for a given option
	 * @param optionWord
	 * @return the default value for the option
	 */
	public String getArgFor ( String optionWord )
	{
		final option o = fOptions.get ( optionWord );
		return ( o != null ) ? o.getDefault () : "";
	}

	/**
	 * reads command line arguments
	 * @param args
	 */
	public cmdLinePrefs processArgs ( String[] args ) throws usageException
	{
		final cmdLinePrefs prefs = new cmdLinePrefs ( this );

		boolean seenDashDash = false;
		int i=0;
		for ( ; i<args.length && !seenDashDash; i++ )
		{
			final String item = args[i];
			if ( item.equals ( "--" ) )
			{
				seenDashDash = true;
			}
			else if ( item.startsWith ( "--" ) )
			{
				// a word, which could take an argument
				final String word = item.substring ( 2 );
				if ( reqsValue ( word ) )
				{
					if ( i+1 == args.length )
					{
						throw new usageException ( "Option " + item + " requires an argument." );
					}
					else
					{
						handleOption ( prefs, word, args[i+1] );
						i++;	// forward one
					}
				}
				else
				{
					handleOption ( prefs, word );
				}
			}
			else if ( item.startsWith ( "-" ) )
			{
				final int len = item.length ();
				if ( len == 1 )
				{
					throw new usageException ( "Can't process '-' alone." );
				}
				else if ( item.length () == 2 )
				{
					// an option, which could take an argument
					char c = item.charAt ( 1 );
					final String word = fSingleToWord.get ( c );
					if ( word == null )
					{
						throw new usageException ( "Option '" + c + "' is invalid." );
					}
					if ( reqsValue ( word ) )
					{
						if ( i==args.length )
						{
							throw new usageException ( "Option " + item + " requires an argument." );
						}
						else
						{
							handleOption ( prefs, word, args[i+1] );
							i++;	// forward one
						}
					}
					else
					{
						handleOption ( prefs, word );
					}
				}
				else
				{
					// an option set...
					for ( char c : item.substring ( 1 ).toCharArray () )
					{
						final String optWord = fSingleToWord.get ( c );
						if ( optWord == null )
						{
							throw new usageException ( "Option '" + c + "' is invalid." );
						}
						handleOption ( prefs, optWord );
					}
				}
			}
			else
			{
				// a plain word (not an option)
				seenDashDash = true;
				break;
			}
		}

		int leftoverCount = 0;
		while ( i<args.length )
		{
			prefs.addLeftover ( args[i] );
			i++;
			leftoverCount++;
		}

		if ( leftoverCount < fMinFiles || leftoverCount > fMaxFiles )
		{
			throw new usageException ( getErrorMsgForWrongCount ( leftoverCount ) );
		}
		
		return prefs;
	}

	private static String plural ( String word, int count )
	{
		return ( count == 1 ? word : word + "s" );
	}

	private String getErrorMsgForWrongCount ( int count )
	{
		if ( fMinFiles == fMaxFiles )
		{
			if ( fMinFiles == 0 )
			{
				return "You may not provide any arguments.";
			}
			else
			{
				return "You must provide " + fMinFiles + " " + plural("argument",fMinFiles) + ".";
			}
		}
		else
		{
			final String minPart = ( fMinFiles <= 0 ? null : "at least " + fMinFiles + " " + plural("argument",fMinFiles) );
			final String maxPart = ( fMaxFiles == Integer.MAX_VALUE ? null : "at most " + fMaxFiles + " " + plural("argument",fMaxFiles) );

			return "You must provide " +
				( minPart != null ? minPart : "" ) +
				( minPart != null && maxPart != null ? " and " : "" ) +
				( maxPart != null ? maxPart : "" ) +
				".";
		}
	}

	private boolean reqsValue ( String optWord )
	{
		return fWordsNeedingValues.contains ( optWord );
	}

	private void handleOption ( cmdLinePrefs prefs, String optWord ) throws usageException
	{
		handleOption ( prefs, optWord, Boolean.TRUE.toString () );
	}

	private void handleOption ( cmdLinePrefs prefs, String optWord, String value ) throws usageException
	{
		final option o = fOptions.get ( optWord );
		if ( o == null )
		{
			throw new usageException ( "Unrecognized option " + optWord );
		}

		final String valToUse = o.checkValue ( value );
		prefs.set ( optWord, valToUse );
	}

	private interface option
	{
		String checkValue ( String val ) throws usageException;
		String getDefault ();
	}

	private class onOff implements option
	{
		public onOff ( boolean defVal )
		{
			fValue = defVal;
		}

		@Override
		public String checkValue ( String val )
		{
			return "" + rrConvertor.convertToBooleanBroad ( val );
		}

		@Override
		public String getDefault ()
		{
			return Boolean.toString ( fValue );
		}

		private final boolean fValue;
	}

	private class setting implements option
	{
		public setting ( String name, String defVal, String[] allowed )
		{
			fSetting = name;
			fValue = defVal;
			fAllowed = allowed;
		}
		
		@Override
		public String checkValue ( String val ) throws usageException
		{
			boolean canSet = true;
			if ( fAllowed != null )
			{
				canSet = false;
				for ( String a : fAllowed )
				{
					if ( a.equals ( val ) )
					{
						canSet = true;
						break;
					}
				}
				if ( !canSet )
				{
					throw new usageException ( "Value " + val + " is not allowed for setting " + fSetting );
				}
			}
			return val;
		}

		@Override
		public String getDefault ()
		{
			return fValue;
		}

		private final String fSetting;
		private String fValue;
		private String[] fAllowed;
	}

	private final HashMap<Character, String> fSingleToWord;
	private final TreeSet<String> fWordsNeedingValues;
	private final HashMap<String, option> fOptions;
	private int fMinFiles;
	private int fMaxFiles;
}
