/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.app.htmlForms.mime.DrumlinMimePart;
import com.att.nsa.drumlin.app.htmlForms.mime.DrumlinMimePartFactory;
import com.att.nsa.drumlin.app.htmlForms.mime.DrumlinMimePartsReader;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.drumlin.service.standards.HttpMethods;
import com.att.nsa.drumlin.till.collections.rrMultiMap;
import com.att.nsa.drumlin.till.data.rrConvertor;

/**
 * A form post wrapper provides form related methods over a DrumlinRequest. 
 * 
 * @author peter@rathravane.com
 *
 */
public class DrumlinFormPostWrapper
{
	/**
	 * Construct a form post wrapper from a request.
	 * @param req
	 */
	public DrumlinFormPostWrapper ( DrumlinRequest req )
	{
		this ( req, null );
	}

	/**
	 * Construct a form post wrapper from a request, and use the given MIME reader
	 * part factory. The MIME reader is only invoked if the request's content type
	 * is multipart/form-data.
	 * 
	 * @param req
	 * @param mimePartFactory If null, use the built-in part factory.
	 */
	public DrumlinFormPostWrapper ( DrumlinRequest req, DrumlinMimePartFactory mimePartFactory )
	{
		fRequest = req;
		final String ct = req.getContentType ();

		fIsMultipartFormData = ct != null && ct.startsWith ( "multipart/form-data" );
		fPartFactory = mimePartFactory == null ? new simpleStorage () : mimePartFactory;
		fParsedValues = new HashMap<String,DrumlinMimePart> ();
		fParseComplete = false;
	}

	/**
	 * this must be called to cleanup mime part resources (e.g. tmp files)
	 */
	public void close ()
	{
		for ( DrumlinMimePart vi : fParsedValues.values () )
		{
			vi.discard ();
		}
	}
	
	@Override
	public String toString ()
	{
		final StringBuffer sb = new StringBuffer ();

		sb.append ( fRequest.getMethod ().toUpperCase () ).append ( " {" );
		if ( fIsMultipartFormData )
		{
			if ( fParseComplete )
			{
				for ( Entry<String, DrumlinMimePart> e : fParsedValues.entrySet () )
				{
					sb.append ( e.getKey () + ":" );
					final DrumlinMimePart mp = e.getValue ();
					if ( mp.getAsString () != null )
					{
						 sb.append ( "'" + mp.getAsString () + "' " );
					}
					else
					{
						sb.append ( "(data) " );
					}
				}
			}
			else
			{
				sb.append ( "not parsed yet" );
			}
		}
		else
		{
			for ( Entry<?, ?> e : fRequest.getParameterMap().entrySet () )
			{
				sb.append ( e.getKey ().toString () + ":'" + e.getValue ().toString () + "' " );
			}
		}
		sb.append ( " }" );

		return sb.toString ();
	}

	/**
	 * Is the underlying request a POST? (Not a PUT, not anything else. Just POST.)
	 * @return true if the underlying request is a POST.
	 */
	public boolean isPost ()
	{
		return fRequest.getMethod ().toLowerCase ().equals ( HttpMethods.POST );
	}

	/**
	 * Does the form have a given parameter (aka field)
	 * @param name
	 * @return true if the named parameter/field exists in the form post
	 */
	public boolean hasParameter ( String name )
	{
		parseIfNeeded ();

		return fIsMultipartFormData ?
			fParsedValues.containsKey ( name ) :
			fRequest.getParameterMap ().containsKey ( name );
	}

	/**
	 * Get the form post parameters in a map from name to string value.
	 * @return a map of post parameters
	 */
	public Map<String,String> getValues ()
	{
		final HashMap<String,String> map = new HashMap<String,String> ();

		parseIfNeeded ();

		if ( fIsMultipartFormData )
		{
			for ( Map.Entry<String,DrumlinMimePart> e : fParsedValues.entrySet () )
			{
				final String val = e.getValue ().getAsString ();
				if ( val != null )
				{
					map.put ( e.getKey(), val );
				}
			}
		}
		else
		{
			for ( Map.Entry<?,?> e : fRequest.getParameterMap ().entrySet() )
			{
				final String key = e.getKey ().toString ();
				final String[] vals = (String[]) e.getValue ();
				String valToUse = "";
				if ( vals.length > 0 )
				{
					valToUse = vals[0];
				}
				map.put ( key, valToUse );
			}
		}
		return map;
	}

	/**
	 * Does the form contain the given field? This goes beyond hasParameter() to check
	 * on a multipart MIME post whether the value provided is a string.
	 * 
	 * @param name
	 * @return true if the named field exists
	 */
	public boolean isFormField ( String name )
	{
		boolean result = false;
		if ( hasParameter ( name ) )
		{
			if ( fIsMultipartFormData )
			{
				final DrumlinMimePart val = fParsedValues.get ( name );
				result = ( val != null && val.getAsString () != null );
			}
			else
			{
				result = true;
			}
		}
		return result;
	}

	/**
	 * Get the value of a field as a string. This returns null for MIME parts like
	 * file uploads -- the value has to be available as a string rather than a stream.
	 * 
	 * @param name
	 * @return a string for the named field
	 */
	public String getValue ( String name )
	{
		parseIfNeeded ();

		String result = null;
		if ( fIsMultipartFormData )
		{
			final DrumlinMimePart val = fParsedValues.get ( name );
			
			result = null;
			if ( val != null && val.getAsString () != null )
			{
				result = val.getAsString ().trim ();
			}
		}
		else
		{
			result = fRequest.getParameter ( name );
			if ( result != null )
			{
				result = result.trim ();
			}
		}
		return result;
	}

	/**
	 * A convenience version of getValue(String). Useful for passing enums. The argument
	 * is converted to a string.
	 * @param o
	 * @return the string value for the given field
	 */
	public String getValue ( Object o )
	{
		return getValue ( o.toString () );
	}

	/**
	 * Get the named value, or return defVal if it does not exist on the form.
	 * @param key
	 * @param defVal
	 * @return the value from the form, or the default value
	 */
	public String getValue ( String key, String defVal )
	{
		String result = getValue ( key );
		if ( result == null )
		{
			result = defVal;
		}
		return result;
	}

	/**
	 * A convenience version for use with Enums. The field name argument is converted to a string.
	 * @param fieldName
	 * @param defVal
	 * @return
	 */
	public String getValue ( Object fieldName, String defVal )
	{
		return getValue ( fieldName.toString (), defVal );
	}

	/**
	 * Get the named value as a boolean, or return valIfMissing if no such field exists.
	 * @param name
	 * @param valIfMissing
	 * @return true/false
	 */
	public boolean getValueBoolean ( String name, boolean valIfMissing )
	{
		boolean result = valIfMissing;
		final String val = getValue ( name );
		if ( val != null )
		{
			result = rrConvertor.convertToBooleanBroad ( val );
		}
		return result;
	}

	/**
	 * A convenience version for use with Enums. The field name argument is converted to a string.
	 * @param fieldName
	 * @param valIfMissing
	 * @return true/false
	 */
	public boolean getValueBoolean ( Object fieldName, boolean valIfMissing )
	{
		return getValueBoolean ( fieldName.toString() , valIfMissing );
	}
	
	/**
	 * Change the value for a given field.
	 * @param fieldName
	 * @param newVal
	 */
	public void changeValue ( String fieldName, String newVal )
	{
		parseIfNeeded ();

		if ( fIsMultipartFormData )
		{
			if ( fParsedValues.containsKey ( fieldName ) )
			{
				fParsedValues.get ( fieldName ).discard ();
			}
			
			final inMemoryFormDataPart part = new inMemoryFormDataPart ( "", "form-data; name=\"" + fieldName + "\"" );
			final byte[] array = newVal.getBytes ();
			part.write ( array, 0, array.length );
			part.close ();
			fParsedValues.put ( fieldName, part );
		}
		else
		{
			fRequest.changeParameter ( fieldName, newVal );
		}
	}

	/**
	 * Get the MIME part for a given field name.
	 * @param name
	 * @return a MIME part
	 */
	public DrumlinMimePart getStream ( String name )
	{
		parseIfNeeded ();

		DrumlinMimePart result = null;
		if ( fIsMultipartFormData )
		{
			final DrumlinMimePart val = fParsedValues.get ( name );
			if ( val != null && val.getAsString () == null )
			{
				return val;
			}
		}
		return result;
	}
	
	private final DrumlinRequest fRequest;
	private final boolean fIsMultipartFormData;
	private boolean fParseComplete;
	private final HashMap<String,DrumlinMimePart> fParsedValues;
	private final DrumlinMimePartFactory fPartFactory;

	private void parseIfNeeded ()
	{
		if ( fIsMultipartFormData && !fParseComplete )
		{
			try
			{
				final String ct = fRequest.getContentType ();
				int boundaryStartIndex = ct.indexOf ( kBoundaryTag );
				if ( boundaryStartIndex != -1 )
				{
					boundaryStartIndex = boundaryStartIndex + kBoundaryTag.length ();
					final int semi = ct.indexOf ( ";", boundaryStartIndex );
					int boundaryEndIndex = semi == -1 ? ct.length () : semi;

					final String boundary = ct.substring ( boundaryStartIndex, boundaryEndIndex ).trim ();
					final DrumlinMimePartsReader mmr = new DrumlinMimePartsReader ( boundary, fPartFactory );
					final InputStream is = fRequest.getBodyStream ();
					mmr.read ( is );
					is.close ();

					for ( DrumlinMimePart mp : mmr.getParts () )
					{
						fParsedValues.put ( mp.getName(), mp );
					}
				}
			}
			catch ( IOException e )
			{
				log.warn ( "There was a problem reading a multipart/form-data POST: " + e.getMessage () );
			}
			fParseComplete = true;
		}
	}

	private static final String kBoundaryTag = "boundary=";

	static final org.slf4j.Logger log = LoggerFactory.getLogger ( DrumlinFormPostWrapper.class );

	public static abstract class basePart implements DrumlinMimePart
	{
		public basePart ( String contentType, String contentDisp )
		{
			fType = contentType;
			fDisp = contentDisp;

			fDispMap = new HashMap<String,String> ();
			parseDisposition ( contentDisp );

			final int nameSpot = fDisp.indexOf ( "name=\"" );
			String namePart = fDisp.substring ( nameSpot + "name=\"".length () );
			final int closeQuote = namePart.indexOf ( "\"" );
			namePart = namePart.substring ( 0, closeQuote );
			fName = namePart;
		}

		@Override
		public String getContentType ()
		{
			return fType;
		}

		@Override
		public String getContentDisposition ()
		{
			return fDisp;
		}

		@Override
		public String getContentDispositionValue ( String key )
		{
			return fDispMap.get ( key );
		}

		@Override
		public String getName ()
		{
			return fName;
		}

		@Override
		public void discard ()
		{
		}

		private final String fType;
		private final String fDisp;
		private final String fName;
		private final HashMap<String,String> fDispMap;

		// form-data; name="file"; filename="IMG_21022013_122919.png"
		private void parseDisposition ( String contentDisp )
		{
			final String[] parts = contentDisp.split ( ";");
			for ( String part : parts )
			{
				String key = part.trim ();
				String val = "";
				final int eq = key.indexOf ( '=' );
				if ( eq > -1 )
				{
					val = key.substring ( eq+1 );
					key = key.substring ( 0, eq );

					// if val is in quotes, remove them
					if ( val.startsWith ( "\"" ) && val.endsWith ( "\"" ) )
					{
						val = val.substring ( 1, val.length () - 1 );
					}
				}
				fDispMap.put ( key, val );
			}
		}
	}
	
	public static class inMemoryFormDataPart extends basePart
	{
		public inMemoryFormDataPart ( String ct, String cd )
		{
			super ( ct, cd );
			fValue = "";
		}
		
		@Override
		public void write ( byte[] line, int offset, int length )
		{
			fValue = new String ( line, offset, length );
		}

		@Override
		public void close ()
		{
		}

		@Override
		public InputStream openStream () throws IOException
		{
			throw new IOException ( "Opening stream on in-memory form data." );
		}

		@Override
		public String getAsString ()
		{
			return fValue;
		}

		private String fValue;
	}

	private static class tmpFilePart extends basePart
	{
		public tmpFilePart ( String ct, String cd ) throws IOException
		{
			super ( ct, cd );

			fFile = File.createTempFile ( "drumlin.", ".part" );
			fStream = new FileOutputStream ( fFile );
		}

		@Override
		public void write ( byte[] line, int offset, int length ) throws IOException
		{
			if ( fStream != null )
			{
				fStream.write ( line, offset, length );
			}
		}

		@Override
		public void close () throws IOException
		{
			if ( fStream != null )
			{
				fStream.close ();
				fStream = null;
			}
		}

		@Override
		public InputStream openStream () throws IOException
		{
			if ( fStream != null )
			{
				log.warn ( "Opening input stream on tmp file before it's fully written." );
			}
			return new FileInputStream ( fFile );
		}

		@Override
		public String getAsString ()
		{
			return null;
		}

		@Override
		public void discard ()
		{
			fFile.delete ();
			fFile = null;
			fStream = null;
		}

		private File fFile;
		private FileOutputStream fStream;
	}

	static class simpleStorage implements DrumlinMimePartFactory
	{
		@Override
		public DrumlinMimePart createPart ( rrMultiMap<String, String> partHeaders ) throws IOException
		{
			final String contentDisp = partHeaders.getFirst ( "content-disposition" );
			if ( contentDisp != null && contentDisp.contains ( "filename=\"" ) )
			{
				return new tmpFilePart ( partHeaders.getFirst ( "content-type" ), contentDisp );
			}
			else
			{
				return new inMemoryFormDataPart ( partHeaders.getFirst ( "content-type" ), contentDisp );
			}
		}
	}
}
