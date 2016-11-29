/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.util;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import com.att.nsa.drumlin.service.standards.MimeTypes;

/**
 * Write JSON objects to a Drumlin response.
 * @author peter@rathravane.com
 */
public class JsonBodyWriter
{
	/**
	 * Write a list of JSON objects to the response stream in the given context, with a 
	 * 200 status code.
	 * 
	 * @param context
	 * @param objects
	 * @throws IOException
	 */
	public static void writeObjectList ( DrumlinRequestContext context, List<JSONObject> objects ) throws IOException
	{
		final JSONArray out = new JSONArray ( objects );
		context.response ().
			setStatus ( HttpStatusCodes.k200_ok ).
			setContentType ( MimeTypes.kAppJson ).
			send ( out.toString () + "\n" );
	}
}
