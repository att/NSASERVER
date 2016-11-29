/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.data;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * create a string that's very unlikely to be guessed
 * @author peter
 */
public class uniqueStringGenerator
{
	public static String create (  )
	{
		final byte[] val = createValue ( 8 );
		return rrConvertor.bytesToHexString ( val );
	}

	public static String createKeyUsingAlphabet ( String alphabet )
	{
		final int alphabetLength = alphabet.length ();
		final byte[] bytes = createValue ( 8 );
		final StringBuffer sb = new StringBuffer ();
		for ( byte b : bytes )
		{
			final int letterIndex = Math.abs ( b ) % alphabetLength;
			final char letter = alphabet.charAt ( letterIndex );
			sb.append ( letter );
		}
		return sb.toString ();
	}

	public static String createKeyUsingAlphabet ( String alphabet, int length )
	{
		String result = createKeyUsingAlphabet (  alphabet );
		while ( result.length () < length )
		{
			result += createKeyUsingAlphabet ( alphabet );
		}
		return result.substring ( 0, length );
	}

	public static String createUrlKey ()
	{
		return createKeyUsingAlphabet (  kUrlKeyAlphabet );
	}

	public static String createMsStyleKeyString ( )
	{
		final String original = createKeyUsingAlphabet (  kLicenseKeyAlphabet );

		final StringBuffer sb = new StringBuffer ();
		int position = -1;
		for ( int i=0; i<original.length (); i++ )
		{
			final char letter = original.charAt ( i );
			position++;
			if ( position > 0 && position % 5 == 0 )
			{
				sb.append ( " " );
			}
			sb.append ( letter );
		}
		return sb.toString ();
	}

	private static final String kLicenseKeyAlphabet = "123456789BCDFGHJKLMNPQRTVWXYZ";
	private static final String kUrlKeyAlphabet = "0123456789ABCDFGHJKLMNPQRTVWXYZabcdefhigjklmnopqrstuvwxyz";

	public static byte[] createValue ( int size )
	{
		try
		{
			final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			byte[] salt = new byte[ size ];
			random.nextBytes(salt);
			return salt;
		}
		catch ( NoSuchAlgorithmException e )
		{
			log.error ( e.getMessage () );
			throw new RuntimeException ( e );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( uniqueStringGenerator.class );
}
