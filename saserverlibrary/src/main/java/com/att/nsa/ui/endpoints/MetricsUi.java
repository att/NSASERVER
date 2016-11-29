/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.ui.endpoints;

import java.io.IOException;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

public class MetricsUi
{
	public static void getMetricsMain ( DrumlinRequestContext ctx )
	{
//		final UiServlet servlet = ((UiServlet) ctx.getServlet ());
//
//		final HpProcessingEngine<?> config = servlet.getConfig ();
//		ctx.renderer ().put ( "config", config );
//
//		final ConfigurableCorrelationEngine<?,?> cce = servlet.getCce ();
//		ctx.renderer ().put ( "cce", cce );
//
//		final CdmMetricsRegistry m = servlet.getMetrics ();
//		ctx.renderer ().put ( "metrics", m );
//
//		// put some organization around hierarchical naming
//		ctx.renderer ().put ( "metricEntries", m.getEntries () );
//
//		ctx.renderer ().renderTemplate ( "metrics.html" );
	}

	public static void getAllMetrics ( DrumlinRequestContext ctx ) throws IOException
	{
//		final UiServlet servlet = ((UiServlet) ctx.getServlet ());
//		final CdmMetricsRegistry m = servlet.getMetrics ();
//
//		final String format = ctx.request ().getParameter ( "format", "json" ).trim ().toLowerCase ();
//		if ( format.equals ( "json" ) )
//		{
//			final JSONObject o = new JSONObject ();
//			for ( Entry<String, CdmMeasuredItem> mi : m.getItems ().entrySet () )
//			{
//				o.put ( mi.getKey(), mi.getValue().getRawValueString () );
//			}
//			ctx.response ().sendErrorAndBody ( HttpStatusCodes.k200_ok, o.toString(), MimeTypes.kAppJson );
//		}
//		else if ( format.equals ( "xml" ) )
//		{
//			// XML
//			ctx.response ().setStatus ( HttpStatusCodes.k200_ok );
//			ctx.response ().setContentType ( MimeTypes.kAppXml );
//			final PrintWriter pw = ctx.response ().getStreamForTextResponse ();
//			pw.println ( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" );
//			pw.println ( "<metrics>" );
//			for ( Entry<String, CdmMeasuredItem> mi : m.getItems ().entrySet () )
//			{
//				pw.println ( "<metric><key>" + mi.getKey() + "</key><value>" +  mi.getValue().getRawValueString () + "</value></metric>" );
//			}
//			pw.println ( "</metrics>" );
//			pw.close ();
//		}
//		else
//		{
//			// plain text
//			ctx.response ().setStatus ( HttpStatusCodes.k200_ok );
//			ctx.response ().setContentType ( MimeTypes.kPlainText );
//			final PrintWriter pw = ctx.response ().getStreamForTextResponse ();
//			for ( Entry<String, CdmMeasuredItem> mi : m.getItems ().entrySet () )
//			{
//				pw.println ( mi.getKey() + "," +  mi.getValue().getRawValueString () );
//			}
//			pw.close ();
//		}
//	}
//
//	public static void getMetric ( DrumlinRequestContext ctx, String id )
//	{
//		final UiServlet servlet = ((UiServlet) ctx.getServlet ());
//
//		final CdmMetricsRegistry m = servlet.getMetrics ();
//		final CdmMeasuredItem mi = m.getItem ( id );
//		if ( mi == null )
//		{
//			ctx.response ().sendError ( HttpStatusCodes.k404_notFound, "metrics [" + id + "] does not exist" );
//		}
//		else
//		{
//			ctx.response().sendErrorAndBody ( HttpStatusCodes.k200_ok, mi.getRawValueString ().toString(), MimeTypes.kPlainText );
//		}
	}
}
