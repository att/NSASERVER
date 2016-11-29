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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.cmdLine.NsaCommandLineUtil;

public class ZkLock
{
	public ZkLock ( ZkClient zk, String lockNode )
	{
		fZk = zk;
		fPath = lockNode;
		fLockNode = null;
	}

	public void lock ( long timeoutMs ) throws KeeperException, TimeoutException, InterruptedException
	{
		fZk.createPersistent ( fPath, true );
		
		final String myLockNode = fPath + "/" + kLockPrefix;
		fLockNode = fZk.createEphemeralSequential ( myLockNode, new byte [0] );

		final long timeoutAtMs = System.currentTimeMillis() + timeoutMs;
		while ( System.currentTimeMillis() < timeoutAtMs )
		{
			final List<String> kids = fZk.getChildren(fPath);
			final LinkedList<String> sortedKids = new LinkedList<String> ();
			for ( String kid : kids )
			{
				sortedKids.add ( fPath + "/" + kid );
			}
			Collections.sort ( sortedKids );

			if ( sortedKids.size() == 0 )
			{
				throw new NoNodeException ( "Node insertion apparently failed." );
			}

			String lowest = sortedKids.iterator ().next ();
			if ( lowest.equals ( fLockNode ) )
			{
				// our node is lowest; good to go
				log.info ( "Secured ZK lock at [" + fPath + "]." );
				return;
			}

			// here, we wait on the next lowest lock from ours. first find it...
			String nextLowest = lowest;
			for ( String curr : sortedKids )
			{
				if ( curr.equals ( fLockNode ) )
				{
					break;
				}
				nextLowest = curr;
			}

			// now watch the next guy for deletion
			final long remainingTimeMs = Math.max ( 1, timeoutAtMs - System.currentTimeMillis () ); 
			log.info ( "Waiting for lock [" + fPath + "]; mine is [" + fLockNode + "], waiting for [" + nextLowest + "], up to " + remainingTimeMs + " ms" );
			ZkWatcher.waitForDeletion ( fZk, nextLowest, remainingTimeMs );
		}

		// lock failed
		release ( false );
		throw new TimeoutException ( "Couldn't obtain lock on " + fPath + " in time." );
	}

	public void unlock () throws TimeoutException, KeeperException
	{
		release ( true );
	}

	protected String getLockNode ()
	{
		return fLockNode;
	}
	
	// test program
	public static void main ( String[] args ) throws IOException, InterruptedException, KeeperException
	{
		final Map<String,String> map = NsaCommandLineUtil.processCmdLine ( args );

		final String zkaddr = NsaCommandLineUtil.getReqdSetting ( map, "-zk", "the ZK connection string with '-zk'" );
		final ZkClient zk = new ZkClient ( zkaddr, 10000 );

		try
		{
			System.out.println ();
			System.out.print ( "# " );

			final HashMap<String,ZkLock> locks = new HashMap<String,ZkLock> ();
			
			String line = "";
			final BufferedReader br = new BufferedReader ( new InputStreamReader ( System.in ) );
			while ( (line = br.readLine ()) != null )
			{
				final String[] parts = line.split ( " " );
				if ( parts.length == 1 && parts[0].equalsIgnoreCase ( "list" ) )
				{
					for ( Entry<String, ZkLock> e : locks.entrySet () )
					{
						System.out.println ( e.getKey() );
					}
				}
				else if ( parts.length == 2 && parts[0].equalsIgnoreCase ( "lock" ) )
				{
					try
					{
						final String path = parts[1];
						if ( locks.containsKey ( path ) )
						{
							System.out.println ( "i've got that lock" );
						}
						else
						{
							final ZkLock lock = new ZkLock ( zk, parts[1] );
							lock.lock ( 30000 );
							locks.put ( path, lock );
							System.out.println ( "locked" );
						}
					}
					catch ( TimeoutException e )
					{
						System.out.println ( "timed out. no lock." );
					}
					catch ( KeeperException e )
					{
						e.printStackTrace();
					}
				}
				else if ( parts.length == 2 && parts[0].equalsIgnoreCase ( "unlock" ) )
				{
					try
					{
						final String path = parts[1];
						if ( locks.containsKey ( path ) )
						{
							locks.remove ( path ).unlock ();
							System.out.println ( "ok" );
						}
						else
						{
							System.out.println ( "not locked by me" );
						}
					}
					catch ( TimeoutException e )
					{
						e.printStackTrace();
					}
					catch ( KeeperException e )
					{
						e.printStackTrace();
					}
				}
				else if ( parts.length == 1 && parts[0].length()==0 )
				{
					// nuthin
				}
				else 
				{
					System.out.println ( "Use 'lock <path>', 'unlock <path>' or 'list'" );
				}

				System.out.println ();
				System.out.print ( "# " );
			}
		}
		finally
		{
			if ( zk != null ) zk.close ();
		}
	}
	
	private final ZkClient fZk;
	private final String fPath;
	private String fLockNode;
	private static final String kLockPrefix = "lock-";
	private static final Logger log = LoggerFactory.getLogger ( ZkLock.class );

	private void release ( boolean locked )
	{
		log.info ( "Releasing " + (locked?"lock":"request") + " at [" + fPath + "]." );
		try
		{
			boolean nodeExisted = fZk.delete ( fLockNode );
			if (!nodeExisted)
			{
				log.warn ( "While releasing " + (locked?"lock":"request") + " at [" + fPath + "], not wasn't found. (Did you manually remove it?)" );
			}
		}
		finally
		{
			fLockNode = null;
		}
		
	}
}
