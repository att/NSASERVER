/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.att.nsa.drumlin.till.data.base64.rrcBase64InputStream;
import com.att.nsa.drumlin.till.data.base64.rrcBase64OutputStream;

public class rrConvertor
{
	public static class conversionError extends Exception
	{
		public conversionError ( String msg ) { super ( msg ); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * equivalent to Boolean.parseBoolean(s)
	 * @param s
	 * @return true/false
	 */
	static public boolean convertToBoolean ( String s )
	{
		return Boolean.parseBoolean(s);
	}

	/**
	 * a broader conversion of common strings for boolean values (e.g. yes/no,
	 * true/false, on/off). A null argument produces false.
	 * @param s
	 * @return true/false
	 */
	static public boolean convertToBooleanBroad ( String s )
	{
		boolean result =
		(
			s != null && (
				convertToBoolean ( s ) ||
				s.equalsIgnoreCase("true") ||
				s.equalsIgnoreCase("yes") ||
				s.equalsIgnoreCase("on") ||
				s.equalsIgnoreCase("1") ||
				s.equalsIgnoreCase("y") ||
				s.equalsIgnoreCase("checked")
			)
		);
		return result;
	}

	static public boolean convertToBoolean ( int i )
	{
		return ( i != 0 );
	}

	static public boolean convertToBoolean ( long i )
	{
		return ( i != 0L );
	}

	static public int convertToInt ( String s ) throws conversionError
	{
		int result = 0;
		if ( s != null )
		{
			try
			{
				result = Integer.parseInt ( s );
			}
			catch ( NumberFormatException e )
			{
				throw new conversionError ( "couldn't convert '" + s + "' to an integer" );
			}
		}
		return result;
	}

	static public int convertToInt ( String s, int errval )
	{
		try
		{
			return convertToInt ( s );
		}
		catch ( conversionError e )
		{
			return errval;
		}
	}

	static public long convertToLong ( String s ) throws conversionError
	{
		long result = 0;
		if ( s != null )
		{
			try
			{
				result = Long.parseLong ( s );
			}
			catch ( NumberFormatException e )
			{
				throw new conversionError ( "couldn't convert " + s + " to a long" );
			}
		}
		return result;
	}

	static public long convertToLong ( String s, long errval )
	{
		try
		{
			return convertToLong ( s );
		}
		catch ( conversionError e )
		{
			return errval;
		}
	}

	static public short convertToShort ( String s ) throws conversionError
	{
		short result = 0;
		if ( s != null )
		{
			try
			{
				result = Short.parseShort ( s );
			}
			catch ( NumberFormatException e )
			{
				throw new conversionError ( "couldn't convert " + s + " to a short" );
			}
		}
		return result;
	}

	static public long convertToShort ( String s, long errval )
	{
		try
		{
			return convertToShort ( s );
		}
		catch ( conversionError e )
		{
			return errval;
		}
	}

	public static double convertToDouble ( String s ) throws conversionError
	{
		double result = 0.0;
		if ( s != null )
		{
			try
			{
				result = Double.parseDouble ( s );
			}
			catch ( NumberFormatException e )
			{
				throw new conversionError ( "couldn't convert " + s + " to a double" );
			}
		}
		return result;
	}

	static public double convertToDouble ( String s, double errval )
	{
		try
		{
			return convertToDouble ( s );
		}
		catch ( conversionError e )
		{
			return errval;
		}
	}

	public static float convertToFloat ( String s ) throws conversionError
	{
		float result = 0.0f;
		if ( s != null )
		{
			try
			{
				result = Float.parseFloat ( s );
			}
			catch ( NumberFormatException e )
			{
				throw new conversionError ( "couldn't convert " + s + " to a double" );
			}
		}
		return result;
	}

	static public float convertToFloat ( String s, float errval )
	{
		try
		{
			return convertToFloat ( s );
		}
		catch ( conversionError e )
		{
			return errval;
		}
	}

	public static char convertToCharacter ( String s ) throws conversionError
	{
		if ( s == null || s.length () != 1 )
		{
			throw new conversionError ( "Expected a string with length 1." );
		}
		return s.charAt ( 0 );
	}

	static public float convertToCharacter ( String s, char errval )
	{
		try
		{
			return convertToCharacter ( s );
		}
		catch ( conversionError e )
		{
			return errval;
		}
	}

	public static byte[] convert ( int[] bytes ) throws conversionError
	{
		int index=0;
		byte[] result = new byte [ bytes.length ];
		for ( int i : bytes )
		{
			if ( i < 0 || i > 255 )
			{
				throw new conversionError ( "In conversion to byte[], int[] contains value " + i );
			}
			result[index++] = (byte) i;
		}
		return result;
	}

	public static int[] convert ( byte[] bytes )
	{
		int index=0;
		int[] result = new int [ bytes.length ];
		for ( byte b : bytes )
		{
			if ( b < 0 )
			{
				result[index++] = (int) b + 256;
			}
			else
			{
				result[index++] = (int) b;
			}
		}
		return result;
	}

	public static String convertToString ( int[] bytes, int offset, int length ) throws conversionError
	{
		StringBuffer sb = new StringBuffer ();
		for ( int i=offset; i<offset+length; i++ )
		{
			final int c = bytes[i];
			if ( c < 0 || c > 255 )
			{
				throw new conversionError ( "Byte array encoded as int[] contains value " + c + ", which is out of range." );
			}

			int topNibble = c >>> 4;
			int bottomNibble = c & 0x0f;

			sb.append ( nibbleToChar ( topNibble ) );
			sb.append ( nibbleToChar ( bottomNibble ) );
		}
		return sb.toString ();
	}

	public static char nibbleToChar ( int c ) throws conversionError
	{
		if ( c < 0 || c > 15 )
		{
			throw new conversionError ( "Value " + c + " is not a valid nibble value." );
		}
		if ( c < 10 )
		{
			return (char)((int)'0' + c);
		}
		else
		{
			return (char)((int)'A' + c - 10);
		}
	}

	public static int charToNibble ( char c ) throws conversionError
	{
		char d = Character.toLowerCase ( c );
		if ( d >= '0' && d <= '9' )
		{
			return ((int)d) - '0';
		}
		else if ( d >= 'a' && d <= 'f' )
		{
			return ((int)d) - 'a' + 10;
		}
		else
		{
			throw new conversionError ( "Character [" + c + "] is not a valid nibble." );
		}
	}
	public static int[] convertToByteArray ( String s ) throws conversionError
	{
		int len = s.length ();
		if ( len % 2 != 0 )
		{
			throw new conversionError ( "When converting to byte[], input string must be even length." );
		}

		int[] buffer = new int [ len / 2 ];
		int index = 0;
		for ( int i=0; i<len; i+=2 )
		{
			char top = s.charAt ( i );
			char bottom = s.charAt ( i+1 );
			buffer[index++] = charToNibble ( top ) * 16 + charToNibble ( bottom );
		}
		
		return buffer;
	}

	static public int[] convertToByteArray ( String s, int[] errval )
	{
		try
		{
			return convertToByteArray ( s );
		}
		catch ( conversionError e )
		{
			return errval;
		}
	}

	public static byte[] hexToBytes ( String s ) throws conversionError
	{
		return convert ( convertToByteArray ( s ) );
	}

	public static String byteToHex ( byte b )
	{
		StringBuffer sb = new StringBuffer ();
		sb.append ( nibbleToHex ( (b & 0xf0) >> 4 ) );
		sb.append ( nibbleToHex ( b & 0x0f ) );
		return sb.toString ();
	}

	public static String byteToHex ( int i )
	{
		return byteToHex ( (byte)( i & 0xff ));
	}

	public static String bytesToHex ( byte[] bb )
	{
		return bytesToHex ( bb, 0, bb.length );
	}

	public static String bytesToHex ( byte[] bb, int offset, int length )
	{
		StringBuffer sb = new StringBuffer ();
		final int total = offset + length;
		for ( int i=offset; i<total; i++ )
		{
			byte b = bb[i];
			sb.append ( nibbleToHex ( ( b & 0xf0 ) >> 4 ) );
			sb.append ( nibbleToHex ( b & 0x0f ) );
		}
		return sb.toString ();
	}

	public static String stringToHex ( String s )
	{
		return bytesToHex ( s.getBytes () );
	}

	public static String hexBytesToString ( String s ) throws conversionError
	{
		final byte[] bytes = hexToBytes ( s );
		return new String ( bytes );
	}

	public static String urlEncode ( String s )
	{
		if ( s == null ) return null;

		try
		{
			return URLEncoder.encode ( s, "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new RuntimeException ( e );
		}
	}
	
	public static String urlDecode ( String s )
	{
		if ( s == null ) return null;

		try
		{
			return URLDecoder.decode ( s, "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new RuntimeException ( e );
		}
	}

	private static char nibbleToHex ( int c )
	{
		int result = c + '0';
		if ( c > 9 )
		{
			result = c - 10 + 'A';
		}
		return (char) result;
	}

	/**
	 * encode the source string so that any occurrence of 'special' is duplicated
	 * @param source
	 * @param special
	 * @return an encoded string
	 */
	public static String encode ( String source, char special )
	{
		return encode ( source, special, new char[]{}, new char[]{} );
	}

	/**
	 * encode the source string so that illegal chars are replaced by the replacement chars,
	 * with an escape sequence started by 'special'. If 'special' occurs, it'll occur twice
	 * in the encoded string.
	 * 
	 * @param source
	 * @param special
	 * @param illegals
	 * @param replacements
	 * @return an encoded string
	 */
	public static String encode ( String source, char special, char[] illegals, char[] replacements )
	{
		final String illStr = new String ( illegals );

		final StringBuffer sb = new StringBuffer ();
		for ( char c : source.toCharArray () )
		{
			if ( c == special )
			{
				sb.append ( special );
				sb.append ( special );
			}
			else
			{
				int pos = illStr.indexOf ( c );
				if ( pos == -1 )
				{
					sb.append ( c );
				}
				else
				{
					sb.append ( special );
					sb.append ( replacements [ pos ] );
				}
			}
		}
		return sb.toString ();
	}

	public static String decode ( String encoded, char special )
	{
		return decode ( encoded, special, new char[]{}, new char[]{} );
	}

	public static String decode ( String encoded, char special, char[] illegals, char[] replacements )
	{
		final String repStr = new String ( replacements );

		final StringBuffer sb = new StringBuffer ();
		char chars[] = encoded.toCharArray ();
		for ( int i=0; i<chars.length; i++ )
		{
			if ( chars[i] == special && (i+1 < chars.length) )
			{
				if ( chars[i+1] == special )
				{
					sb.append ( special );
					i++;
				}
				else
				{
					int pos = repStr.indexOf ( chars[i+1] );
					if ( pos != -1 )
					{
						sb.append ( illegals[pos] );
						i++;	// eat the next char too 
					}
					// else: shrug...
				}
			}
			else
			{
				sb.append ( chars[i] );
			}
		}
		return sb.toString ();
	}

	public static String base64Encode ( String in )
	{
		try
		{
			return base64Encode ( in.getBytes ( "UTF-8" ) );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new RuntimeException ( "Missing UTF-8 encoding in Java installation??" );
		}
	}

	public static String base64Encode ( byte[] in )
	{
		try
		{
			final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
			final rrcBase64OutputStream os = new rrcBase64OutputStream ( baos );
			os.write ( in );
			os.close ();
			final byte[] textBytes = baos.toByteArray ();
			return new String ( textBytes );
		}
		catch ( IOException e )
		{
			throw new RuntimeException ( "Error writing bytes to byte stream. " + e.getMessage () );
		}
	}

	public static byte[] base64Decode ( String in )
	{
		try
		{
			final ByteArrayInputStream sis = new ByteArrayInputStream ( in.getBytes ("UTF-8") );
			final rrcBase64InputStream is = new rrcBase64InputStream ( sis );

			final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
			final byte[] bytes = new byte [1024];
			int len = 0;
			while ( -1 != ( len = is.read ( bytes ) ) )
			{
				baos.write ( bytes, 0, len );
			}
			baos.close ();

			is.close ();

			return baos.toByteArray ();
		}
		catch ( IOException e )
		{
			throw new RuntimeException ( "Error writing bytes to byte stream. " + e.getMessage () );
		}
	}

	public static String bytesToHexString ( byte[] bytes )
	{
		final StringBuffer sb = new StringBuffer ();
		for ( byte b : bytes )
		{
			final String bstr = Integer.toHexString ( (int)(b&0xff) );
			if ( bstr.length () < 2 ) sb.append ( "0" );	// putting a 0 into the buffer before the value
			sb.append ( bstr );
		}
		return sb.toString ();
	}

	public static byte[] hexStringToBytes ( String key )
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		while ( key.length () > 0 )
		{
			final String part = key.substring ( 0, 2 );
			key = key.substring ( 2 );

			final int partValue = Integer.parseInt ( part , 16 );
			baos.write ( partValue );
		}
		return baos.toByteArray ();
	}
}
