/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.till.nv;

import java.util.Collection;
import java.util.Map;

/**
 * A data supplier
 */
public interface rrNvReadable
{
	class loadException extends Exception
	{
		public loadException ( Throwable cause ) { super(cause); }
		private static final long serialVersionUID = 1L;
	}

	class missingReqdSetting extends Exception
	{
		public missingReqdSetting ( String key ) { super("Missing required setting \"" + key + "\"" ); fKey = key; }
		public missingReqdSetting ( String key, Throwable cause ) { super("Missing required setting \"" + key + "\" because " + cause.getMessage (), cause ); fKey=key; }
		private static final long serialVersionUID = 1L;
		public final String fKey;
	}

	class invalidSettingValue extends Exception
	{
		public invalidSettingValue ( String key ) { super("Invalid setting for \"" + key + "\"" ); fKey=key; }
		public invalidSettingValue ( String key, Throwable cause ) { super("Invalid setting for \"" + key + "\" because " + cause.getMessage (), cause ); fKey=key; }
		public invalidSettingValue ( String key, String why ) { super("Invalid setting for \"" + key + "\" because " + why ); fKey=key; }
		public invalidSettingValue ( String key, Throwable cause, String why ) { super("Invalid setting for \"" + key + "\" because " + why, cause ); fKey=key; }
		private static final long serialVersionUID = 1L;
		public final String fKey;
	}

	String getString ( String key ) throws missingReqdSetting;
	String getString ( String key, String defValue );

	char getCharacter ( String key ) throws missingReqdSetting;
	char getCharacter ( String key, char defValue );

	boolean getBoolean ( String key ) throws missingReqdSetting;
	boolean getBoolean ( String key, boolean defValue );

	int getInt ( String key ) throws missingReqdSetting;
	int getInt ( String key, int defValue );

	long getLong ( String key ) throws missingReqdSetting;
	long getLong ( String key, long defValue );

	double getDouble ( String key ) throws missingReqdSetting;
	double getDouble ( String key, double defValue );

	byte[] getBytes ( String key ) throws missingReqdSetting, invalidSettingValue;
	byte[] getBytes ( String key, byte[] defValue );

	/**
	 * Get a set of strings given a key. Most implementations expect to use "getString()" and then
	 * split the value by commas.
	 *  
	 * @param key
	 * @return a string array
	 * @throws missingReqdSetting
	 */
	String[] getStrings ( String key ) throws missingReqdSetting;
	String[] getStrings ( String key, String[] defValue );

	int size ();
	boolean hasValueFor ( String key );
	Collection<String> getAllKeys ();
	Map<String, String> getCopyAsMap ();

	void copyInto ( rrNvWriteable writeable );
	void copyInto ( Map<String,String> writeable );

	void rescan () throws loadException;
}
