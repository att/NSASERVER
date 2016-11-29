/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms.mime;

import java.io.IOException;

import com.att.nsa.drumlin.till.collections.rrMultiMap;

/**
 * A MIME part factory. The factory is provided to the multipart MIME reader to
 * allow an application to create parts. For example, a web app receiving a file
 * input may want to store that file on AWS S3 rather than in a local tmp file.
 * 
 * @author peter@rathravane.com
 *
 */
public interface DrumlinMimePartFactory
{
	/**
	 * Create a MIME part given header values for the part section.
	 * @param partHeaders
	 * @return a new MIME part
	 * @throws IOException
	 */
	DrumlinMimePart createPart ( rrMultiMap<String,String> partHeaders ) throws IOException;
}
