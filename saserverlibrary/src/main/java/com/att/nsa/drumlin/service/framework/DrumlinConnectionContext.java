/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework;

import java.util.concurrent.TimeUnit;

/**
 * Context provided to a DrumlinConnection when the servlet associates a client call.
 * 
 * @author peter@rathravane.com
 */
public interface DrumlinConnectionContext
{
	/**
	 * If the connection should timeout after inactivity, call setInactiveExpiration on
	 * the connection after it's setup.
	 * @param units
	 * @param tu
	 */
	void setInactiveExpiration ( long units, TimeUnit tu );
}
