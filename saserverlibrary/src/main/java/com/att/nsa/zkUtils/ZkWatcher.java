/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.att.nsa.zkUtils;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkWatcher
{
	public static Watcher getLoggingWatcher ( final String title, final Logger log )
	{
		return new Watcher()
		{
			@Override
			public void process ( WatchedEvent event )
			{
				log.info ( "ZK watch for " + title + ": " + event.getType () + " @ " + event.getPath () );
			}
		};
	}
	
	public static void waitForDeletion ( ZkClient zk, final String path, final long timeoutMs ) throws KeeperException, InterruptedException
	{
		log.info ( "Setting up deletion monitor on znode [" + path + "], for " + timeoutMs + " ms." );
		
		final Monitor ready = new Monitor ();

		final long timeoutAtMs = System.currentTimeMillis () + timeoutMs;
		while ( !ready.get () && System.currentTimeMillis () < timeoutAtMs )
		{
			//final Stat s = zk.exists ( path, new LocalWatcher( ready, path ) );
			final boolean nodeExists = zk.exists(path);
			
			if ( !nodeExists )
			{
				// node doesn't exist
				return;
			}
			else
			{
				zk.subscribeDataChanges(path,  new LocalWatcher(ready, path));
				// node is present, wait for deletion notification. if this times out,
				// the InterruptedException is thrown. If it just runs, the implication
				// is that the node was deleted.
				final long remainingTimeMs = Math.max ( 1, timeoutAtMs - System.currentTimeMillis () ); 
				ready.waitFor ( remainingTimeMs );
			}
		}
	}

	private static class LocalWatcher implements IZkDataListener
	{
		public LocalWatcher ( Monitor m, String path )
		{
			fMonitor = m;
			fPath = path;
		}

		private final Monitor fMonitor;
		private final String fPath;
		
		@Override
		public void handleDataChange(String dataPath, Object data)
				throws Exception {
			// Ignore
		}

		@Override
		public void handleDataDeleted(String dataPath) throws Exception {
			if (dataPath.equals(fPath)) {
				fMonitor.set();
			}
		}
	}

	private static class Monitor
	{
		public Monitor ()
		{
			fValue = false;
		}

		public synchronized void waitFor ( long timeoutMs ) throws InterruptedException
		{
			this.wait ( timeoutMs );
		}

		public synchronized void set ()
		{
			fValue = true;
			this.notify ();
		}

		public synchronized boolean get ()
		{
			return fValue;
		}

		private boolean fValue;
	}

	private static final Logger log = LoggerFactory.getLogger ( ZkWatcher.class );
}
