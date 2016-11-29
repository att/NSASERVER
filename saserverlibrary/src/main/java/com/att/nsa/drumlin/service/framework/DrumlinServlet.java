/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.service.framework;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.LoggerFactory;

import com.att.nsa.clock.SaClock;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.framework.rendering.vtlTools.DrumlinVtlHelper;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter.noMatchingRoute;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRouteInvocation;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.impl.nvInstallTypeWrapper;
import com.att.nsa.drumlin.till.nv.impl.nvPropertiesFile;
import com.att.nsa.drumlin.till.nv.impl.nvReadableStack;
import com.att.nsa.drumlin.util.rrVeloLogBridge;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.LoggingContextFactory;
import com.att.nsa.logging.log4j.EcompFields;

/**
 * The base servlet associates a connection object with an HTTP connection. Even
 * session-less servers like a RESTful API have connections -- they're just not
 * stored across calls.
 * 
 * @author peter
 */
public class DrumlinServlet extends HttpServlet
{
	public static final String kJvmSetting_FileRoot = "RRWT_FILES";
	public static final String kSetting_BaseWebAppDir = "drumlin.webapp.base"; 
	
	// in your HTML templates, use "$warRoot" to mean the base of your war file/directory
	public static final String kWarRoot = "warRoot";

	// in your HTML templates, use "$servletRoot" to mean the path that this servlet is mapped onto
	// (your mapping is specified in web.xml, so you could just hardcode it and not use servletRoot
	// and instead use $warRoot/<mapping>/)
	public static final String kServletRoot = "servletRoot";

	// a helper tool for velocity
	public static final String kDrumlinVtlToolName = "drumlinVtl";

	/**
	 * Session life cycle is determined at servlet creation time.
	 * @author peter@rathravane.com
	 */
	public enum sessionLifeCycle
	{
		/**
		 * No session data is stored on the server for the client.
		 */
		NO_SESSION,

		/**
		 * The server stores a "flash session" for the client, allowing your code to
		 * retrieve data that was stored while processing the last request from each client only.
		 */
		FLASH_SESSION,

		/**
		 * The server stores a full session for the client. The session eventually
		 * expires if not explicitly removed.
		 */
		FULL_SESSION
	};

	/**
	 * Construct a servlet with default settings and the "no session" session life cycle.
	 */
	public DrumlinServlet ()
	{
		this ( sessionLifeCycle.NO_SESSION );
	}

	/**
	 * Construct a servlet with default settings, and the specified session life cycle.
	 */
	public DrumlinServlet ( String prefsFileName )
	{
		this ( prefsFileName, sessionLifeCycle.NO_SESSION );
	}

	/**
	 * Construct a servlet with default settings, and the specified session life cycle.
	 */
	public DrumlinServlet ( sessionLifeCycle slc )
	{
		this ( null, slc );
	}

	/**
	 * Construct a servlet with settings from a named file, and the specified
	 * session life cycle.
	 * 
	 * @param prefsFileName
	 * @param slc
	 */
	public DrumlinServlet ( String prefsFileName, sessionLifeCycle slc )
	{
		this ( null, prefsFileName, slc );
	}

	/**
	 * Construct a servlet with settings from a named file, and the specified
	 * session life cycle.
	 * 
	 * @param prefsFileName
	 * @param slc
	 */
	public DrumlinServlet ( rrNvReadable settings, String addlSettingsFileName, sessionLifeCycle slc )
	{
		fWebInfDir = null;
		fProvidedPrefs = settings;
		fPrefsConfigFilename = addlSettingsFileName;
		fSessionLifeCycle = slc;
		fRouter = null;
		fObjects = new HashMap<String,Object> ();
		fSearchDirs = new LinkedList<File> ();
		fRuntimeControls = new DrumlinRuntimeControls ();
		fBasePath = "./";

		// setup a logging context
		fLogContext = new LoggingContextFactory.Builder().build();
	}

	/**
	 * Initialize the servlet (called by the servlet container).
	 */
	@Override
	public final void init ( ServletConfig sc ) throws ServletException
	{
		super.init ( sc );

		// find the WEB-INF dir
		fBasePath = sc.getServletContext ().getRealPath ( "/" );
		if ( fBasePath == null )
		{
			log.info ( "Servlet engine did not map '/' into a real path. Using './'" );
			fBasePath = "./";
		}
	
		log.info ( "working dir = " + System.getProperty("user.dir") );
		log.info ( "servlet class: " + this.getClass ().getName() );
		log.info ( "real path of '/' = " + fBasePath );

		// make the settings
		final nvReadableStack settingsStack = new nvReadableStack ();
//		settingsStack.push ( new nvJvmProperties () );
		settingsStack.push ( new DrumlinServletSettings ( sc ) );
		if ( fProvidedPrefs != null )
		{
			settingsStack.push ( fProvidedPrefs );
		}

		// find the base webapp directory, normally "WEB-INF"
		final String webappDirName = settingsStack.getString ( kSetting_BaseWebAppDir, new File ( fBasePath, "WEB-INF" ).getAbsolutePath () );
		final File checkDir = new File ( webappDirName );
		if ( checkDir.exists () )
		{
			fWebInfDir = checkDir;
		}
		else
		{
			log.debug ( "Drumlin can't find the webapp's base directory. Used '" + webappDirName + "'." );
		}

		// get additional search dirs
		final String searchDirString = settingsStack.getString ( "drumlin.config.search.dirs", null );
		if ( searchDirString != null )
		{
			log.info ( "config search dirs: " + searchDirString );
			final String searchDirs[] = searchDirString.split ( ":" );
			for ( String searchDir : searchDirs )
			{
				addToFileSearchDirs ( new File ( searchDir ) );
			}
		}
		else
		{
			log.info ( "drumlin.config.search.dirs is not set. (Typically set in web.xml)" );
		}

		if ( fPrefsConfigFilename != null && fPrefsConfigFilename.length() > 0 )
		{
			try
			{
				log.info ( "finding config stream named [" + fPrefsConfigFilename + "]." );
				final URL configFile = findStream ( fPrefsConfigFilename );
				if ( configFile != null )
				{
					log.info ( "chose stream [" + configFile.toString () + "]." );
					final rrNvReadable filePrefs = new nvPropertiesFile ( configFile );
					settingsStack.push ( filePrefs );
				}
				else
				{
					log.warn ( "could not find config stream." );
				}
			}
			catch ( rrNvReadable.loadException e )
			{
				log.warn (  "Couldn't load settings from [" + fPrefsConfigFilename + "]." );
			}
		}
		else
		{
			log.info ( "no preferences file specified to " + getClass().getSimpleName() + "'s constructor." );
		}

		// add the runtime control settings to the settings stack
		settingsStack.push ( fRuntimeControls );
		
		// put a wrapper on the top-level settings object to allow for
		// installation-type specific settings.
		final rrNvReadable appLevelSettings = makeSettings ( settingsStack );
		fSettings = new nvInstallTypeWrapper ( appLevelSettings );

		// routing setup
		fRouter = new DrumlinRequestRouter ();
		
		// velocity setup
		try
		{
			fVelocity = new VelocityEngine ();
			fVelocity.setProperty ( VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new rrVeloLogBridge ( log ) );

			setupResourceLoader ( fVelocity, fSettings );

			fVelocity.init ();

			// create a base context and add the servletRoot value
			fBaseContext = new VelocityContext ();

			// we want relative template finding
			{
				final EventCartridge ec = new EventCartridge ();
				ec.addEventHandler ( new IncludeRelativePath () );
				ec.attachToContext ( fBaseContext );
			}
			
			// contextPath is the base directory for this servlet. if the servlet is at the
			// root location, the context path is "", per spec.
			final String contextPath = sc.getServletContext ().getContextPath ();
			log.info ( "context path ($" + kWarRoot + "): [" + contextPath + "]." );
			fBaseContext.put ( kWarRoot, contextPath );
			fBaseContext.put ( kServletRoot, contextPath );

			// some simple tools for velocity
			fBaseContext.put ( kDrumlinVtlToolName, new DrumlinVtlHelper () );
		}
		catch ( Exception e )
		{
			throw new ServletException ( e );
		}

		// app-level setup
		try
		{
			log.info ( "Calling app servlet setup." );
			servletSetup ();
		}
		catch ( rrNvReadable.missingReqdSetting e )
		{
			log.error ( "Shutting down due to missing setting. " + e.getMessage () );
			throw new ServletException ( e );
		}
		catch ( rrNvReadable.invalidSettingValue e )
		{
			log.error ( "Shutting down due to invalid setting. " + e.getMessage () );
			throw new ServletException ( e );
		}

		log.info ( "Servlet is ready." );
	}

	@Override
	public final void destroy ()
	{
		super.destroy ();
		try
		{
			servletShutdown ();
		}
		catch ( Exception x )
		{
			log.error ( "During tear-down: " + x.getMessage () );
		}
	}

	/**
	 * Find the named resource and return an InputStream for it. This is related to findFile(), but
	 * is built to be more general. Use this if you don't actually require a file on disk.<br/>
	 * <br/>
	 * 1. If the JVM system properties include a setting with the key specified by kJvmSetting_FileRoot, look
	 * for the file relative to that path.<br/>
	 * 2. Try the system's findResource() call.
	 * 3. Try findFile()
	 * <br/>
	 * @param resourceName
	 * @return an InputStream, or null
	 */
	public static URL findStream ( String resourceName, Class<?> clazz )
	{
		try
		{
			// first try it as an absolute file name
			File file = new File ( resourceName );
			if ( file.isAbsolute () )
			{
				return file.toURI().toURL();
			}

			// next try the file root setting, which takes precedence
			final String filesRoot = System.getProperty ( kJvmSetting_FileRoot, null );
			if ( filesRoot != null )
			{
				final String fullPath = filesRoot + "/" + resourceName;
				log.debug ( "Looking for [" + fullPath + "]." );
				file = new File ( fullPath );
				if ( file.exists () )
				{
					return file.toURI().toURL();
				}
			}

			// next try the class's resource finder
			URL res = clazz.getClassLoader().getResource ( resourceName );
			if ( res != null )
			{
				return res;
			}

			// now try the system class loaders' resource finder
			res = ClassLoader.getSystemResource ( resourceName );
			if ( res != null )
			{
				return res;
			}
		}
		catch ( MalformedURLException e )
		{
			log.warn ( "Unexpected failure to convert a local filename into a URL: " + e.getMessage () );
		}

		return null;
	}

	public URL findStream ( String resourceName )
	{
		URL res = findStream ( resourceName, getClass() );
		if ( res != null ) return res;

		try
		{
			// finally, do the regular file search
			final File f = findFile ( resourceName );
			if ( f.exists () )
			{
				final URI u = f.toURI ();
				final URL uu = u.toURL ();
				return uu;
			}
		}
		catch ( MalformedURLException e )
		{
			log.warn ( "Unexpected failure to convert a local filename into a URL: " + e.getMessage () );
		}

		return null;
	}

	/**
	 * Find a file given a file name. If the name is absolute, the file is returned. Otherwise,
	 * the file is located using this search path:<br/>
	 * <br/>
	 * 1. If the JVM system properties include a setting with the key specified by kJvmSetting_FileRoot, look
	 * for the file relative to that path.<br/>
	 * 2. If not yet found, look for the file relative to the servlet's WEB-INF directory, if that exists.<br/>
	 * 3. If not yet found, look for the file relative to the servlet's real path for "/". (Normally inside the war.)<br/>
	 * 4. If not yet found, check each app-provided search directory. <br/>
	 * 4. If not yet found, return a File with the relative path as-is. (This does not mean the file exists!) 
	 *  
	 * @param appRelativePath
	 * @return a File
	 */
	public File findFile ( String appRelativePath )
	{
		File file = new File ( appRelativePath );
		if ( !file.isAbsolute () )
		{
			final String filesRoot = System.getProperty ( kJvmSetting_FileRoot, null );
			if ( filesRoot != null )
			{
				final String fullPath = filesRoot + "/" + appRelativePath;
				log.debug ( "Looking for [" + fullPath + "]." );
				file = new File ( fullPath );
			}

			// check in WEB-INF	(FIXME: using a member variable; think about thread synchronization)
			if ( !file.exists () && fWebInfDir != null )
			{
				file = new File ( fWebInfDir, appRelativePath );
				log.debug ( "Looking for [" + file.getAbsolutePath() + "]." );
			}

			// check in webapp's "/"
			if ( !file.exists () )
			{
				final String fullPath = fBasePath + ( fBasePath.endsWith ( "/" ) ? "" : "/" ) + appRelativePath;
				log.debug ( "Looking for [" + fullPath + "]." );
				file = new File ( fullPath );
			}

			// check search dirs specified by app
			if ( !file.exists () )
			{
				for ( File dir : fSearchDirs )
				{
					final File candidate = new File ( dir, appRelativePath );
					log.debug ( "Looking for [" + candidate.getAbsolutePath () + "]." );
					if ( candidate.exists () )
					{
						file = candidate;
						break;
					}
				}
			}
			
			if ( !file.exists () )
			{
				file = new File ( appRelativePath );
			}
		}
		log.debug ( "Given [" + appRelativePath + "], using file [" + file.getAbsolutePath () + "]." );
		return file;
	}

	/**
	 * Get settings in use by this servlet. They can come from the servlet container, from an
	 * optional config file named by the string provided to the constructor, and anything else
	 * the servlet init code (in the concrete class) decides to add.
	 * 
	 * @return settings
	 */
	public rrNvReadable getSettings ()
	{
		return fSettings;
	}

	/**
	 * Get servlet controls. (These are settings that don't get cached.)
	 * @return
	 */
	public DrumlinRuntimeControls getControls ()
	{
		return fRuntimeControls;
	}

	/**
	 * Put an object into the servlet's directory by name.
	 * @param key
	 * @param o
	 */
	public void putObject ( String key, Object o )
	{
		fObjects.put ( key, o );
	}

	/**
	 * Get an object from the servlet's directory by name. If none is found, null is returned.
	 * @param key
	 * @return a previously stored object, or null.
	 */
	public Object getObject ( String key )
	{
		return fObjects.get ( key );
	}

	/**
	 * Get the velocity engine.
	 * @return the Velocity engine
	 */
	public VelocityEngine getVelocity ()
	{
		return fVelocity;
	}

	/**
	 * Get the base context for Velocity. This context is shared among all sessions.
	 * 
	 * @return a velocity context
	 */
	public VelocityContext getBaseContext ()
	{
		return fBaseContext;
	}

	/**
	 * Add an object to the base velocity context. Keep in mind that the base velocity
	 * context is shared among all sessions.
	 * 
	 * @param key
	 * @param o
	 */
	public void addToBaseContext ( String key, Object o )
	{
		if ( fBaseContext == null )
		{
			log.warn ( "Call to addToBaseContext is ignored. You need to init the servlet first." );
			return;
		}
		fBaseContext.put ( key, o );
	}

	/**
	 * Create a session.
	 * @return a session.
	 * @throws rrNvReadable.missingReqdSetting
	 */
	public DrumlinConnection createSession () throws rrNvReadable.missingReqdSetting
	{
		return null;
	}

	/**
	 * Get the servlet's request router.
	 * @return a request router.
	 */
	public DrumlinRequestRouter getRequestRouter ()
	{
		return fRouter;
	}

	/**
	 * Get the servlet's base URL.
	 * @return the $servletRoot value
	 */
	public String getBaseUrl ()
	{
		return fBaseContext.get ( kServletRoot ).toString ();
	}

	/**
	 * Get the shared logging context for this servlet. Anything written to this logging
	 * context will be copied into each thread's logging context when the thread calls
	 * getLoggingContextForThread()
	 * 
	 * @return a common logging context
	 */
	public LoggingContext getCommonLoggingContext ()
	{
		return fLogContext;
	}

	/**
	 * Get a logging context for the current thread that's based on the common logging context.
	 * @return a logging context for the current thread
	 */
	public LoggingContext getLoggingContextForThread ()
	{
		// note that this operation requires everything from the common context
		// to be (re)copied into the target context. That seems slow, but it actually
		// helps prevent the thread from overwriting supposedly common data. It also
		// should be fairly quick compared with the overhead of handling the actual
		// service call.
		
		return new LoggingContextFactory.Builder().
			withBaseContext ( getCommonLoggingContext () ).
			build();
	}

	/**
	 * Override this to take the settings built by the base servlet and return
	 * something wrapping them (or different, even)
	 * @param fromBase
	 * @return a settings object
	 */
	protected rrNvReadable makeSettings ( rrNvReadable fromBase )
	{
		return fromBase;
	}

	/**
	 * Add a directory to the file search directory path.
	 * @param dir
	 */
	protected synchronized void addToFileSearchDirs ( File dir )
	{
		if ( dir.exists() && dir.isDirectory () )
		{
			fSearchDirs.add ( dir );
		}
		else
		{
			log.warn ( "File [" + dir.toString () + "] is not a directory. Ignored." );
		}
	}
	
	/**
	 * Called at the end of servlet initialization. Override servletSetup to do
	 * custom init work in your servlet.
	 * 
	 * @throws drumlinSettings.missingReqdSetting
	 * @throws drumlinSettings.invalidSettingValue
	 * @throws ServletException
	 */
	protected void servletSetup () throws rrNvReadable.missingReqdSetting, rrNvReadable.invalidSettingValue, ServletException {}

	/**
	 * override servletShutdown to do custom shutdown work in your servlet. Note that this isn't always called,
	 * depending on the servlet container.
	 */
	protected void servletShutdown () {}

	@Override
	protected final void service ( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, java.io.IOException
	{
		String logLine = null;
		final long startMs = System.currentTimeMillis ();

		final LoggingContext lc = getLoggingContextForThread();

		try
		{
			populateLoggingContextForThread ( lc, req );

			String methodForLog = req.getMethod();
			if ( methodForLog.length() > 3 ) methodForLog = methodForLog.substring(0,3);

			// build an initial log line
			logLine = req.getRemoteHost () + ":" + req.getRemotePort() + " " + methodForLog + " " + req.getRequestURI ();
			final String queryString = req.getQueryString ();
			if ( queryString != null )
			{
		        logLine = logLine + "?" + queryString;
		    }
			log.debug ( logLine );

			// update the servlet root
			{
				final String uri = URLDecoder.decode ( req.getRequestURI (), "UTF-8" );
				final int uriLen = uri.length ();

				final String path = req.getPathInfo ();
				final int pathLen = ( path == null ? 0 : path.length () );

				final int endIndex = ( path == null ) ? 0 : uriLen - pathLen + 1;
					// FIXME: check endindex!

				String basePart = uri.substring ( 0, endIndex );
				if ( basePart.endsWith ( "/" ) )
				{
					basePart = basePart.substring ( 0, basePart.length () - 1 );
				}

				final String srIs = fBaseContext.get ( kServletRoot ).toString ();
				if ( !srIs.equals ( basePart ) )
				{
					log.info ( "updating $" + kServletRoot + "=" + basePart + " (was " + srIs + ")" );
					fBaseContext.put ( kServletRoot, basePart );
				}
			}

			if ( getSettings().getBoolean ( DrumlinRuntimeControls.kSetting_LogHeaders, false ) )
			{
				log.info ( "--" );
				log.info ( "REQUEST from " + req.getRemoteHost () + " (" + req.getRemoteAddr () + "):" );
				log.info ( "    " + req.getMethod () + " " + req.getPathInfo () + " " + req.getQueryString () );
				log.info ( "" );

				final Enumeration<?> e = req.getHeaderNames ();
				while ( e.hasMoreElements () )
				{
					final String name = e.nextElement ().toString ();
					final String val = req.getHeader ( name );
					log.info ( "    " + name + ": " + val );
				}
				log.info ( "--" );
			}

			String routeName = "unknownRoute";
			
			final DrumlinRequestContext ctx = createHandlingContext ( req, resp, getSession ( req ), fObjects, fRouter );
			try
			{
				final DrumlinRouteInvocation handler = fRouter.route ( ctx.request () );
				routeName = handler.getName ();
				handler.run ( ctx );
			}
			catch ( noMatchingRoute e )
			{
				onError ( ctx, e, new DrumlinErrorHandler ()
				{
					@Override
					public void handle ( DrumlinRequestContext ctx, Throwable cause )
					{
						ctx.response ().sendError ( HttpStatusCodes.k404_notFound, "Not found." );
					}
				} );
			}
			catch ( InvocationTargetException x )
			{
				final Throwable t = x.getCause ();
				if ( t != null )
				{
					onError ( ctx, t, null );
				}
				else
				{
					onError ( ctx, x, null );
				}
			}
			catch ( Throwable t )
			{
				onError ( ctx, t, null );
			}
			
			final long endMs = System.currentTimeMillis ();
			final long durationMs = Math.max ( 0, endMs - startMs );
			onRouteComplete ( routeName, durationMs );
			lc.put ( "timer", durationMs );

			// more ecomp log population
			final int statusCode = ctx.response ().getStatusCode ();
			lc.put ( "statusCode", statusCode >= 500 ? "ERROR":"COMPLETE" );
			switch ( statusCode )
			{
				case HttpStatusCodes.k401_unauthorized:
					lc.put ( "responseCode", 100 );
					break;
				case HttpStatusCodes.k503_serviceUnavailable:
					lc.put ( "responseCode", 200 );
					break;
				case HttpStatusCodes.k200_ok:
				case HttpStatusCodes.k201_created:
				case HttpStatusCodes.k202_accepted:
				case HttpStatusCodes.k203_nonAuthoritativeInformation:
				case HttpStatusCodes.k204_noContent:
				case HttpStatusCodes.k205_resetContent:
				case HttpStatusCodes.k206_partialContent:
					lc.put ( "responseCode", 0 );	// there's no success category... ??
					break;
	
				default:
					// 300-599
					lc.put ( "responseCode", 900 + (statusCode-300) );
			}
			lc.put ( "responseDescription", "HTTP " + statusCode );

			logLine = logLine + " " + statusCode + " " + durationMs + " ms";
		}
		finally
		{
			if ( logLine != null ) log.info ( logLine );
		}
	}

	/**
	 * Override this to provide logging context. This implementation writes requestId,
	 * serviceInstanceId, and ipAddress
	 * 
	 * @param lc
	 * @param req
	 */
	protected void populateLoggingContextForThread ( LoggingContext lc, HttpServletRequest req )
	{
		// We need a "unique" value for the request ID, so we use a UUID (that helps with
		// keeping the ID unique across servers)
		lc.put ( "requestId", UUID.randomUUID ().toString () );

		// service name is the API invoked
		lc.put ( "serviceName", req.getPathInfo () );

		// timing
		lc.put ( EcompFields.kBeginTimestampMs, SaClock.now () );

		// result
		lc.put ( "statusCode", "" );
		lc.put ( "responseCode", "" );

		// remote partner/IP
		final String remoteIp = req.getRemoteAddr ();
		if ( remoteIp != null )
		{
			lc.put ( "ipAddress", remoteIp );
			lc.put ( "clientIpAddress", remoteIp );
			lc.put ( "partnerName", remoteIp );
		}
	}

	/**
	 * Override this to create a custom handling context for your request handlers.
	 * @param req
	 * @param resp
	 * @param dc
	 * @param objects
	 * @param rr
	 * @return
	 */
	protected DrumlinRequestContext createHandlingContext ( HttpServletRequest req, HttpServletResponse resp, DrumlinConnection dc, HashMap<String,Object> objects, DrumlinRequestRouter rr )
	{
		return new DrumlinRequestContext ( this, req, resp, dc, objects, rr );
	}

	protected void onRouteComplete ( String name, long durationMs ) {}

	private void onError ( DrumlinRequestContext ctx, Throwable t, DrumlinErrorHandler defHandler )
	{
		DrumlinErrorHandler eh = fRouter.route ( t );
		if ( eh == null && defHandler != null )
		{
			eh = defHandler;
		}

		if ( eh != null )
		{
			try
			{
				eh.handle ( ctx, t );
			}
			catch ( Throwable tt )
			{
				log.warn ( "Error handler failed, handling a " + t.getClass().getName() + ", with " + tt.getMessage () );
				ctx.response ().sendError ( HttpStatusCodes.k500_internalServerError, t.getMessage () );
			}
		}
		else
		{
			log.warn ( "No handler defined for " + t.getClass().getName() + ". Sending 500." );
			ctx.response ().sendError ( HttpStatusCodes.k500_internalServerError, t.getMessage () );

			final StringWriter sw = new StringWriter ();
			final PrintWriter pw = new PrintWriter ( sw );
			t.printStackTrace ( pw );
			pw.close ();
			log.warn ( sw.toString () );
		}
	}
	
	private DrumlinConnection getSession ( HttpServletRequest req ) throws ServletException
	{
		DrumlinConnection result = null;
		if ( !fSessionLifeCycle.equals ( sessionLifeCycle.NO_SESSION ) )
		{
			try
			{
				final String servletSessionName = getSessionObjectName ( this.getClass () );
				final String flashSessionName = getFlashSessionObjectName ( this.getClass () );
	
				final HttpSession session = req.getSession ( true );

				// is there a prior flash session?
				final DrumlinConnection oldFlashSession = (DrumlinConnection) session.getAttribute ( flashSessionName );
				if ( oldFlashSession != null )
				{
					oldFlashSession.onSessionClose ();
				}
	
				// locate the last session
				result = (DrumlinConnection) session.getAttribute ( servletSessionName );
	
				// if we're using flash sessions, save this session as the last flash session
				// and create a new session
				if ( fSessionLifeCycle.equals ( sessionLifeCycle.FLASH_SESSION ) )
				{
					session.setAttribute ( flashSessionName, result );
					result = null;
				}
	
				if ( result == null )
				{
					result = createSession ();
					if ( result != null )
					{
						session.setAttribute ( servletSessionName, result );
						result.onSessionCreate ( this, new DrumlinConnectionContext ()
						{
							@Override
							public void setInactiveExpiration ( long units, TimeUnit tu )
							{
								final long timeInSeconds = TimeUnit.SECONDS.convert ( units, tu );
								if ( timeInSeconds < 0 || timeInSeconds > Integer.MAX_VALUE )
								{
									throw new IllegalArgumentException ( "Invalid time specification." );
								}
								final int timeInSecondsInt = (int) timeInSeconds;
								session.setMaxInactiveInterval ( timeInSecondsInt );
							}
						} );
					}
				}
	
				if ( result != null )
				{
					result.noteActivity ();
				}
			}
			catch ( rrNvReadable.missingReqdSetting e )
			{
				throw new ServletException ( e );
			}
		}
		return result;
	}

	private static String getSessionObjectName ( Class<?> c )
	{
		return kWebSessionObject + c.getName ();
	}

	private static String getFlashSessionObjectName ( Class<?> c )
	{
		return kFlashSessionObject + c.getName ();
	}

	private rrNvReadable fSettings;
	private final DrumlinRuntimeControls fRuntimeControls;
	private String fBasePath;
	private File fWebInfDir;
	private final rrNvReadable fProvidedPrefs;
	private final String fPrefsConfigFilename;
	private final LinkedList<File> fSearchDirs;
	private final sessionLifeCycle fSessionLifeCycle;
	private DrumlinRequestRouter fRouter;
	private VelocityEngine fVelocity;
	private VelocityContext fBaseContext;
	private final HashMap<String,Object> fObjects;
	private final LoggingContext fLogContext;

	private static final String kWebSessionObject = "drumlin.session.";
	private static final String kFlashSessionObject = "drumlin.flash.";

	public static final String kSetting_BaseTemplateDir = "drumlin.templates.path";
	private static final long serialVersionUID = 1L;
	private static org.slf4j.Logger log = LoggerFactory.getLogger ( DrumlinServlet.class );

	protected void setupResourceLoader ( VelocityEngine ve, rrNvReadable p )
	{
		final String baseTemplateDir = p.getString ( kSetting_BaseTemplateDir, "WEB-INF/templates" );
		final File realDir = findFile ( baseTemplateDir );
		log.info ( "INIT: velocity templates: " + realDir.getAbsolutePath () );
		final boolean caching = Boolean.parseBoolean ( System.getProperty ( "drumlin.cacheTemplates", "true" ) );

		ve.setProperty ( RuntimeConstants.RESOURCE_LOADER, "file, class" );

		ve.setProperty ( RuntimeConstants.FILE_RESOURCE_LOADER_PATH, realDir.getAbsolutePath () );
		ve.setProperty ( "file.resource.loader.cache", caching );

		ve.setProperty ( "class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader" );
	}
}
