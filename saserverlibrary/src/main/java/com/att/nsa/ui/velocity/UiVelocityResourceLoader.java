/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui.velocity;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.io.FilenameUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class UiVelocityResourceLoader extends ResourceLoader
{
	// the magic string should not include a slash so that velocity's "relative
	// path" handler won't see the resulting resource name as having a directory
	private static final String kMagicSeparatorString = ":";

	public static String getAppPrefix ( String name )
	{
		final int magic = name.indexOf ( kMagicSeparatorString );
		if ( magic >= 0 )
		{
			final String appId = name.substring ( 0, magic );
			return appId;
		}
		return null;
	}

	public static String getBaseName ( String name )
	{
		final String prefix = getAppPrefix ( name );
		if ( prefix != null )
		{
			return name.substring ( prefix.length() + 1 );
		}
		else
		{
			return name;
		}
	}

	public static String encodeTemplateName ( String appId, String baseName )
	{
		if ( getAppPrefix ( baseName ) != null ) return baseName;
		return ( appId == null ? "" : appId + kMagicSeparatorString ) + baseName;
	}

	public static List<String> decodeTemplateName ( String name )
	{
		final LinkedList<String> list = new LinkedList<String> ();
		final int magic = name.indexOf ( kMagicSeparatorString );
		if ( magic >= 0 )
		{
			final String appId = name.substring ( 0, magic );
			final String template = name.substring ( magic+kMagicSeparatorString.length () );
			list.add ( "templates/" + appId + "/" + template );
			list.add ( "templates/" + template );
		}
		else
		{
			list.add ( "templates/" + name );
		}
		return list;
	}

	public UiVelocityResourceLoader ()
	{
		fNormalLoader = new ClasspathResourceLoader ();
	}

	@Override
	public void commonInit ( RuntimeServices rs, ExtendedProperties configuration)
	{
		super.commonInit ( rs, configuration );
		fNormalLoader.commonInit ( rs, configuration );
	}

	@Override
	public void init ( ExtendedProperties configuration )
	{
		fNormalLoader.init ( configuration );
	}

	@Override
	public InputStream getResourceStream ( String source ) throws ResourceNotFoundException
	{
		InputStream result = null;

		final String origSource = source;

		// build a normalized version of the same thing
		String base = getBaseName ( source );
		if ( !base.equals ( source ) )
		{
			// then it had an app prefix
			final String normal = FilenameUtils.normalize ( base );
			source = encodeTemplateName ( getAppPrefix ( source ), normal );
		}

		log.info ( "velocity requests [" + origSource + "], normalized as [" + source + "]..." );

		final List<String> options = decodeTemplateName ( source );
		for ( String option : options )
		{
			log.info ( "\ttrying " + option );
			if ( fNormalLoader.resourceExists ( option ) )
			{
				result = fNormalLoader.getResourceStream ( option );
				break;
			}
		}

		if ( result == null )
		{
			log.info ( "\ttrying " + origSource + " (the original)" );
			result = fNormalLoader.getResourceStream ( origSource );
		}

		log.info ( "UiVelocityResourceLoader.getResourceStream ( " + origSource + " ) --> " +
			( result == null ? "<not found>" : "found" )  );

		return result;
	}

	@Override
	public boolean isSourceModified ( Resource resource )
	{
		return fNormalLoader.isSourceModified ( resource );
	}

	@Override
	public long getLastModified ( Resource resource )
	{
		return fNormalLoader.getLastModified ( resource );
	}

	private final ClasspathResourceLoader fNormalLoader;
}
