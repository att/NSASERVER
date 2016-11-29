/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * A form validation tool.
 * @author peter@rathravane.com
 *
 */
public class DrumlinFormValidator
{
	/**
	 * Construct an empty form validator.
	 */
	public DrumlinFormValidator ()
	{
		this ( null );
	}

	/**
	 * Construct a form validator using another form validator as a starting point.
	 * This is useful for "wizard" UIs that carry form data along through each step.
	 * The validator for step 2 is based on the validator for step 1, step 3 is based
	 * on step 2, etc.
	 * 
	 * @param wrapped
	 */
	public DrumlinFormValidator ( DrumlinFormValidator wrapped )
	{
		fMap = new HashMap<String,DrumlinFormFieldInfo> ();
		fValidators = new LinkedList<DrumlinFormValidationStep> ();

		if ( wrapped != null )
		{
			addValidation ( new wrapper ( wrapped ) );
		}
	}

	/**
	 * <p>Validate the given form (referenced by the form wrapper) using the validators
	 * registered on this object. Validation is done at the field level first, then
	 * at the form level.</p>
	 * 
	 * <p>Validation problems are collected into an exception. If the exception instance
	 * contains any errors after all validation steps complete, it's thrown.</p> 
	 * 
	 * @param context
	 * @param w
	 * @throws DrumlinInvalidFormException if a validation step fails
	 */
	public void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper w ) throws DrumlinInvalidFormException
	{
		final DrumlinInvalidFormException ve = new DrumlinInvalidFormException ();
		for ( DrumlinFormFieldInfo fi : fMap.values () )
		{
			fi.validate ( context, w, ve );
		}
		for ( DrumlinFormValidationStep step : fValidators )
		{
			step.validate ( context, w, null, ve );
		}
		if ( ve.size () > 0 )
		{
			throw ve;
		}
	}

	/**
	 * Get the fields known to this validator, along with their field info objects.
	 * @return a map from field name to field info
	 */
	public Map<String,DrumlinFormFieldInfo> getFields ()
	{
		return fMap;
	}

	/**
	 * Get or create a field info object for a named field. Then add validation requirements
	 * to the field.
	 * @param name
	 * @return a field info object
	 */
	public DrumlinFormFieldInfo field ( String name )
	{
		DrumlinFormFieldInfo fi = fMap.get ( name );
		if ( fi == null )
		{
			fi = new DrumlinFormFieldInfo ( name );
			fMap.put ( name, fi );
		}
		return fi;
	}

	/**
	 * Add a form-level validation step to this validator.
	 * @param step
	 */
	public void addValidation ( DrumlinFormValidationStep step )
	{
		fValidators.add ( step );
	}

	private final HashMap<String,DrumlinFormFieldInfo> fMap;
	private final LinkedList<DrumlinFormValidationStep> fValidators;

	private class wrapper implements DrumlinFormValidationStep
	{
		public wrapper ( DrumlinFormValidator step )
		{
			fWrapped = step;
		}

		@Override
		public void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper form, DrumlinFormFieldInfo field, DrumlinInvalidFormException err )
		{
			try
			{
				fWrapped.validate ( context, form );
			}
			catch ( DrumlinInvalidFormException e )
			{
				err.addProblemsFrom ( e );
			}
		}

		private DrumlinFormValidator fWrapped;
	}
}
