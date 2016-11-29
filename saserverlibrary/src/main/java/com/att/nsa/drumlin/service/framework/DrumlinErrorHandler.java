/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * You can register an error handler with the request router.
 */
public interface DrumlinErrorHandler
{
	/**
	 * Handle the error. Do not throw out of this method!
	 * @param ctx
	 * @param cause
	 */
	void handle ( DrumlinRequestContext ctx, Throwable cause );
}
