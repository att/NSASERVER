/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author peter
 * @Deprecated use StreamTools in saToolkit - same stuff, just moved
 */
@Deprecated
public class rrStreamTools
{
	protected static final int kBufferLength = 4096;

	/**
	 * Reads the stream into a byte array using a default-sized array for each read,
	 * then closes the input stream.
	 * 
	 * @param is
	 * @param bufferSize
	 * @return a byte array
	 * @throws IOException
	 */
	public static byte[] readBytes ( InputStream is ) throws IOException
	{
		return readBytes ( is, kBufferLength );
	}

	/**
	 * Reads the stream into a byte array using a bufferSize array for each read,
	 * then closes the input stream.
	 * 
	 * @param is
	 * @param bufferSize
	 * @return a byte array
	 * @throws IOException
	 */
	public static byte[] readBytes ( InputStream is, int bufferSize ) throws IOException
	{
		return readBytes ( is, bufferSize, -1 );
	}

	/**
	 * Reads the stream into a byte array using a bufferSize array for each read,
	 * then closes the input stream. If limit >= 0, at most limit bytes are read.<br/>
	 * Note: even with a negative limit, 2GB is the limit.
	 * 
	 * @param is
	 * @param bufferSize
	 * @param limit
	 * @return a byte array
	 * @throws IOException
	 */
	public static byte[] readBytes ( InputStream is, int bufferSize, int limit ) throws IOException
	{
		int counter = 0;
		final int atMost = limit < 0 ? Integer.MAX_VALUE : Math.min ( limit, Integer.MAX_VALUE );

		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		if ( is != null )
		{
			byte[] b = new byte [ bufferSize ];
			int len = 0;
			do
			{
				len = is.read ( b );
				if ( -1 != len )
				{
					final int readNow = Math.min ( len, atMost - counter );
					baos.write ( b, 0, readNow );
					counter += readNow;
				}
			}
			while ( len != -1 && counter < atMost );
			is.close ();
		}

		return baos.toByteArray ();
	}

	/**
	 * Copy from the input stream to the output stream, then close the output stream.
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copyStream ( InputStream in, OutputStream out ) throws IOException
	{
		copyStream ( in, out, kBufferLength );
	}

	/**
	 * Copy from the input stream to the output stream, then close the output stream.
	 * @param in
	 * @param out
	 * @param bufferSize
	 * @throws IOException
	 */
	public static void copyStream ( InputStream in, OutputStream out, int bufferSize ) throws IOException
	{
		final byte[] buffer = new byte [bufferSize];
		int len;
		while ( ( len = in.read ( buffer ) ) != -1 )
		{
			out.write ( buffer, 0, len );
		}
		out.close ();
	}
}
