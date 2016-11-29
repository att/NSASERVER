/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms;

import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;

/**
 * A form validation step.
 * @author peter@rathravane.com
 *
 */
public interface DrumlinFormValidationStep
{
	/**
	 * <p>Given a context, form, and field information, decide if the value is valid. If it is not,
	 * add an error listing for the field to the supplied validation error. (Once all
	 * validation steps are complete, if the error object contains any errors, it's thrown as
	 * an exception, indicating a problem with the form submission.)</p>
	 * 
	 * <p>For form-level validation, the field argument is null.</p>
	 * 
	 * @param context
	 * @param form
	 * @param field Field info, or null for form-level validation.
	 * @param err
	 */
	void validate ( DrumlinRequestContext context, DrumlinFormPostWrapper form, DrumlinFormFieldInfo field, DrumlinInvalidFormException err );
}
