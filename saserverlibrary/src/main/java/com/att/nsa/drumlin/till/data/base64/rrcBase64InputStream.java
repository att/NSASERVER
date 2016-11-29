/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.data.base64;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class rrcBase64InputStream extends InputStream
{
	public rrcBase64InputStream ( InputStream upstream )
	{
		fUpstream = upstream;
		fPendingOutput = new LinkedList<Byte> ();
		fEndOfStream = false;
	}

	private final LinkedList<Byte> fPendingOutput;

	@Override
	public int available () throws IOException
	{
		fillBuffer ();
		return fPendingOutput.size ();
	}

	@Override
	public int read ()
		throws IOException
	{
		int result = -1;

		fillBuffer ();
		if ( fPendingOutput.size () > 0 )
		{
			result = fPendingOutput.remove ();
			if ( result < 0 )
			{
				result += 256;
			}
		}
		else if ( !fEndOfStream )
		{
			result = 0;
		}
		return result;
	}

	private byte[] decode ( char[] in )
	{
		int iLen = in.length;
		if ( iLen % 4 != 0 )
			throw new IllegalArgumentException (
				"Length of Base64 encoded input string is not a multiple of 4." );
		while ( iLen > 0 && in[iLen - 1] == '=' )
			iLen--;
		int oLen = ( iLen * 3 ) / 4;
		byte[] out = new byte [oLen];
		int ip = 0;
		int op = 0;
		while ( ip < iLen )
		{
			int i0 = in[ip++];
			int i1 = in[ip++];
			int i2 = ip < iLen ? in[ip++] : 'A';
			int i3 = ip < iLen ? in[ip++] : 'A';
			if ( i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127 )
				throw new IllegalArgumentException (
					"Illegal character in Base64 encoded data." );
			int b0 = rrcBase64Constants.b64ToNibbles[i0];
			int b1 = rrcBase64Constants.b64ToNibbles[i1];
			int b2 = rrcBase64Constants.b64ToNibbles[i2];
			int b3 = rrcBase64Constants.b64ToNibbles[i3];
			if ( b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0 )
				throw new IllegalArgumentException (
					"Illegal character in Base64 encoded data." );
			int o0 = ( b0 << 2 ) | ( b1 >>> 4 );
			int o1 = ( ( b1 & 0xf ) << 4 ) | ( b2 >>> 2 );
			int o2 = ( ( b2 & 3 ) << 6 ) | b3;
			out[op++] = (byte) o0;
			if ( op < oLen )
				out[op++] = (byte) o1;
			if ( op < oLen )
				out[op++] = (byte) o2;
		}
		return out;
	}

	private final InputStream fUpstream;
	private boolean fEndOfStream;

	private void fillBuffer () throws IOException
	{
		if ( fPendingOutput.size () == 0 )
		{
			// skip any newlines
			int first = fUpstream.read ();
			while ( first == rrcBase64Constants.kNewline )
			{
				first = fUpstream.read ();
			}

			if ( first == -1 )
			{
				fEndOfStream = true;
			}
			else
			{
				// read 3 more bytes
				byte[] next3 = new byte[3];
				int readFromNext3 = fUpstream.read ( next3 );
				if ( readFromNext3 != 3 )
				{
					throw new IOException ( "rrcBase64InputStream expects 4 bytes from its underlying stream but got " + 
						(1+readFromNext3) );
				}
	
				char[] block = new char[4];
				block[0] = (char)((byte) first);
				for ( int i=0; i<3; i++ ) { block[i+1]=(char)next3[i]; }
	
				byte[] decoded = decode ( block );
				for ( byte b : decoded )
				{
					fPendingOutput.add ( b );
				}
			}
		}
	}
}
