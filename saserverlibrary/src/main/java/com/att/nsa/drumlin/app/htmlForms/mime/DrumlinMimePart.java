/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * A MIME part. These are created by the multipart MIME reader via the supplied
 * part factory. 
 * 
 * @author peter@rathravane.com
 *
 */
public interface DrumlinMimePart
{
	/**
	 * Get the content type for this part.
	 * @return a type string
	 */
	String getContentType ();

	/**
	 * Get the content disposition value for this part.
	 * @return a content disposition string
	 */
	String getContentDisposition ();

	/**
	 * Get the name for this part.
	 * @return a name string
	 */
	String getName ();

	/**
	 * Get the value associated with a given content disposition key. If the value doesn't
	 * exist, null is returned. If the value is not provided, an empty string is returned.
	 * @param key
	 * @return a string or null if undefined
	 */
	String getContentDispositionValue ( String key );
	
	/**
	 * open a stream to read this part's data.
	 * @return an input stream
	 * @throws IOException
	 */
	InputStream openStream () throws IOException;

	/**
	 * Get this part's data as a string.
	 * @return the part data as a string
	 */
	String getAsString ();

	/**
	 * Discard this part.
	 */
	void discard ();

	/**
	 * Used by the MIME reader to write bytes to the part.
	 * @param line
	 * @param offset
	 * @param length
	 * @throws IOException
	 */
	void write ( byte[] line, int offset, int length ) throws IOException;

	/**
	 * Used by the MIME reader to close the part during its creation.
	 * @throws IOException
	 */
	void close () throws IOException;
}
