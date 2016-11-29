/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer.streams;

import java.io.IOException;
import java.io.InputStream;

public class ChunkedInputStream extends InputStream
{
	private static final int kCapacity = 1024 * 4;

	public ChunkedInputStream ( InputStream base )
	{
		fBase = base;
		fBuffer = new byte[kCapacity];
		fPos = 0;
		fLen = 0;
		fClosed = false;
		fRemainingInChunk = 0;
	}

	@Override
	public void close () throws IOException
	{
		fBase.close ();
		fClosed = true;
	}

	@Override
	public int read () throws IOException
	{
		while ( !fClosed )
		{
			readMore ();
			if ( fLen > 0 )
			{
				int result = fBuffer[fPos++];
				fLen--;
				return result;
			}
		}
		return -1;
	}

	private final InputStream fBase;
	private final byte[] fBuffer;
	private boolean fClosed; 
	private int fPos;
	private int fLen;
	private int fRemainingInChunk;

	private int readWithBlock ( byte[] bytes, int pos, int len ) throws IOException
	{
		int total = 0;
		while ( total < len )
		{
			final int now = fBase.read ( bytes, pos + total, len - total );
			if ( now < 0 )
			{
				close ();
				return -1;
			}

			total += now;
		}
		return total;
	}
	
	private void readMore () throws IOException
	{
		if ( fLen == 0 )
		{
			if ( fRemainingInChunk == 0 )
			{
				// start a new chunk
				final int chunkLen = readChunkLength ( fBase );
				if ( chunkLen > 0 )
				{
					fRemainingInChunk = chunkLen;
				}
				else if ( chunkLen == kClosed )
				{
					close ();
				}
				else if ( chunkLen == 0 )
				{
					// last chunk, read CRLF, 
					final byte[] bytes = new byte[2];
					if ( readWithBlock ( bytes, 0, 2 ) >= 0 )
					{
						if ( bytes[0] != '\r' || bytes[1] != '\n' )
						{
							throw new IOException ( "Chunked encoding format error." );
						}
					}
					fClosed = true;	// there's no more here, but don't close the underlying stream
				}
			}

			if ( fRemainingInChunk > 0 )
			{
				fLen = fBase.read ( fBuffer, 0, fRemainingInChunk );
				if ( fLen == -1 )
				{
					close ();
					return;
				}

				fPos = 0;
				fRemainingInChunk -= fLen;

				if ( fRemainingInChunk == 0 )
				{
					// read trailing \r\n
					final byte[] bytes = new byte[2];
					if ( readWithBlock ( bytes, 0, 2 ) >= 0 )
					{
						if ( bytes[0] != '\r' || bytes[1] != '\n' )
						{
							throw new IOException ( "Chunked encoding format error." );
						}
					}
				}
			}
		}
		// else: wait until what's read is consumed
	}

	private static int kClosed = -1;
	private static int kNotAvailable = -2;
	
	private static int readChunkLength ( InputStream is ) throws IOException
	{
		if ( 0 == is.available () )
		{
			return kNotAvailable;
		}

		int result = 0;

		int c = is.read ();
		while ( true )
		{
			if ( c == -1 )
			{
				return kClosed;
			}

			int val = 0;
			c = Character.toLowerCase ( c );
			if ( c >= '0' && c <= '9' )
			{
				val = ( c - '0' );
			}
			else if ( c >= 'a' && c <= 'f' )
			{
				val = ( c - 'a' ) + 10;
			}
			else if ( c == '\r' )
			{
				break;
			}
			else
			{
				throw new IOException ( "Chunked encoding format error." );
			}

			result = ( result * 16 ) + val;
			c = is.read();
		}

		c = is.read ();
		if ( c == -1 )
		{
			return kClosed;
		}
		else if ( c == '\n' )
		{
			// okay
		}
		else
		{
			// bogus
			throw new IOException ( "Chunked encoding format error." );
		}
		
		return result;
	}
}
