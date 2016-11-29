/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.app.htmlForms;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.att.nsa.drumlin.till.collections.rrMultiMap;

/**
 * An exception signalling that a form is invalid. It carries specific problems
 * (and warnings) by field name, and separately for the overall form. It's intended
 * to be caught and used to report problems back to the user.
 * 
 * @author peter@rathravane.com
 */
public class DrumlinInvalidFormException extends Exception
{
	public DrumlinInvalidFormException ()
	{
		super ( "Form validation errors" );
		fFieldProblems = new rrMultiMap<String,String> ();
		fFieldWarnings = new rrMultiMap<String,String> (); 
		fFormProblems = new LinkedList<String> (); 
	}

	/**
	 * Add a general problem.
	 * @param problem
	 */
	public void addProblem ( String problem )
	{
		fFormProblems.add ( problem );
	}

	/**
	 * Add a problem with a specific field.
	 * @param field
	 * @param problem
	 */
	public void addProblem ( String field, String problem )
	{
		fFieldProblems.put ( field, problem );
	}

	/**
	 * Add a warning for a specific field.
	 * @param field
	 * @param problem
	 */
	public void addWarning ( String field, String problem )
	{
		fFieldWarnings.put ( field, problem );
	}

	/**
	 * Copy problems from another invalid form exception.
	 * @param that
	 */
	public void addProblemsFrom ( DrumlinInvalidFormException that )
	{
		fFormProblems.addAll ( that.getFormProblems () );
		fFieldProblems.putAll ( that.getFieldProblems () );
	}

	/**
	 * Get the count of problems.
	 * @return the count of problems.
	 */
	public int size ()
	{
		return fFieldProblems.size () + fFormProblems.size ();
	}

	/**
	 * Get all field problems.
	 * @return a map from field name to a list of problem strings
	 */
	public Map<String,List<String>> getFieldProblems ()
	{
		return fFieldProblems.getValues ();
	}

	/**
	 * Get problems on a particular field.
	 * @param field
	 * @return a list of 0 or more problems.
	 */
	public List<String> getProblemsOn ( String field )
	{
		final LinkedList<String> list = new LinkedList<String> ();
		final List<String> vals = fFieldProblems.get ( field );
		if ( vals != null )
		{
			list.addAll ( vals );
		}
		return list;
	}
	
	/**
	 * Get all field warnings.
	 * @return a map from field name to a list of warning strings
	 */
	public Map<String,List<String>> getFieldWarnings ()
	{
		return fFieldWarnings.getValues ();
	}

	/**
	 * Get the form-level problems.
	 * @return a list of form problems
	 */
	public List<String> getFormProblems ()
	{
		return fFormProblems;
	}

	private final LinkedList<String> fFormProblems; 
	private rrMultiMap<String,String> fFieldProblems;
	private rrMultiMap<String,String> fFieldWarnings;
	private static final long serialVersionUID = 1L;
}
