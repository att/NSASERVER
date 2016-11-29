/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.data.base64;

public class rrcBase64Constants
{
	public static final char kNewline = 10;

	public static char[] nibblesToB64 = new char [64];
	public static byte[] b64ToNibbles = new byte [128];

	static
	{
		int j = 0;
		for ( char c = 'A'; c <= 'Z'; c++ )
		{
			nibblesToB64[j++] = c;
		}
		for ( char c = 'a'; c <= 'z'; c++ )
		{
			nibblesToB64[j++] = c;
		}
		for ( char c = '0'; c <= '9'; c++ )
		{
			nibblesToB64[j++] = c;
		}
		nibblesToB64[j++] = '+';
		nibblesToB64[j++] = '/';

		for ( int i = 0; i < b64ToNibbles.length; i++ )
		{
			b64ToNibbles[i] = -1;
		}
		for ( int i = 0; i < 64; i++ )
		{
			b64ToNibbles[nibblesToB64[i]] = (byte) i;
		}
	}
}
