/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class HumanReadableHelper
{
	private static final long kKilobyte = 1024;
	private static final long kMegabyte = 1024 * kKilobyte;
	private static final long kGigabyte = 1024 * kMegabyte;
	private static final long kTerabyte = 1024 * kGigabyte;
	private static final long kPetabyte = 1024 * kTerabyte;
	private static final long kExabyte = 1024 * kPetabyte;

	public static String byteCountValue ( long inBytes )
	{
		String result = "" + inBytes + " bytes";
		if ( inBytes > kExabyte )
		{
			double d = inBytes / kExabyte;
			result = "" + d + " EB";
		}
		else if ( inBytes > kPetabyte )
		{
			double d = inBytes / kPetabyte;
			result = "" + d + " PB";
		}
		else if ( inBytes > kTerabyte )
		{
			double d = inBytes / kTerabyte;
			result = "" + d + " TB";
		}
		else if ( inBytes > kGigabyte )
		{
			double d = inBytes / kGigabyte;
			result = "" + d + " GB";
		}
		else if ( inBytes > kMegabyte )
		{
			double d = inBytes / kMegabyte;
			result = "" + d + " MB";
		}
		else if ( inBytes > kKilobyte )
		{
			double d = inBytes / kKilobyte;
			result = "" + d + " KB";
		}
		return result;
	}

	@Deprecated
	public static String memoryValue ( long inBytes )
	{
		return byteCountValue ( inBytes );
	}

	public static final long kSecond = 1000;
	public static final long kMinute = 60 * kSecond;
	public static final long kHour = 60 * kMinute;
	public static final long kDay = 24 * kHour;
	public static final long kWeek = 7 * kDay;
	public static final long kMonth = 30 * kDay;
	public static final long kYear = 365 * kDay;

	public static String numberValue ( long units )
	{
		final StringBuffer sb = new StringBuffer ();

		final String raw = "" + units;
		final int count = raw.length ();
		final int firstPart = count % 3;
		int printed = 3 - firstPart;
		for ( int i=0; i<count; i++ )
		{
			if ( printed == 3 )
			{
				if ( sb.length () > 0 )
				{
					sb.append ( ',' );
				}
				printed = 0;
			}
			sb.append ( raw.charAt ( i ) );
			printed++;
		}

		return sb.toString ();
	}

	public static String ratioValue ( double d )
	{
		// FIXME: use formatter
		double rounded2 = Math.round ( d * 100 ) / 100.0;
		return "" + rounded2;
	}

	public static String pctValue ( double d )
	{
		// FIXME: use formatter
		final long pct = Math.round ( d * 100 );
		return "" + pct + "%";
	}

	public static String dateValue ( Date d )
	{
		return sdf.format ( d );
	}
	private static final SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy.MM.dd HH:mm:ss z" );
	
	public static String elapsedTimeSince ( Date d )
	{
		// return elapsed time with precision that's scaled back as the time grows distant
		long unit = 1;
		final long elapsedMs = System.currentTimeMillis () - d.getTime ();

		// over 5 seconds, report in seconds
		if ( elapsedMs > 1000 * 5 )
		{
			unit = 1000;
		}

		// over 5 minutes, report in minutes
		if ( elapsedMs > 1000*60*5 )
		{
			unit = 1000 * 60;
		}

		// over 5 hours, report in hours
		if ( elapsedMs > 1000*60*60*5 )
		{
			unit = 1000 * 60 * 60;
		}

		// over 5 days, report in days
		if ( elapsedMs > 1000*60*60*24*5 )
		{
			unit = 1000 * 60 * 60 * 24;
		}

		// over 5 weeks, report in weeks
		if ( elapsedMs > 1000*60*60*24*7*5 )
		{
			unit = 1000 * 60 * 60 * 24 * 7;
		}

		// over 5 months, report in months
		if ( elapsedMs > 1000*60*60*24*30*5 )
		{
			unit = 1000 * 60 * 60 * 24 * 30;
		}

		// over 2 years, report in years
		if ( elapsedMs > 1000*60*60*24*365*2 )
		{
			unit = 1000 * 60 * 60 * 24 * 365;
		}

		return elapsedTimeSince ( d, unit );
	}

	public static String elapsedTimeSince ( Date d, long smallestUnitInMillis )
	{
		if ( d == null )
		{
			return "";
		}

		final Date now = new Date ();
		final long elapsedMs = now.getTime () - d.getTime ();
		if ( elapsedMs < 0 )
		{
			return timeValue ( elapsedMs * -1, TimeUnit.MILLISECONDS, smallestUnitInMillis ) + " in the future";
		}
		else
		{
			return timeValue ( elapsedMs, TimeUnit.MILLISECONDS, smallestUnitInMillis ) + " ago";
		}
	}

	public static String timeValue ( long units, TimeUnit tu, long smallestUnit )
	{
		final long timeInMs = TimeUnit.MILLISECONDS.convert ( units, tu );
		
		String result = "" + timeInMs + " ms";
		if ( timeInMs > kYear )
		{
			final long years = timeInMs / kYear;
			final long remaining = timeInMs - ( years * kYear );
			result = "" + years + " yrs";
			if ( remaining > smallestUnit )
			{
				result += ", ";
				result += timeValue ( remaining, TimeUnit.MILLISECONDS, smallestUnit );
			}
		}
		else if ( timeInMs > kMonth )
		{
			final long months = timeInMs / kMonth;
			final long remaining = timeInMs - ( months * kMonth );
			result = "" + months + " months";
			if ( remaining > smallestUnit )
			{
				result += ", ";
				result += timeValue ( remaining, TimeUnit.MILLISECONDS, smallestUnit );
			}
		}
		else if ( timeInMs > kWeek )
		{
			final long weeks = timeInMs / kWeek;
			final long remaining = timeInMs - ( weeks * kWeek );
			result = "" + weeks + " wks";
			if ( remaining > smallestUnit )
			{
				result += ", ";
				result += timeValue ( remaining, TimeUnit.MILLISECONDS, smallestUnit );
			}
		}
		else if ( timeInMs > kDay )
		{
			final long days = timeInMs / kDay;
			final long remaining = timeInMs - ( days * kDay );
			result = "" + days + " days";
			if ( remaining > smallestUnit )
			{
				result += ", ";
				result += timeValue ( remaining, TimeUnit.MILLISECONDS, smallestUnit );
			}
		}
		else if ( timeInMs > kHour )
		{
			final long hrs = timeInMs / kHour;
			final long remaining = timeInMs - ( hrs * kHour );
			result = "" + hrs + ( hrs == 1 ? " hr" : " hrs" );
			if ( remaining > smallestUnit )
			{
				result += ", ";
				result += timeValue ( remaining, TimeUnit.MILLISECONDS, smallestUnit );
			}
		}
		else if ( timeInMs > kMinute )
		{
			final long mins = timeInMs / kMinute;
			final long remaining = timeInMs - ( mins * kMinute );
			result = "" + mins + " m";
			if ( remaining > smallestUnit )
			{
				result += ", ";
				result += timeValue ( remaining, TimeUnit.MILLISECONDS, smallestUnit );
			}
		}
		else if ( timeInMs > kSecond )
		{
			final long seconds = timeInMs / kSecond;
			final long remaining = timeInMs - ( seconds * kSecond );
			result = "" + seconds + " s";
			if ( remaining > smallestUnit )
			{
				result += ", ";
				result += timeValue ( remaining, TimeUnit.MILLISECONDS, smallestUnit );
			}
		}
		else
		{
			result = "" + timeInMs + " ms";
		}
		return result;
	}
}
