/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.LoggerFactory;

public class consoleLineReader
{
	public static String getLine ( String prompt ) throws IOException
	{
		return sfReader.getLine ( prompt );
	}

	private interface reader 
	{
		String getLine ( String prompt ) throws IOException;
	}

	static
	{
		jline.console.ConsoleReader cr = null;
		if ( Boolean.parseBoolean ( System.getProperty ( "rrJline", "true" ) ) )
		{
			try
			{
				cr = new jline.console.ConsoleReader ();
			}
			catch ( IOException e )
			{
				LoggerFactory.getLogger ( consoleLineReader.class ).warn ( "IOException initializing JLine. Falling back to standard Java I/O." );
				cr = null;
			}
		}

		if ( cr != null )
		{
			final jline.console.ConsoleReader crf = cr;
			sfReader = new reader ()
			{
				@Override
				public String getLine ( String prompt ) throws IOException
				{
					return crf.readLine ( prompt );
				}
			};
		}
		else
		{
			final BufferedReader br = new BufferedReader ( new InputStreamReader ( System.in ) );
			sfReader = new reader ()
			{
				@Override
				public String getLine ( String prompt ) throws IOException
				{
					System.out.print ( prompt );
					System.out.flush ();
					return br.readLine ();
				}
			};
		}
	}

	private static final reader sfReader;
}
