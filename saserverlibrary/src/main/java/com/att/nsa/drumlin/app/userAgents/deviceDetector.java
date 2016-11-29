/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/

package com.att.nsa.drumlin.app.userAgents;

import com.att.nsa.drumlin.app.userAgents.browsers.chromeBrowser;
import com.att.nsa.drumlin.app.userAgents.browsers.firefoxBrowser;
import com.att.nsa.drumlin.app.userAgents.browsers.genericBrowser;
import com.att.nsa.drumlin.app.userAgents.browsers.safariBrowser;
import com.att.nsa.drumlin.app.userAgents.devices.genericDevice;
import com.att.nsa.drumlin.app.userAgents.devices.android.androidDevice;
import com.att.nsa.drumlin.app.userAgents.devices.computers.macintosh;
import com.att.nsa.drumlin.app.userAgents.devices.ios.iPhone;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.drumlin.till.nv.rrNvReadable;

public class deviceDetector
{
	public static final String kSetting_DebugForceDevice = "drumlin.deviceDetect.force";
	
	public static userAgent detect ( DrumlinRequest r, rrNvReadable settings )
	{
		return detect ( userAgentInfo.analyze ( r ), settings );
	}

	public static userAgent detect ( userAgentInfo uaa, rrNvReadable settings )
	{
		final String forced = settings.getString ( kSetting_DebugForceDevice, null );
		if ( forced != null )
		{
			if ( forced.equalsIgnoreCase ( "genericBrowser" ) )
			{
				return new genericAgent ( new genericDevice ( false ), new genericBrowser () );
			}
			else if ( forced.equalsIgnoreCase ( "genericMobile" ) )
			{
				return new genericAgent ( new genericDevice ( true ), new genericBrowser () );
			}
		}

		// FIXME: do this in a more robust way, but for now, just get it done for some simple known devices

		if ( uaa.hasFeature ( "Mozilla" ) && uaa.getFeatureVersion ( "Mozilla", 0.0 ) >= 5.0 )
		{
			final String comment = uaa.getFeatureComment ( "Mozilla" );
			if ( comment.contains ( "iPhone" ) )
			{
				if ( uaa.hasFeature ( "CriOS" ) )
				{
					return new genericAgent ( new iPhone (), new chromeBrowser () );
				}
				else if ( uaa.hasFeature ( "Safari" ) )
				{
					return new genericAgent ( new iPhone (), new safariBrowser () );
				}
			}
			else if ( comment.contains ( "Android" ) )
			{
				return new genericAgent ( new androidDevice (), new genericBrowser () );
			}
			else if ( comment.contains ( "Macintosh" ) )
			{
				if ( uaa.hasFeature ( "Chrome" ) )
				{
					return new genericAgent ( new macintosh(), new chromeBrowser () );
				}
				else if ( uaa.hasFeature ( "Safari" ) )
				{
					return new genericAgent ( new macintosh(), new safariBrowser () );
				}
				else if ( uaa.hasFeature ( "Firefox" ) )
				{
					return new genericAgent ( new macintosh(), new firefoxBrowser () );
				}
			}
			else if ( comment.contains ( "Linux" ) )
			{
				if ( uaa.hasFeature ( "Chrome" ) )
				{
					return new genericAgent ( new genericDevice(), new chromeBrowser () );
				}
				else if ( uaa.hasFeature ( "Firefox" ) )
				{
					return new genericAgent ( new genericDevice(), new firefoxBrowser () );
				}
			}
		}

		return new genericAgent ();
	}

	// NOTES:
	//
	//	Chrome browser on iPhone 3: User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 6_0 like Mac OS X; en-us) AppleWebKit/536.26 (KHTML, like Gecko) CriOS/23.0.1271.100 Mobile/10A403 Safari/8536.25
	//	Safari browser on iPhone 3: User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 6_0 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10A403 Safari/8536.25
	//
	//	Firefox on Linux: User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:17.0) Gecko/20100101 Firefox/17.0
	//
	//	Chrome on Mac OSX: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.101 Safari/537.11
	//
	
}
