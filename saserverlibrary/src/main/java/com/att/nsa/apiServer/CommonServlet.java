/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;
import java.util.UUID;

import javax.servlet.ServletException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.apiServer.util.Emailer;
import com.att.nsa.configs.ConfigDb;
import com.att.nsa.configs.ConfigDbException;
import com.att.nsa.configs.confimpl.CassandraConfigDb;
import com.att.nsa.configs.confimpl.EncryptingLayer;
import com.att.nsa.configs.confimpl.FileSystemConfigDb;
import com.att.nsa.configs.confimpl.MemConfigDb;
import com.att.nsa.configs.confimpl.ZkConfigDb;
import com.att.nsa.drumlin.service.framework.DrumlinErrorHandler;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.service.framework.routing.playish.DrumlinPlayishRoutingFileSource;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import com.att.nsa.drumlin.service.standards.MimeTypes;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.security.NsaAuthenticatorService;
import com.att.nsa.security.ReadWriteSecuredResource.AccessDeniedException;
import com.att.nsa.security.authenticators.OriginalUebAuthenticator;
import com.att.nsa.security.authenticators.RemoteSaIamAuthenticator;
import com.att.nsa.security.authenticators.SimpleAuthenticator;
import com.att.nsa.security.db.BaseNsaApiDbImpl;
import com.att.nsa.security.db.EncryptingApiDbImpl;
import com.att.nsa.security.db.NsaApiDb;
import com.att.nsa.security.db.NsaApiDb.KeyExistsException;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.att.nsa.security.db.simple.NsaSimpleApiKeyFactory;
import com.att.nsa.security.db.simple.NsaSimpleRemoteApiKey;
import com.att.nsa.util.rrConvertor;

public abstract class CommonServlet extends DrumlinServlet
{
	// authentication system settings and defaults
	public static final String kSetting_RequireSecureChannel = "authentication.requireSecureChannel";
	public static final boolean kDefault_RequireSecureChannel = true;
	public static final String kSetting_RequestTimeWindow = "authentication.allowedTimeSkewMs";
	public static final long kDefault_RequestTimeWindow = 1000 * 60 * 10; // 10 minutes

	// admin API calls are protected via special API key "admin" and an API secret loaded from config
	public static final String kSetting_AdminSecret = "authentication.adminSecret";

	public static final String kSetting_UseLocalConfigDbAuth = "iam.local";
	public static final boolean kDefault_UseLocalConfigDbAuth = true;

	public static final String kSetting_UseRemoteAuth = "iam.remote";
	public static final boolean kDefault_UseRemoteAuth = false;

	// zookeeper related settings and defaults
	public static final String kSetting_ZkConfigDbServers = "config.zk.servers";
	public static final String kSetting_ZkConfigDbRoot = "config.zk.root";
	public static final String kDefault_ZkConfigDbServers = "localhost:2181";

	// file system related settings and defaults
	public static final String kSetting_FsConfigDbDir = "config.fs.location";
	public static final String kDefault_FsConfigDbDir = "./etc/configdb";

	// cassandra related settings and defaults
	public static final String kSetting_CassandraContactPoint = "flatiron.cassandra.contactpoint";
	public static final String kSetting_CassandraPort = "flatiron.cassandra.port";
	public static final String kDefault_CassandraContactPoint = "localhost";
	public static final int kDefault_CassandraPort = 9042;

	public enum ConfigDbType
	{
		MEMORY,
		FILESYSTEM,
		ZOOKEEPER,
		CASSANDRA
	}

	public CommonServlet ( rrNvReadable settings, String sysName, boolean withApiKeyEndpoints ) throws loadException, missingReqdSetting
	{
		super ( settings, null, sessionLifeCycle.NO_SESSION );

		fSysName = sysName;
		fInited = false;
		fWithApiKeyEndpoints = withApiKeyEndpoints;
		fAdminUserKey = null;
	}

	public ConfigDb getConfigDb ()
	{
		checkInit ();
		return fConfigDb;
	}

	public NsaAuthenticatorService<NsaSimpleApiKey> getSecurityManager ()
	{
		checkInit ();
		return fSecurityManager;
	}

	public NsaApiDb<NsaSimpleApiKey> getApiKeyDb ()
	{
		checkInit ();
		return fApiKeyDb;
	}

	public Emailer getSystemEmailer ()
	{
		checkInit ();
		return fEmailer;
	}

	/**
	 * check if this system has an admin user
	 * @return true if this system has an admin user
	 */
	public boolean hasAdminUser ()
	{
		return fAdminUserKey != null;
	}

	/**
	 * Check if the given user api key is the admin user.
	 * @param apiKey
	 * @return true if the api key is that of the admin user.
	 */
	public boolean isAdminUser ( String apiKey )
	{
		return fAdminUserKey != null && fAdminUserKey.equals ( apiKey );
	}

	/**
	 * Get the default root node string for the ZK-backed config db
	 * @param sysName
	 * @return
	 */
	public static String getDefaultZkRoot ( String sysName )
	{
		return "/fe3c/" + sysName + "/config";
	}
	
	/**
	 * Call this early in your servletSetup() to initialize services provided by this class.
	 * 
	 * @param cdbType
	 * @throws rrNvReadable.missingReqdSetting
	 * @throws rrNvReadable.invalidSettingValue
	 * @throws ServletException
	 * @throws ConfigDbException
	 * @throws IOException 
	 */
	protected synchronized void commonServletSetup ( ConfigDbType cdbType ) throws ConfigDbException, missingReqdSetting, IOException 
	{
		if ( fInited )
		{
			log.warn ( "CommonServlet is already initialized." );
		}
		fInited = true;
		
		// setup some constant log info (for ECOMP)
		setupEcompLogging ();

		// setup standard error handling replies
		setupStandardErrorHandlers ();

		// build a configdb
		final rrNvReadable settings = getSettings ();
		if ( cdbType == null || cdbType == ConfigDbType.MEMORY )
		{
			fConfigDb = new MemConfigDb ();
		}
		else if ( cdbType == ConfigDbType.FILESYSTEM )
		{
			final File baseDir = new File ( settings.getString ( kSetting_FsConfigDbDir, kDefault_FsConfigDbDir ) );
			fConfigDb = new FileSystemConfigDb ( baseDir );
		}
		else if ( cdbType == ConfigDbType.ZOOKEEPER )
		{
			final String defRoot = getDefaultZkRoot ( fSysName );
			fConfigDb = new ZkConfigDb (
				settings.getString ( kSetting_ZkConfigDbServers, kDefault_ZkConfigDbServers ),
				settings.getString ( kSetting_ZkConfigDbRoot, defRoot )
			);
		}
		else if ( cdbType == ConfigDbType.CASSANDRA )
		{
			final int cassandraPort = getSettings ().getInt ( kSetting_CassandraPort, kDefault_CassandraPort );
			final String contactPoints = getSettings ().getString ( kSetting_CassandraContactPoint, kDefault_CassandraContactPoint );
			fConfigDb = new CassandraConfigDb ( Arrays.asList ( contactPoints.split ( "," ) ), cassandraPort );
		}
		else
		{
			throw new IllegalArgumentException ( "Unrecognized configDb type: " + cdbType );
		}

		// setup authentication system
		fApiKeyDb = buildApiKeyDb ( settings, fConfigDb );
		fSecurityManager = new NsaAuthenticatorService<NsaSimpleApiKey> (
			settings.getBoolean ( kSetting_RequireSecureChannel, kDefault_RequireSecureChannel )
		);

		// add the usual api key authenticator
		if ( settings.getBoolean ( kSetting_UseLocalConfigDbAuth, kDefault_UseLocalConfigDbAuth ) )
		{
			fSecurityManager.addAuthenticator ( new OriginalUebAuthenticator<NsaSimpleApiKey> (
				fApiKeyDb,
				settings.getLong ( kSetting_RequestTimeWindow, kDefault_RequestTimeWindow ) )
			);
		}

		// add the remote api key authenticator
		if ( settings.getBoolean ( kSetting_UseRemoteAuth, kDefault_UseRemoteAuth ) )
		{
			try
			{
				fSecurityManager.addAuthenticator ( new RemoteSaIamAuthenticator<NsaSimpleApiKey> ( settings, new RemoteSaIamAuthenticator.ApiKeyFactory<NsaSimpleApiKey>()
				{
					@Override
					public NsaSimpleApiKey createApiKey ( JSONObject data )
					{
						return new NsaSimpleRemoteApiKey ( data );
					}
				} ) );
			}
			catch ( GeneralSecurityException e )
			{
				log.warn ( "Couldn't create a remote authenticator client: " + e.getMessage(), e );
			}
		}

		// add the admin authenticator
		final String adminSecret = settings.getString ( kSetting_AdminSecret, null );
		if ( adminSecret != null && adminSecret.length () > 0 )
		{
			fAdminUserKey = "admin";
			try
			{
				// add via API key auth
				final NsaApiDb<NsaSimpleApiKey> adminDb = new BaseNsaApiDbImpl<NsaSimpleApiKey> ( new MemConfigDb(), new NsaSimpleApiKeyFactory() );
				adminDb.createApiKey ( fAdminUserKey, adminSecret );
				fSecurityManager.addAuthenticator ( new OriginalUebAuthenticator<NsaSimpleApiKey> ( adminDb, 10*60*1000 ) );

				// also add admin via Basic Auth
				fSecurityManager.addAuthenticator ( new SimpleAuthenticator ().add ( "admin", adminSecret ) );
			}
			catch ( KeyExistsException e )
			{
				throw new RuntimeException ( "This key can't exist in a fresh in-memory DB!", e );
			}
		}

		// setup the emailer
		fEmailer = new Emailer ( settings );

		// common endpoints
		if ( fWithApiKeyEndpoints )
		{
			final URL routes = findStream ( "iamRoutes.conf" );
			if ( routes == null )
			{
				log.warn ( "System requested IAM routes, but iamRoutes.conf was not found." );
			}
			else
			{
				final DrumlinPlayishRoutingFileSource drs = new DrumlinPlayishRoutingFileSource ( routes );
				getRequestRouter ().addRouteSource ( drs );
			}
		}
	}

	private NsaApiDb<NsaSimpleApiKey> buildApiKeyDb ( rrNvReadable settings, ConfigDb cdb ) throws ConfigDbException, missingReqdSetting
	{
		// this system uses an encrypted api key db

		final String keyBase64 = settings.getString ( fSysName + ".secureConfig.key", null );
		final String initVectorBase64 = settings.getString ( fSysName + ".secureConfig.iv", null );

		// if neither value was provided, don't encrypt api key db
		if ( keyBase64 == null && initVectorBase64 == null )
		{
			log.warn ( "This server is configured to use an unencrypted API key database. See the settings documentation." );
			return new BaseNsaApiDbImpl<NsaSimpleApiKey> ( cdb, new NsaSimpleApiKeyFactory () );
		}
		else if ( keyBase64 == null )
		{
			// neither or both, otherwise something's goofed
			throw new missingReqdSetting ( fSysName + ".secureConfig.key" );
		}
		else if ( initVectorBase64 == null )
		{
			// neither or both, otherwise something's goofed
			throw new missingReqdSetting ( fSysName + ".secureConfig.iv" );
		}
		else
		{
			log.info ( "This server is configured to use an encrypted API key database." );
			final Key key = EncryptingLayer.readSecretKey ( keyBase64 );
			final byte[] iv = rrConvertor.base64Decode ( initVectorBase64 );
			return new EncryptingApiDbImpl<NsaSimpleApiKey> ( cdb, new NsaSimpleApiKeyFactory (), key, iv );
		}
	}

	private final String fSysName;
	private final boolean fWithApiKeyEndpoints;
	private boolean fInited;
	private ConfigDb fConfigDb;
	private NsaApiDb<NsaSimpleApiKey> fApiKeyDb;
	private NsaAuthenticatorService<NsaSimpleApiKey> fSecurityManager;
	private Emailer fEmailer;
	private String fAdminUserKey;

	private static final long serialVersionUID = 1L;

	/*
	 * Our servers are meant as the front-end to stand-alone services, but because they're used
	 * by various ECOMP clients, and may be bundled in distribution with them someday, the GFP
	 * SE group things that our servers must comply with general ECOMP requirements. This
	 * code sets up a number of static fields that are required by ECOMP's (very amateur) logging
	 * standard. The fields populated here are available via Log4J's EnhancedPatternLayout
	 * system, using "%X{key}" to get the value of "key".
	 * 
	 * As of 7 July 2015, the logging standard is available on Rally:
	 * 	https://rally1.rallydev.com/slm/attachment/38451080500/ECOMP%20platform%20application%20logging%20guidelines.docx
	 * 
	 * A log format line that meets current reqs looks like:
	 * 
	 *		%d{yyyy-MM-dd'T'HH:mm:ss}{GMT+0}+00:00|%X{requestId}|%X{serviceInstanceId}|%-10t|%X{serverName}|%X{serviceName}|%X{instanceUuid}|%-5p|%X{severity}|%X{serverIpAddress}|%X{server}|%X{ipAddress}|%X{className}|%X{timer}|%m%n
	 * 
	 * Each field referenced inside a %X{} should be available to fully meet the reqs.
	 */
	private void setupEcompLogging ()
	{
		final LoggingContext lc = getCommonLoggingContext ();

		String ipAddr = "127.0.0.1";
		String hostname = "localhost";
		try
		{
			final InetAddress ip = InetAddress.getLocalHost ();
			hostname = ip.getCanonicalHostName ();
			ipAddr = ip.getHostAddress();
		}
		catch ( UnknownHostException x )
		{
			// just use localhost
		}

		lc.put ( "serverName", hostname );
		lc.put ( "serviceName", fSysName );
		lc.put ( "server", hostname );
		lc.put ( "serverIpAddress", ipAddr.toString () );

		// instance UUID is meaningless here, so we just create a new one each time the
		// server starts. One could argue each new instantiation of the service should
		// have a new instance ID.
		lc.put ( "instanceUuid", UUID.randomUUID ().toString () );

		// *really* meaningless data
		lc.put ( "severity", "" );
	}

	private void setupStandardErrorHandlers ()
	{
		final DrumlinRequestRouter drr = getRequestRouter ();

		drr.setHandlerForException ( AccessDeniedException.class, new DrumlinErrorHandler()
		{
			@Override
			public void handle ( DrumlinRequestContext ctx, Throwable cause )
			{
				sendJsonReply ( ctx,
					HttpStatusCodes.k401_unauthorized,
					"Access denied. Check your API key and signature, or check with the scope administrator."
				);
			}
		});

		drr.setHandlerForException ( ConfigDbException.class, new DrumlinErrorHandler()
		{
			@Override
			public void handle ( DrumlinRequestContext ctx, Throwable cause )
			{
				sendJsonReply ( ctx,
					HttpStatusCodes.k503_serviceUnavailable,
					"There was a problem using the cluster's configuration database. Contact the cluster administrators or try again later." );
			}
		});

		drr.setHandlerForException ( JSONException.class, new DrumlinErrorHandler()
		{
			@Override
			public void handle ( DrumlinRequestContext ctx, Throwable cause )
			{
				sendJsonReply ( ctx,
					HttpStatusCodes.k400_badRequest,
					"There's a problem with your JSON. " + cause.getMessage ()
				);
			}
		});
	}

	protected void sendJsonReply ( DrumlinRequestContext ctx, int statusCode, String errMsg )
	{
		ctx.response ().sendErrorAndBody (
			statusCode,
			new JSONObject()
				.put ( "statusCode", statusCode )
				.put ( "error", errMsg )
				.toString(),
			MimeTypes.kAppJson );
	}

	private void checkInit ()
	{
		if ( !fInited ) throw new IllegalStateException ( "CommonServlet was not initialzied." );
	}

	private static final Logger log = LoggerFactory.getLogger ( CommonServlet.class );
}
