/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.security.authenticators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.security.NsaApiKey;
import com.att.nsa.security.NsaAuthenticator;
import com.att.nsa.security.db.NsaApiDb;

/**
 * An authenticator for AT&T MechIds.
 * 
 * @author peter
 *
 * @param <K>
 */
public class MechIdAuthenticator<K extends NsaApiKey> implements NsaAuthenticator<K>
{
	public MechIdAuthenticator ( NsaApiDb<K> db )
	{
//		fDb = db;
	}

	@Override
	public boolean qualify ( DrumlinRequest req )
	{
		// we haven't implemented anything here yet, so there's no qualifying request
		return false;
	}

	@Override
	public K isAuthentic ( DrumlinRequest req )
	{
		final String remoteAddr = req.getRemoteAddress ();
		authLog ( "MechId auth is not yet implemented.", remoteAddr );
		return null;
	}

	private static void authLog ( String msg, String remoteAddr )
	{
		log.info ( "AUTH-LOG(" + remoteAddr + "): " + msg );
	}

//	private final NsaApiDb<K> fDb;
	private static final Logger log = LoggerFactory.getLogger ( MechIdAuthenticator.class );
}
