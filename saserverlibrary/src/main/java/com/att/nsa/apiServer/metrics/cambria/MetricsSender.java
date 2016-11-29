/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer.metrics.cambria;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaPublisher;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.metrics.CdmMetricsRegistry;

/**
 * MetricsSender will send the given metrics registry content as an event on the Cambria
 * event broker to the given topic.
 * 
 * @author peter
 *
 */
public class MetricsSender implements Runnable
{
	public static final String kSetting_CambriaEnabled = "metrics.send.cambria.enabled";
	public static final String kSetting_CambriaBaseUrl = "metrics.send.cambria.baseUrl";
	public static final String kSetting_CambriaTopic = "metrics.send.cambria.topic";
	public static final String kSetting_CambriaSendFreqSecs = "metrics.send.cambria.sendEverySeconds";

	/**
	 * Schedule a periodic send of the given metrics registry using the given settings
	 * container for the Cambria location, topic, and send frequency.<br/>
	 * <br/>
	 * If the enabled flag is false, this method returns null.
	 *  
	 * @param scheduler
	 * @param metrics
	 * @param settings
	 * @return a handle to the scheduled task
	 */
	public static ScheduledFuture<?> sendPeriodically ( ScheduledExecutorService scheduler,
		CdmMetricsRegistry metrics, rrNvReadable settings, String defaultTopic )
	{
		if ( settings.getBoolean ( kSetting_CambriaEnabled, true ) )
		{
			return MetricsSender.sendPeriodically ( scheduler, metrics,
				settings.getString ( kSetting_CambriaBaseUrl, "localhost" ),
				settings.getString ( kSetting_CambriaTopic, defaultTopic ), 
				settings.getInt ( kSetting_CambriaSendFreqSecs, 30 ) );
		}
		else
		{
			return null;
		}
	}

	/**
	 * Schedule a periodic send of the metrics registry to the given Cambria broker
	 * and topic. 
	 * 
	 * @param scheduler
	 * @param metrics the registry to send
	 * @param cambriaBaseUrl the base URL for Cambria
	 * @param topic the topic to publish on
	 * @param everySeconds how frequently to publish
	 * @return a handle to the scheduled task
	 */
	public static ScheduledFuture<?> sendPeriodically ( ScheduledExecutorService scheduler,
		CdmMetricsRegistry metrics, String cambriaBaseUrl, String topic, int everySeconds )
	{
		return scheduler.scheduleAtFixedRate ( 
			new MetricsSender ( metrics, cambriaBaseUrl, topic ),
			everySeconds, everySeconds, TimeUnit.SECONDS );
	}

	/**
	 * Create a metrics sender.
	 * 
	 * @param metrics
	 * @param cambriaBaseUrl
	 * @param topic
	 */
	public MetricsSender ( CdmMetricsRegistry metrics, String cambriaBaseUrl, String topic )
	{
		try
		{
			fMetrics = metrics;
			fHostname = InetAddress.getLocalHost ().getHostName ();

			// setup a publisher that will send metrics immediately
			fCambria = new CambriaClientBuilders.PublisherBuilder ()
				.usingHosts ( cambriaBaseUrl )
				.onTopic ( topic )
				.limitBatch ( 1, 100 )
				.build ();
		}
		catch ( UnknownHostException | MalformedURLException | GeneralSecurityException e )
		{
			log.warn ( "Unable to get localhost address in MetricsSender constructor.", e );
			throw new RuntimeException ( e );
		}
	}

	/**
	 * Send on demand.
	 */
	public void send ()
	{
		try
		{
			final JSONObject o = fMetrics.toJson ();
			o.put ( "hostname", fHostname );
			o.put ( "now", System.currentTimeMillis () );
			fCambria.send ( fHostname, o.toString () );
		}
		catch ( JSONException e )
		{
			log.warn ( "Error posting metrics to Cambria: " + e.getMessage () );
		}
		catch ( IOException e )
		{
			log.warn ( "Error posting metrics to Cambria: " + e.getMessage () );
		}
	}

	/**
	 * Run() calls send(). It's meant for use in a background-scheduled task.
	 */
	@Override
	public void run ()
	{
		send ();
	}

	private final CdmMetricsRegistry fMetrics;
	private final CambriaPublisher fCambria;
	private final String fHostname;

	private static final Logger log = LoggerFactory.getLogger ( MetricsSender.class );
}
