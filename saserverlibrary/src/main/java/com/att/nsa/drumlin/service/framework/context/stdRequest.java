/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

class StdRequest implements DrumlinRequest
{
	public StdRequest ( HttpServletRequest r )
	{
		fRequest = r;
		fParamOverrides = new HashMap<String,String[]> ();
	}

	@Override
	public String getUrl ()
	{
		return fRequest.getRequestURL ().toString ();
	}

	@Override
	public boolean isSecure ()
	{
		return fRequest.isSecure ();
	}

	@Override
	public String getQueryString ()
	{
		final String qs = fRequest.getQueryString ();
		if ( qs != null && qs.length () == 0 ) return null;
		return qs;
	}
	
	@Override
	public String getMethod ()
	{
		return fRequest.getMethod ();
	}

	@Override
	public String getPathInContext ()
	{
		final String ctxPath = fRequest.getContextPath ();
		final int ctxPathLen = ctxPath.length();
		return fRequest.getRequestURI().substring ( ctxPathLen );
	}

	@Override
	public String getFirstHeader ( String h )
	{
		List<String> l = getHeader ( h );
		return ( l.size () > 0 ) ? l.iterator ().next () : null;
	}

	@Override
	public List<String> getHeader ( String h )
	{
		final LinkedList<String> list = new LinkedList<String> ();
		final Enumeration<?> e = fRequest.getHeaders ( h );
		while ( e.hasMoreElements () )
		{
			list.add ( e.nextElement ().toString () );
		}
		return list;
	}

	@Override
	public String getContentType ()
	{
		return fRequest.getContentType ();
	}

	@Override
	public int getContentLength ()
	{
		return fRequest.getContentLength ();
	}

	@Override
	public InputStream getBodyStream ()
		throws IOException
	{
		return fRequest.getInputStream ();
	}

	@Override
	public BufferedReader getBodyStreamAsText ()
		throws IOException
	{
		return new BufferedReader ( new InputStreamReader ( getBodyStream () ) );
	}

	@Override
	public Map<String, String[]> getParameterMap ()
	{
		final HashMap<String,String[]> map = new HashMap<String,String[]>();
		final Map<String,String[]> m = fRequest.getParameterMap ();
		map.putAll ( m );
		map.putAll ( fParamOverrides );
		return map;
	}

	@Override
	public String getParameter ( String key )
	{
		if ( fParamOverrides.containsKey ( key ) )
		{
			final String[] o = fParamOverrides.get ( key );
			return o.length > 0 ? o[0] : "";
		}
		else
		{
			return fRequest.getParameter ( key );
		}
	}

	@Override
	public String getParameter ( String key, String defVal )
	{
		String p = getParameter ( key );
		if ( p == null )
		{
			p = defVal;
		}
		return p;
	}

	@Override
	public int getIntParameter ( String key, int defVal )
	{
		int result = defVal;
		final String p = getParameter ( key );
		if ( p != null )
		{
			try
			{
				result = Integer.parseInt ( p );
			}
			catch ( Exception x )
			{
				result = defVal;
			}
		}
		return result;
	}

	@Override
	public void changeParameter ( String fieldName, String value )
	{
		fParamOverrides.put ( fieldName, new String[] { value } );
	}

	@Override
	public String getRemoteAddress ()
	{
		final String reqAddr = fRequest.getRemoteAddr ();
		final String fwdHeader = getFirstHeader ( "X-Forwarded-For" );
		return fwdHeader != null ? fwdHeader : reqAddr;
	}

	private final HttpServletRequest fRequest;
	private final HashMap<String,String[]> fParamOverrides;
}
