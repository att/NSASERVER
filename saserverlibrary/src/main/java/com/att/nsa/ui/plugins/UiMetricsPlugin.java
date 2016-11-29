/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.framework.rendering.DrumlinRenderContext;
import com.att.nsa.drumlin.service.framework.routing.DrumlinRequestRouter;
import com.att.nsa.drumlin.service.framework.routing.playish.DrumlinPlayishInstanceCallRoutingSource;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import com.att.nsa.drumlin.service.standards.MimeTypes;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.metrics.CdmMeasuredItem;
import com.att.nsa.metrics.CdmMetricsRegistry;
import com.att.nsa.ui.UiPlugin;
import com.att.nsa.ui.UiSession;

public class UiMetricsPlugin implements UiPlugin
{
	public void setMetrics ( CdmMetricsRegistry metrics )
	{
		fMetrics = metrics;
	}

	@Override
	public String getUiName ()
	{
		return "metrics";
	}

	@Override
	public String getUiLink ()
	{
		return "/metrics";
	}

	@Override
	public List<String> getUiCssList ()
	{
		return new LinkedList<String> ();
	}

	@Override
	public void configure ( rrNvReadable settings )
	{
		// TODO Auto-generated method stub
		
	}

	public void setupRouting ( DrumlinRequestRouter router )
	{
		final DrumlinPlayishInstanceCallRoutingSource src = new DrumlinPlayishInstanceCallRoutingSource ( this );
		src.addRoute ( "get", "/metrics", this.getClass().getName() + ".getAll" );
		src.addRoute ( "get", "/metrics/clear", this.getClass().getName() + ".clear" );
		src.addRoute ( "get", "/api/metrics", this.getClass().getName() + ".getAllMetrics" );
		src.addRoute ( "get", "/api/metrics/{id}", this.getClass().getName() + ".getMetric" );
		router.addRouteSource ( src );
	}

	public void getAll ( DrumlinRequestContext ctx )
	{
		final DrumlinRenderContext renderer = ctx.renderer();

		renderer.put ( "metrics", fMetrics );
		renderer.put ( "metricEntries", fMetrics.getEntries () );

		renderer.renderTemplate ( "/templates/metrics.html" );
	}
	
	public void clear ( DrumlinRequestContext ctx )
	{
		// FIXME: there's no reset mechanism!
		
		getAll ( ctx );
	}

	public void getAllMetrics ( DrumlinRequestContext ctx ) throws IOException
	{
		final String format = ctx.request ().getParameter ( "format", "json" ).trim ().toLowerCase ();
		if ( format.equals ( "json" ) )
		{
			final JSONObject o = new JSONObject ();
			for ( Entry<String, CdmMeasuredItem> mi : fMetrics.getItems ().entrySet () )
			{
				o.put ( mi.getKey(), mi.getValue().getRawValueString () );
			}
			ctx.response ().sendErrorAndBody ( HttpStatusCodes.k200_ok, o.toString(), MimeTypes.kAppJson );
		}
		else if ( format.equals ( "xml" ) )
		{
			// XML
			ctx.response ().setStatus ( HttpStatusCodes.k200_ok );
			ctx.response ().setContentType ( MimeTypes.kAppXml );
			final PrintWriter pw = ctx.response ().getStreamForTextResponse ();
			pw.println ( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" );
			pw.println ( "<metrics>" );
			for ( Entry<String, CdmMeasuredItem> mi : fMetrics.getItems ().entrySet () )
			{
				pw.println ( "<metric><key>" + mi.getKey() + "</key><value>" +  mi.getValue().getRawValueString () + "</value></metric>" );
			}
			pw.println ( "</metrics>" );
			pw.close ();
		}
		else
		{
			// plain text
			ctx.response ().setStatus ( HttpStatusCodes.k200_ok );
			ctx.response ().setContentType ( MimeTypes.kPlainText );
			final PrintWriter pw = ctx.response ().getStreamForTextResponse ();
			for ( Entry<String, CdmMeasuredItem> mi : fMetrics.getItems ().entrySet () )
			{
				pw.println ( mi.getKey() + "," +  mi.getValue().getRawValueString () );
			}
			pw.close ();
		}
	}

	public void getMetric ( DrumlinRequestContext ctx, String id )
	{
		final CdmMeasuredItem mi = fMetrics.getItem ( id );
		if ( mi == null )
		{
			ctx.response ().sendError ( HttpStatusCodes.k404_notFound, "metrics [" + id + "] does not exist" );
		}
		else
		{
			ctx.response().sendErrorAndBody ( HttpStatusCodes.k200_ok, mi.getRawValueString ().toString(), MimeTypes.kPlainText );
		}
	}

	@Override
	public void onNewSession ( UiSession s )
	{
	}

	private CdmMetricsRegistry fMetrics;
}
