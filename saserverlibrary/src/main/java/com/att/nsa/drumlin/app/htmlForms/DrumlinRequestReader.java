/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

/**
 * A request reader. Is this in use??
 * @author peter@rathravane.com
 *
 */
@Deprecated
public class DrumlinRequestReader
{
	public static class invalidRequest extends Exception
	{
		public invalidRequest ( String msg )
		{
			super ( msg );
		}
		private static final long serialVersionUID = 1L;
	}

	public static class badInputRequest extends invalidRequest
	{
		public badInputRequest ( String key, String msg )
		{
			super ( msg );
			fKey = key;
			fMsg = msg;
		}
		public final String fKey;
		public final String fMsg;
		private static final long serialVersionUID = 1L;
	}

	public DrumlinRequestReader ()
	{
		fFieldMap = new HashMap<String,fieldInfo> ();
	}

	public void registerArgument ( String key, boolean reqd, boolean notEmpty, String defVal, String[] limitTo )
	{
		final fieldInfo fi = new fieldInfo ();
		fi.fDefVal = defVal;
		fi.fReqd = reqd;
		fi.fNotEmpty = notEmpty;
		fi.fValueLimitedTo = limitTo;

		fFieldMap.put ( key, fi );
	}

	public Map<String,String> read ( HttpServletRequest req ) throws invalidRequest
	{
		final HashMap<String,String> valueMap = new HashMap<String,String> ();

		// start with required defaults
		for ( final Entry<String, fieldInfo> e : fFieldMap.entrySet () )
		{
			final String key = e.getKey ();
			final fieldInfo fi = e.getValue ();
			if ( fi.fReqd && fi.fDefVal != null )
			{
				valueMap.put ( key, fi.fDefVal );
			}
		}

		// read the input
		final Map<?,?> params = req.getParameterMap ();
		for ( Entry<?, ?> e : params.entrySet () )
		{
			final String p = e.getKey().toString ();
			final String[] vArray = (String[]) e.getValue();
			final String v = vArray.length > 0 ? vArray[0] : null;

			final fieldInfo fi = fFieldMap.get ( p );
			if ( fi != null )
			{
				if ( fi.fValueLimitedTo != null )
				{
					boolean found = false;
					for ( int i=0; !found && i<fi.fValueLimitedTo.length; i++ )
					{
						found = fi.fValueLimitedTo[i].equals ( v );
					}
					if ( !found )
					{
						final StringBuffer sb = new StringBuffer ();
						sb.append ( "Invalid value: [" + v + "]; it must be one of: " );
						for ( String s : fi.fValueLimitedTo )
						{
							sb.append ( s );
							sb.append ( " " );
						}
						throw new badInputRequest ( p, sb.toString () );
					}
				}
				valueMap.put ( p, v );
			}
		}

		// validate the settings
		for ( final Entry<String, fieldInfo> e : fFieldMap.entrySet () )
		{
			final String key = e.getKey ();
			final fieldInfo fi = e.getValue ();
			if ( fi.fReqd )
			{
				final String val = valueMap.get ( key );
				if ( val == null || ( fi.fNotEmpty && val.length() == 0 ) )
				{
					throw new badInputRequest ( key, "Please provide a value!" );
				}
			}
			else if ( fi.fNotEmpty )
			{
				final String val = valueMap.get ( key );
				if ( val != null && val.length() == 0 )
				{
					throw new badInputRequest ( key, "Please provide a value!" );
				}
			}
		}

		return valueMap;
	}

	private class fieldInfo
	{
		public fieldInfo ()
		{
			fDefVal = null;
			fReqd = false;
			fValueLimitedTo = null;
		}

		public String fDefVal;
		public boolean fReqd;
		public boolean fNotEmpty;
		public String[] fValueLimitedTo;
	}
	private HashMap<String,fieldInfo> fFieldMap;
}
