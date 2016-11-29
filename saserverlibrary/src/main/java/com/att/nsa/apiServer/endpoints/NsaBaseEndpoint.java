package com.att.nsa.apiServer.endpoints;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.att.nsa.apiServer.CommonServlet;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.framework.context.DrumlinResponse;
import com.att.nsa.drumlin.service.standards.MimeTypes;
import com.att.nsa.security.NsaAuthenticatorService;
import com.att.nsa.security.ReadWriteSecuredResource.AccessDeniedException;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;

/**
 * Convenience base class for endpoint classes.
 * @author peter
 *
 * @param <T>
 */
public class NsaBaseEndpoint
{
	protected static final int kBufferLength = 4096;

	public interface StreamWriter
	{
		void write ( OutputStream os ) throws IOException;
	}

	/**
	 * Non-idempotent GET requests should have no-cache flags in the response header 
	 * @param ctx
	 */
	public static void setNoCacheHeadings ( DrumlinRequestContext ctx )
	{
		final DrumlinResponse r = ctx.response ();
		r.writeHeader ( "Cache-Control", "no-store, no-cache, must-revalidate" );
		r.writeHeader ( "Pragma", "no-cache" );
		r.writeHeader ( "Expires", "0" );
	}

	/**
	 * Send a success response with content.
	 * @param ctx
	 * @param result
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void respondOk ( DrumlinRequestContext ctx, JSONObject result ) throws IOException
	{
		respondOkWithStream ( ctx, "application/json", new ByteArrayInputStream(result.toString(4).getBytes ()) );
	}

	/**
	 * Send a success response without content.
	 * @param ctx
	 * @param result
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void respondOkNoContent ( DrumlinRequestContext ctx ) throws IOException
	{
		ctx.response ().setStatus ( HttpServletResponse.SC_NO_CONTENT );
	}

	/**
	 * Send a success response with content.
	 * @param ctx
	 * @param result
	 * @throws JSONException
	 * @throws IOException
	 */
	public static void respondOkWithHtml ( DrumlinRequestContext ctx, String html ) throws IOException
	{
		respondOkWithStream ( ctx, "text/html", new ByteArrayInputStream(html.toString().getBytes ()) );
	}

	/**
	 * Send a success response with a content stream.
	 * @param ctx
	 * @param mediaType
	 * @param is
	 * @throws IOException
	 */
	public static void respondOkWithStream ( DrumlinRequestContext ctx, String mediaType, final InputStream is ) throws IOException
	{
		respondOkWithStream ( ctx, mediaType, new StreamWriter ()
		{
			@Override
			public void write ( OutputStream os ) throws IOException
			{
				copyStream ( is, os );
			}
		});
	}

	/**
	 * Send a success response with a content stream.
	 * @param ctx
	 * @param mediaType
	 * @param is
	 * @throws IOException
	 */
	public static void respondOkWithStream ( DrumlinRequestContext ctx, String mediaType, final StreamWriter writer ) throws IOException
	{
		ctx.response ().setStatus ( HttpServletResponse.SC_OK );
		final OutputStream os = ctx.response ().getStreamForBinaryResponse ( mediaType );
		writer.write ( os );
	}

	/**
	 * Respond to the client with the given error code and status message
	 * @param ctx
	 * @param errCode
	 * @param msg
	 * @throws IOException
	 */
	public static void respondWithError ( DrumlinRequestContext ctx, int errCode, String msg ) throws IOException
	{
		ctx.response ().sendError ( errCode, msg );
	}

	/**
	 * Respond to the client with the given error code and status message and a JSON body
	 * @param ctx
	 * @param errCode
	 * @param msg
	 * @throws IOException
	 */
	public static void respondWithErrorInJson ( DrumlinRequestContext ctx, int errCode, String msg ) throws IOException
	{
		final JSONObject o = new JSONObject ();
		o.put ( "status", errCode );
		o.put ( "message", msg );
		respondWithError ( ctx, errCode, o );
	}

	/**
	 * Respond to the client with the given error code and status message
	 * @param ctx
	 * @param errCode
	 * @param msg
	 * @throws IOException
	 */
	public static void respondWithError ( DrumlinRequestContext ctx, int errCode, JSONObject body ) throws IOException
	{
		ctx.response ().sendErrorAndBody ( errCode, body.toString ( 4 ), MimeTypes.kAppJson );
	}

	/**
	 * Copy from the input stream to the output stream, then close the output stream.
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copyStream ( InputStream in, OutputStream out ) throws IOException
	{
		copyStream ( in, out, kBufferLength );
	}

	/**
	 * Copy from the input stream to the output stream, then close the output stream.
	 * @param in
	 * @param out
	 * @param bufferSize
	 * @throws IOException
	 */
	public static void copyStream ( InputStream in, OutputStream out, int bufferSize ) throws IOException
	{
		final byte[] buffer = new byte [bufferSize];
		int len;
		while ( ( len = in.read ( buffer ) ) != -1 )
		{
			out.write ( buffer, 0, len );
		}
		out.close ();
	}

	/**
	 * Get the typed servlet (this will throw ClassCastException if it's not the 
	 * correct servlet type!)
	 * @param ctx
	 * @return
	 */
	public static CommonServlet getServlet ( DrumlinRequestContext ctx )
	{
		return (CommonServlet) ctx.getServlet ();
	}

	/**
	 * Get the user associated with the incoming request, or null if the user is not authenticated.
	 * @param ctx
	 * @return
	 */
	public static NsaSimpleApiKey getAuthenticatedUser ( DrumlinRequestContext ctx )
	{
		final CommonServlet s = getServlet ( ctx );
		final NsaAuthenticatorService<NsaSimpleApiKey> m = s.getSecurityManager ();
		return m.authenticate ( ctx );
	}

	/**
	 * Authenticate the caller as the system administrator
	 * @param ctx
	 * @throws AccessDeniedException
	 */
	public static NsaSimpleApiKey adminAuthenticate ( DrumlinRequestContext ctx ) throws AccessDeniedException
	{
		final CommonServlet s = getServlet ( ctx );
		final NsaAuthenticatorService<NsaSimpleApiKey> m = s.getSecurityManager ();
		final NsaSimpleApiKey user = m.authenticate ( ctx );
		if ( user == null || !user.getKey ().equals ( "admin" ) )
		{
			throw new AccessDeniedException ();
		}
		return user;
	}

	/**
	 * Read a JSON object body
	 * @param ctx
	 * @return a JSON object 
	 * @throws JSONException
	 * @throws IOException
	 */
	public static JSONObject readJsonBody ( DrumlinRequestContext ctx ) throws JSONException, IOException
	{
		return new JSONObject ( new JSONTokener ( ctx.request ().getBodyStream () ) );
	}

	public static void sendJson ( DrumlinResponse r, JSONObject json ) throws IOException
	{
		sendJson ( r, json.toString (4) );
	}

	public static void sendJson ( DrumlinResponse r, String json ) throws IOException
	{
		final PrintWriter pw = r.getStreamForTextResponse ( MimeTypes.kAppJson );
		pw.println ( json );
	}

	public static void sendError ( DrumlinResponse r, int statusCode, String msg ) throws IOException
	{
		final JSONObject o = new JSONObject ()
			.put ( "error", msg )
			.put ( "status", statusCode )
		;
		r.sendErrorAndBody ( statusCode, o.toString (), MimeTypes.kAppJson );
	}
}

