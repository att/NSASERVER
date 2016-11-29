/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework.rendering;

/**
 * An interface to the Velocity renderer. Get/put/remove objects
 * for use in VTL; Render templates to the response stream (provided
 * by the handlingContext where the renderContext was acquired).
 * 
 * @author peter
 */
public interface DrumlinRenderContext
{
	/**
	 * Get an object in the context by name.
	 * @param key
	 * @return an object, or null.
	 */
	Object get ( String key );

	/**
	 * Put an object into the render context with a name. The name is available
	 * in Velocity VTL.
	 * 
	 * @param key
	 * @param o
	 */
	void put ( String key, Object o );

	/**
	 * Remove an object given its name.
	 * @param key
	 */
	void remove ( String key );

	/**
	 * Render the named template.
	 * @param templateName
	 */
	void renderTemplate ( String templateName );

	/**
	 * Render the named template with the given content type in the HTTP header.
	 * @param templateName
	 * @param contentType
	 */
	void renderTemplate ( String templateName, String contentType );
}
