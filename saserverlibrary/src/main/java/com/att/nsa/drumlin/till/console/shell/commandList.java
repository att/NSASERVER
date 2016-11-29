/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.console.shell;

public interface commandList
{
	/**
	 * return the command for a text command, or null
	 * @param cmd
	 * @return information about the command, or null
	 */
	command getCommandFor ( String cmd );
}
