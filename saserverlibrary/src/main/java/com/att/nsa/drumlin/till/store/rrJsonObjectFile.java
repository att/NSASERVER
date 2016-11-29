/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class rrJsonObjectFile
{
	public static void initialize ( File file, int blockSize ) throws IOException
	{
		rrBlockFile.initialize ( file, blockSize );
	}

	public rrJsonObjectFile ( File f ) throws IOException
	{
		this ( f, true );
	}

	public rrJsonObjectFile ( File f, boolean withWrite ) throws IOException
	{
		this ( f, withWrite, null );
	}

	public rrJsonObjectFile ( File f, boolean withWrite, String passwd ) throws IOException
	{
		fFile = new rrBlockFile ( f, withWrite, passwd );
	}

	public String getFilePath ()
	{
		return fFile.getFilePath ();
	}
	
	public void close () throws IOException
	{
		fFile.close ();
	}

	public JSONObject read ( long address ) throws IOException
	{
		JSONObject o = null;
		final InputStream is = fFile.readToStream ( address );
		try
		{
			o = new JSONObject ( new JSONTokener ( new InputStreamReader ( is ) ) );
		}
		catch ( JSONException e )
		{
			throw new IOException ( e );
		}
		finally
		{
			is.close ();
		}
		return o;
	}

	public long write ( JSONObject o ) throws IOException
	{
		final byte[] b = o.toString ().getBytes ( Charset.forName ( "UTF-8" ) );
		return fFile.create ( b );
	}

	public void overwrite ( long address, JSONObject o ) throws IOException
	{
		final byte[] b = o.toString ().getBytes ( Charset.forName ( "UTF-8" ) );
		fFile.overwrite ( address, b );
	}

	public void delete ( long address ) throws IOException
	{
		fFile.delete ( address );
	}

	public long indexToAddress ( long index )
	{
		return fFile.indexToAddress ( index );
	}

	private final rrBlockFile fFile;
}
