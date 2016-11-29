/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class oneWayHasher
{
	public static String digest ( String input )
	{
		return hash ( input, "" );
	}

	/**
	 * return a 20 byte (160 bit) hash of the input string, using a salt, if provided.
	 * @param input
	 * @param moreSalt
	 * @return 20 bytes
	 */
	@Deprecated
	public static byte[] hashToBytes ( String input, String moreSalt )
	{
		try
		{
			final StringBuffer fullMsg = new StringBuffer ();
			fullMsg.append ( kPart1 );
			fullMsg.append ( input );
			fullMsg.append ( kPart2 );
			if ( moreSalt != null )
			{
				fullMsg.append ( moreSalt );
			}

			final MessageDigest md = MessageDigest.getInstance ( "SHA-1" );
			md.reset ();
			md.update ( fullMsg.toString().getBytes () );
			return md.digest ();	// 160 bits
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException ( "MessageDigest can't find SHA-1 implementation." );
		}
	}

	public static String pbkdf2HashToString ( String input, String salt )
	{
		final byte[] bytes = pbkdf2Hash ( input, salt );
		return rrConvertor.bytesToHexString ( bytes );
	}

	public static byte[] pbkdf2Hash ( String input, String salt )
	{
		try
		{
			final String algorithm = "PBKDF2WithHmacSHA1";
			final int derivedKeyLength = 160;
			final int iterations = 20000;

			final KeySpec spec = new PBEKeySpec(input.toCharArray(), salt.getBytes (), iterations, derivedKeyLength);
			final SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
			return f.generateSecret(spec).getEncoded();
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( InvalidKeySpecException e )
		{
			throw new RuntimeException ( e );
		}
	}

	@Deprecated
	public static String hash ( String input, String moreSalt )
	{
		final byte[] outBytes = hashToBytes ( input, moreSalt );
		return rrConvertor.bytesToHexString ( outBytes );
	}

	static public void main ( String args[] )
	{
		if ( args.length != 1 && args.length != 2 )
		{
			System.err.println ( "usage: oneWayHasher <input> [<extraSalt>]" );
		}
		else if ( args.length == 1 )
		{
			System.out.println ( hash ( args[0], "" ) );
		}
		else if ( args.length == 2 )
		{
			System.out.println ( hash ( args[0], args[1] ) );
		}
	}

	static private final String kPart1 = "In the fields of Rathravane, yell '";
	static private final String kPart2 = "!!!', and you'll find a rock thrown by a giant. ";
}
