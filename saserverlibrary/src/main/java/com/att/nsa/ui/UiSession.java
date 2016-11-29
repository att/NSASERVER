/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import com.att.nsa.drumlin.service.framework.DrumlinConnection;
import com.att.nsa.drumlin.service.framework.DrumlinConnectionContext;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

public class UiSession implements DrumlinConnection
{
	public UiSession ()
	{
		fMsgs = new LinkedList<Message> ();
	}

	public enum Level
	{
		INFO,
		WARNING
	}
	
	public static class Message
	{
		public Message ( Level level, String msg )
		{
			fLevel = level;
			fMsg = msg;
		}
		public String getMessage () { return fMsg; }
		public Level getLevel () { return fLevel; }
		private final String fMsg;
		private final Level fLevel;
	}
	
	/**
	 * Convenience method to downcast a session to a UiSession.
	 * Will throw ClassCastExcpetion if the session is not the correct type.
	 * @param ctx
	 * @return
	 */
	public static UiSession getUiSession ( DrumlinRequestContext ctx )
	{
		return (UiSession) ctx.session ();
	}

	public void setStatusMessage ( Message m )
	{
		synchronized ( fMsgs )
		{
			fMsgs.add ( m );
		}
	}

	public List<Message> getMessages ()
	{
		synchronized ( fMsgs )
		{
			final LinkedList<Message> result = new LinkedList<Message> ();
			result.addAll ( fMsgs );
			fMsgs.clear ();
			return result;
		}
	}
	
	@Override
	public void onSessionCreate ( DrumlinServlet ws, DrumlinConnectionContext dcc ) throws ServletException
	{
	}

	@Override
	public void onSessionClose ()
	{
	}

	@Override
	public void noteActivity ()
	{
	}

	@Override
	public void buildTemplateContext ( HashMap<String, Object> context )
	{
		context.put ( "messages", getMessages() );
	}

	public void put ( String key, Object obj )
	{
		fObjects.put ( key, obj );
	}

	public void clear ( String key )
	{
		fObjects.remove ( key );
	}

	public Object get ( String key )
	{
		return fObjects.get ( key );
	}

	private HashMap<String,Object> fObjects = new HashMap<String,Object> ();
	private LinkedList<Message> fMsgs = new LinkedList<Message> ();
}
