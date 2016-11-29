/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer.util;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.drumlin.till.nv.rrNvReadable;

/**
 * Send an email from a message.
 * 
 * @author peter
 */
public class Emailer
{
	public static final String kField_To = "to";
	public static final String kField_Subject = "subject";
	public static final String kField_Message = "message";

	public Emailer ( rrNvReadable settings )
	{
		fExec = Executors.newCachedThreadPool ();
		fSettings = settings;
	}

	public void send ( String to, String subj, String body ) throws IOException
	{
		final String[] addrs = to.split ( "," );

		if ( to.length () > 0 )
		{
			final MailTask mt = new MailTask ( addrs, subj, body );
			fExec.submit ( mt );
		}
		else
		{
			log.warn ( "At least one address is required." );
		}
	}

	public void close ()
	{
		fExec.shutdown ();
	}

	private final ExecutorService fExec;
	private final rrNvReadable fSettings;

	private static final Logger log = LoggerFactory.getLogger ( Emailer.class );
	
	public static final String kSetting_MailAuthUser = "mailLogin";
	public static final String kSetting_MailAuthPwd = "mailPassword";
	public static final String kSetting_MailFromEmail = "mailFromEmail";
	public static final String kSetting_MailFromName = "mailFromName";
	public static final String kSetting_SmtpServer = "mailSmtpServer";
	public static final String kSetting_SmtpServerPort = "mailSmtpServerPort";
	public static final String kSetting_SmtpServerSsl = "mailSmtpServerSsl";
	public static final String kSetting_SmtpServerUseAuth = "mailSmtpServerUseAuth";

	private class MailTask implements Runnable
	{
		public MailTask ( String[] to, String subject, String msgBody )
		{
			fToAddrs = to;
			fSubject = subject;
			fBody = msgBody;
		}

		private String getSetting ( String settingKey, String defval )
		{
			return fSettings.getString ( settingKey, defval );
		}

		// we need to get setting values from the evaluator but also the channel config
		private void makeSetting ( Properties props, String propKey, String settingKey, String defval )
		{
			props.put ( propKey, getSetting ( settingKey, defval ) );
		}

		private void makeSetting ( Properties props, String propKey, String settingKey, int defval )
		{
			makeSetting ( props, propKey, settingKey, "" + defval );
		}

		private void makeSetting ( Properties props, String propKey, String settingKey, boolean defval )
		{
			makeSetting ( props, propKey, settingKey, "" + defval );
		}

		@Override
		public void run ()
		{
			final StringBuffer tag = new StringBuffer ();
			final StringBuffer addrList = new StringBuffer ();
			tag.append ( "(" );
			for ( String to : fToAddrs )
			{
				if ( addrList.length () > 0 )
				{
					addrList.append ( ", " );
				}
				addrList.append ( to );
			}
			tag.append ( addrList.toString () );
			tag.append ( ") \"" );
			tag.append ( fSubject );
			tag.append ( "\"" );
			
			log.info ( "sending mail to " + tag );

			try
			{
				final Properties prop = new Properties ();
				makeSetting ( prop, "mail.smtp.port", kSetting_SmtpServerPort, 587 );
				prop.put ( "mail.smtp.socketFactory.fallback", "false" );
				prop.put ( "mail.smtp.quitwait", "false" );
				makeSetting ( prop, "mail.smtp.host", kSetting_SmtpServer, "smtp.it.att.com" );
				makeSetting ( prop, "mail.smtp.auth", kSetting_SmtpServerUseAuth, true );
				makeSetting ( prop, "mail.smtp.starttls.enable", kSetting_SmtpServerSsl, true );

				final String un = getSetting ( kSetting_MailAuthUser, "" );
				final String pw = getSetting ( kSetting_MailAuthPwd, "" );
				final Session session = Session.getInstance ( prop,
					new javax.mail.Authenticator()
					{
						@Override
						protected PasswordAuthentication getPasswordAuthentication()
						{
							return new PasswordAuthentication ( un, pw );
						}
					}
				);
				
				final Message msg = new MimeMessage ( session );

				final InternetAddress from = new InternetAddress (
					getSetting ( kSetting_MailFromEmail, "team@sa2020.it.att.com" ),
					getSetting ( kSetting_MailFromName, "The GFP/SA2020 Team" ) );
				msg.setFrom ( from );
				msg.setReplyTo ( new InternetAddress[] { from } );
				msg.setSubject ( fSubject );

				for ( String toAddr : fToAddrs )
				{
					final InternetAddress to = new InternetAddress ( toAddr );
					msg.addRecipient ( Message.RecipientType.TO, to );
				}

				final Multipart multipart = new MimeMultipart ( "related" );
				final BodyPart htmlPart = new MimeBodyPart ();
				htmlPart.setContent ( fBody, "text/plain" );
				multipart.addBodyPart ( htmlPart );
				msg.setContent ( multipart );

				Transport.send ( msg );

				log.info ( "mailing " + tag + " off without error" );
			}
			catch ( Exception e )
			{
				log.warn ( "Exception caught for " + tag, e );
			}
		}

		private final String[] fToAddrs;
		private final String fSubject;
		private final String fBody;
	}
}
