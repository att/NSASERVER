/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.att.nsa.drumlin.service.standards.MimeTypes;

public class NsaAppException extends Exception
{
	public NsaAppException ( JSONObject jsonObject )
	{
		this ( HttpServletResponse.SC_OK, jsonObject );
	}

	public NsaAppException ( int status, String msg )
	{
		this ( status, makeObject ( status, msg ) );
	}

	public NsaAppException ( int status, JSONObject jsonObject )
	{
		super ( "" + status + " " + jsonObject.toString () );

		fStatus = status;
		fBody = jsonObject;
		fType = MimeTypes.kAppJson;
	}

	public int getStatus ()
	{
		return fStatus;
	}

	public String getMediaType ()
	{
		return fType;
	}

	public String getBody ()
	{
		return fBody.toString ();
	}

	private static final long serialVersionUID = 1L;

	private static JSONObject makeObject ( int status, String msg )
	{
		final JSONObject o = new JSONObject ();
		o.put ( "message", msg );
		return o;
	}

	private final String fType;
	private final JSONObject fBody;
	private final int fStatus;
}
