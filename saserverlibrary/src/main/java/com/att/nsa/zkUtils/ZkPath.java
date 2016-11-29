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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class ZkPath
{
	public static String makeZkPath ( String[] pathParts )
	{
		StringBuffer path = new StringBuffer ();
		for ( String part : pathParts )
		{
			path.append ( "/" );
			path.append ( part );
		}
		return path.toString ();
	}

	public static String[] appendZkPath ( String[] basePathParts, String part )
	{
		final String[] result = new String [ basePathParts.length + 1 ];
		for ( int i=0; i<basePathParts.length; i++ )
		{
			result[i] = basePathParts[i];
		}
		result[basePathParts.length] = part;
		return result;
	}

	public static String[] appendZkPath ( String[] basePathParts, String[] addedParts )
	{
		final String[] result = new String [ basePathParts.length + addedParts.length ];
		for ( int i=0; i<basePathParts.length; i++ )
		{
			result[i] = basePathParts[i];
		}
		for ( int i=0; i<addedParts.length; i++ )
		{
			result[basePathParts.length + i] = addedParts[i];
		}
		return result;
	}

	public static String[] appendZkPath ( String basePathString, String... addedParts )
	{
		return appendZkPath ( getPathParts ( basePathString ), addedParts );
	}

	public static void ensureZkPathExists ( ZooKeeper zk, String[] pathParts ) throws InterruptedException, KeeperException
	{
		String path = "";
		for ( String part : pathParts )
		{
			path += ( "/" + part );
			ensureZkPathExists ( zk, path );
		}
	}

	public static void ensureZkPathExists ( ZooKeeper zk, String path ) throws InterruptedException, KeeperException
	{
		try
		{
			zk.create ( path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
		}
		catch ( KeeperException e )
		{
			if ( e.code ().equals ( Code.NODEEXISTS ) )
			{
				// already there (checking exists() up front doesn't remove
				// the need to catch this, as independent processes could
				// race thru this code.
			}
			else
			{
				throw e;
			}
		}
	}

	public static String[] getPathParts ( String path )
	{
		if ( path == null ) return null;

		if ( path.startsWith ( "/" ) )
		{
			path = path.substring ( 1 );
		}
		return path.split ( "/" );
	}

	public static String getLastPathPart ( String path )
	{
		if ( path == null ) return null;

		final String[] parts = getPathParts ( path );
		return parts[parts.length-1];
	}
}
