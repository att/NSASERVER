/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms;

import java.util.LinkedList;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * Information about a form field.
 * 
 * @author peter@rathravane.com
 *
 */
public class DrumlinFormFieldInfo
{
	/**
	 * Construct field info starting with a field name.
	 * @param fn
	 */
	public DrumlinFormFieldInfo ( String fn )
	{
		fFieldName = fn;
		fSteps = new LinkedList<DrumlinFormValidationStep> ();
	}

	/**
	 * validate form input for this field, given a request context, a form wrapper,
	 * and an exception to populate (which, if it has a size() > 0, will be thrown by
	 * the validator).
	 * 
	 * @param fieldName
	 * @param context
	 * @param form
	 * @param err
	 */
	public void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper form, DrumlinInvalidFormException err )
	{
		for ( DrumlinFormValidationStep step : fSteps )
		{
			step.validate ( context, form, this, err );
		}
	}

	/**
	 * Setup validation with a validation step.
	 * @param step
	 * @return this
	 */
	public DrumlinFormFieldInfo validateWith ( DrumlinFormValidationStep step )
	{
		fSteps.add ( step );
		return this;
	}

	/**
	 * Note that this field is required. The error message is used when the field
	 * is missing to populate the validation exception.
	 * 
	 * @param errMsg
	 * @return this
	 */
	public DrumlinFormFieldInfo required ( final String errMsg )
	{
		return validateWith ( new DrumlinFormValidationStep ()
		{
			@Override
			public void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper form, DrumlinFormFieldInfo field, DrumlinInvalidFormException err )
			{
				if ( !form.hasParameter ( fFieldName ) )
				{
					err.addProblem ( fFieldName, errMsg );
				}
				else
				{
					final String val = form.getValue ( fFieldName );
					if ( val != null && val.length () == 0 )
					{
						err.addProblem ( fFieldName, errMsg );
					}
				}
			}
		} );
	}

	/**
	 * Note that the field value must be one of the given values. The error message is used
	 * to populate the validation exception. This is a case-sensitive match.
	 * @param values
	 * @param errMsg
	 * @return this
	 */
	public DrumlinFormFieldInfo oneOf ( final String[] values, final String errMsg )
	{
		return oneOf ( values, true, errMsg );
	}

	/**
	 * Note that the field value must be one of the given values. The error message is used
	 * to populate the validation exception. The comparison are case sensitive based on the
	 * caseSensitive argument.
	 * @param values
	 * @param caseSensitive
	 * @param errMsg
	 * @return this
	 */
	public DrumlinFormFieldInfo oneOf ( final String[] values, final boolean caseSensitive, final String errMsg )
	{
		return validateWith ( new DrumlinFormValidationStep ()
		{
			@Override
			public void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper form, DrumlinFormFieldInfo field, DrumlinInvalidFormException err )
			{
				if ( !form.hasParameter ( fFieldName ) )
				{
					err.addProblem ( fFieldName, errMsg );
				}
				else
				{
					final String val = form.getValue ( fFieldName );

					boolean found = false;
					for ( String v : values )
					{
						if (( caseSensitive && v.equals ( val ) ) ||
							( !caseSensitive && v.equalsIgnoreCase ( val ) ) )
						{
							found = true;
							break;
						}
					}
					
					if ( !found )
					{
						err.addProblem ( fFieldName, errMsg );
					}
				}
			}
		} );
	}

	/**
	 * Note that the field value must be one of the given values. The objects are
	 * converted to strings (via toString) before the comparison, and the comparison
	 * is case sensitive.
	 * 
	 * @param values
	 * @param errMsg
	 * @return this
	 */
	public DrumlinFormFieldInfo oneOf ( final Object[] values, final String errMsg )
	{
		int current = 0;
		final String[] stringVals = new String [ values.length ];
		for ( Object o : values )
		{
			stringVals [ current++ ] = o.toString ();
		}
		return oneOf ( stringVals, errMsg );
	}

	/**
	 * Note that this field must match the given regular expression. The error message
	 * is used to populate the validation exception when the match fails.
	 * @param regex
	 * @param errMsg
	 * @return this
	 */
	public DrumlinFormFieldInfo matches ( final String regex, final String errMsg )
	{
		return validateWith ( new DrumlinFormValidationStep ()
		{
			@Override
			public void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper form, DrumlinFormFieldInfo field, DrumlinInvalidFormException err )
			{
				String value = form.getValue ( fFieldName );
				if ( value == null )
				{
					value = "";
				}
				if ( !value.matches ( regex ) )
				{
					err.addProblem ( fFieldName, errMsg );
				}
			}
		} );
	}

	/**
	 * Provide this field with a default value that's used when the form does not
	 * contain a value. Note that validation steps are run in the order they're
	 * created, so using required() before defaultValue() is probably not what
	 * you'd want.
	 * 
	 * @param defVal
	 * @return this
	 */
	public DrumlinFormFieldInfo defaultValue ( final String defVal )
	{
		return validateWith ( new DrumlinFormValidationStep ()
		{
			@Override
			public void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper form, DrumlinFormFieldInfo field, DrumlinInvalidFormException err )
			{
				if ( !form.hasParameter ( fFieldName ) ||
					( form.isFormField ( fFieldName ) && form.getValue ( fFieldName ).length () == 0 ) )
				{
					form.changeValue ( fFieldName, defVal );
				}
			}
		} );
	}

	/**
	 * Provide a default value for this field. Note that the object is converted to a string.
	 * (This is purely a convenience method.)
	 * @param o
	 * @return this
	 */
	public DrumlinFormFieldInfo defaultValue ( final Object o )
	{
		return defaultValue ( o.toString () );
	}

	/**
	 * Note the field value must be one of a set of known boolean equivalent strings:
	 * 	true/false, yes/no, on/off, 1/0, and checked.
	 * @param errMsg
	 * @return
	 */
	public DrumlinFormFieldInfo isBoolean ( final String errMsg )
	{
		return oneOf ( new String[] { "true", "false", "yes", "no", "on", "off", "1", "0", "checked" }, false, errMsg );
	}

	public final String fFieldName;
	private LinkedList<DrumlinFormValidationStep> fSteps;
}
