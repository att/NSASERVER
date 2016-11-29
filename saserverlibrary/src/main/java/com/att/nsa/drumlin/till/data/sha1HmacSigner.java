/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.data;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class sha1HmacSigner
{
	private static final String kHmacSha1Algo = "HmacSHA1";

	public static String sign ( String message, String key )
	{
		try
		{
			final SecretKey secretKey = new SecretKeySpec ( key.getBytes (), kHmacSha1Algo );
			final Mac mac = Mac.getInstance ( kHmacSha1Algo );
			mac.init ( secretKey );
			final byte[] rawHmac = mac.doFinal ( message.getBytes () );
			return rrConvertor.base64Encode ( rawHmac );
		}
		catch ( InvalidKeyException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( IllegalStateException e )
		{
			throw new RuntimeException ( e );
		}
	}

	static public void main ( String args[] )
	{
		if ( args.length != 2 )
		{
			System.err.println ( "usage: sha1HmacSigner <message> <key>" );
		}
		else if ( args.length == 2 )
		{
			System.out.println ( sign ( args[0], args[1] ) );
		}
	}
}
