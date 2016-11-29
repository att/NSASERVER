/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.framework.rendering.DrumlinRenderContext;
import com.att.nsa.drumlin.till.data.rrConvertor;
import com.att.nsa.drumlin.till.data.rrConvertor.conversionError;
import com.att.nsa.drumlin.till.data.stringUtils;

/**
 * Do some analysis on the user agent.
 * @author peter
 */
public class userAgentInfo
{
	public static userAgent populateUserAgentInfo ( DrumlinRequestContext ctx )
	{
		final userAgentInfo uai = analyze ( ctx.request () );
		final userAgent ua = deviceDetector.detect ( uai, ctx.systemSettings () );

		final DrumlinRenderContext rc = ctx.renderer ();
		rc.put ( "drumlinUserAgentInfo", ua );

		return ua;
	}
	
	/**
	 * Pass a request and get user agent info.
	 * @param r
	 * @return a user agent analysis
	 */
	public static userAgentInfo analyze ( DrumlinRequest r )
	{
		final String userAgent = r.getFirstHeader ( "User-Agent" );
		return new userAgentInfo ( userAgent );
	}

	/**
	 * Construct an analysis for a given user agent string.
	 * @param userAgent
	 */
	public userAgentInfo ( String userAgent )
	{
		fMap = new HashMap<String,userAgentFeature> ();
		parse ( userAgent );
	}

	/**
	 * Is a given feature listed in this user agent?
	 * @param name the name of the feature
	 * @return true/false
	 */
	public boolean hasFeature ( String name )
	{
		return fMap.containsKey ( name );
	}

	/**
	 * get the details about a given feature
	 * @param name
	 * @return a user agent feature record
	 */
	public userAgentFeature getFeatureDetails ( String name )
	{
		return fMap.get ( name );
	}

	/**
	 * get the version string for a feature. If it doesn't exist, null is returned.
	 * @param name
	 * @return the version string or null
	 */
	public String getFeatureVersionString ( String name )
	{
		final userAgentFeature uaf = fMap.get ( name );
		if ( uaf != null )
		{
			return uaf.getVersion ();
		}
		return null;
	}

	/**
	 * Get the version value for a feature, if possible. If the version string doesn't exist, -1.0 is returned.
	 * If it exists but can't be parsed as a long, a conversionError is thrown. 
	 * @param name
	 * @return a numeric value for the feature version
	 * @throws conversionError
	 */
	public double getFeatureVersion ( String name ) throws conversionError
	{
		final String v = getFeatureVersionString ( name );
		if ( v == null )
		{
			return -1.0;
		}
		return rrConvertor.convertToDouble ( v );
	}
	
	public double getFeatureVersion ( String name, double errValue )
	{
		final String v = getFeatureVersionString ( name );
		if ( v == null )
		{
			return -1.0;
		}
		return rrConvertor.convertToDouble ( v, errValue );
	}

	/**
	 * Get the comment for a feature. If the feature doesn't exist, null is returned.
	 * @param name
	 * @return the feature comment string
	 */
	public String getFeatureComment ( String name )
	{
		final userAgentFeature uaf = fMap.get ( name );
		if ( uaf != null )
		{
			return uaf.getComment ();
		}
		return null;
	}

	private Map<String,userAgentFeature> fMap;
	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( userAgentInfo.class );

	private void parse ( String ua )
	{
		String currentName = null;
		String currentVersion = null;
		String currentComment = null;

		// deal with no header as if it's an empty header
		if ( ua == null ) ua = "";

		ua = ua.trim ();
		while ( ua.length () > 0 )
		{
			if ( ua.startsWith ( "(" ) )
			{
				if ( currentName != null )
				{
					String commentPart = ""; 
					final int closeParen = ua.indexOf ( ')' ); 
					if ( closeParen >= 0 )
					{
						commentPart = ua.substring ( 0, closeParen ); 
						ua = ua.substring ( closeParen + 1 ).trim ();
					}
					else
					{
						// bad format. use it all and warn
						commentPart = ua.substring ( 1 ); 
						ua = "";
						log.warn ( "Missing a close paren in a user agent comment for " + currentName + "." );
					}
					currentComment = ( currentComment == null ? commentPart : currentComment + commentPart );
				}
				else
				{
					// stray comment field
					log.warn ( "Found a comment section without a preceding feature name. Ignoring." );
				}
			}
			else
			{
				// store the last entry
				if ( currentName != null )
				{
					final userAgentFeature uaf = new userAgentFeature ( currentName, currentVersion, currentComment );
					fMap.put ( currentName, uaf );
					currentName = null;
					currentVersion = null;
					currentComment = null;
				}

				String token = ua;
				final int space = stringUtils.indexOf ( ua, new stringUtils.charSelector()
				{
					@Override
					public boolean select ( Character c )
					{
						return Character.isWhitespace ( c );
					}
				} );
				if ( space >= 0 )
				{
					token = ua.substring ( 0, space );
					ua = ua.substring ( space ).trim ();
				}
				else
				{
					// take it all
					ua = "";
				}

				final int slash = token.indexOf ( '/' );
				if ( slash < 0 )
				{
					currentName = token;
					currentVersion = "";
				}
				else
				{
					currentName = token.substring ( 0, slash );
					currentVersion = token.substring ( slash + 1 );
				}
			}
		}

		// store the last entry
		if ( currentName != null )
		{
			final userAgentFeature uaf = new userAgentFeature ( currentName, currentVersion, currentComment );
			fMap.put ( currentName, uaf );
		}
	}
}
