/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui.velocity;

import org.apache.velocity.app.event.IncludeEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UiVelocityEventHandler implements IncludeEventHandler
{
    /**
     * Called when an include-type directive is encountered (
     * <code>#include</code> or <code>#parse</code>). May modify the path
     * of the resource to be included or may block the include entirely. All the
     * registered IncludeEventHandlers are called unless null is returned. If
     * none are registered the template at the includeResourcePath is retrieved.
     *
     * @param includeResourcePath  the path as given in the include directive.
     * @param currentResourcePath the path of the currently rendering template that includes the
     *            include directive.
     * @param directiveName  name of the directive used to include the resource. (With the
     *            standard directives this is either "parse" or "include").
     *
     * @return a new resource path for the directive, or null to block the
     *         include from occurring.
     */
	public String includeEvent ( String includeResourcePath, String currentResourcePath, String directiveName )
	{
		// in this scheme, the system always sees the included path as relative, because 
		// when current is prefixed with the app tag, it's not thought to be an absolute path.
		// So velocity will go to the last slash of current, then append include. If current doesn't 
		// contain a slash, it'll use includeResourcePath

		String result = includeResourcePath;
		final String appId = UiVelocityResourceLoader.getAppPrefix ( currentResourcePath );
		if ( appId != null && !currentResourcePath.contains ( "/" ) )
		{
			result = UiVelocityResourceLoader.encodeTemplateName ( appId, includeResourcePath );
		}

		log.info ( "UiVelocityEventHandler.includeEvent ( " + includeResourcePath + ", " +
			currentResourcePath + ", " + directiveName + " ) --> " + result );
		return result;
	}

	private static final Logger log = LoggerFactory.getLogger ( UiVelocityEventHandler.class );
}
