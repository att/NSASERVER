/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.util;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;

public class rrVeloLogBridge implements org.apache.velocity.runtime.log.LogChute
{
	public rrVeloLogBridge ( Logger log )
	{
		this.log = log;
	}
	
	@Override
	public void init ( RuntimeServices rs ) throws Exception
	{
	}

	@Override
	public void log ( int level, String message )
	{
		log ( level, message, null );
	}

	@Override
	public void log ( int level, String message, Throwable t )
	{
		switch ( level )
		{
			case LogChute.DEBUG_ID: { if ( t == null ) log.debug ( message ); else log.debug ( message, t ); } break;
			case LogChute.INFO_ID: { if ( t == null ) log.info ( message ); else log.info ( message, t ); } break;
			case LogChute.WARN_ID: { if ( t == null ) log.warn ( message ); else log.warn ( message, t ); } break;
			case LogChute.ERROR_ID: { if ( t == null ) log.error ( message ); else log.error ( message, t ); } break;
		}
	}

	@Override
	public boolean isLevelEnabled ( int level )
	{
		switch ( level )
		{
			case LogChute.DEBUG_ID: { return log.isDebugEnabled (); }
			case LogChute.INFO_ID: { return log.isInfoEnabled (); }
			case LogChute.WARN_ID: { return log.isWarnEnabled (); }
			case LogChute.ERROR_ID: { return log.isErrorEnabled (); }
		}
		return false;
	}

	private final Logger log;
}
