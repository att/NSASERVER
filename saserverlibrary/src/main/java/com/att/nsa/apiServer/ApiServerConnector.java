/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.apiServer;

public class ApiServerConnector {

	private final boolean isSecure;
	private final int port;
	private final int maxThreads;
	private final String keystorePassword;
	private final String keystoreFile;
	private final String keyAlias;
	private final int socketTimeoutMs;
	private final int keepAliveTimeoutMs;

	public static class Builder {
		
		private final int port;
		
		private boolean secure = false;
		private String keystorePassword = null;
		private String keystoreFile = null;
		private String keyAlias = null;
		private int maxThreads = -1;
		private int socketTimeoutMs = 1000 * 5;
		private int keepAliveTimeoutMs = 1000 * 60 * 2;
		
		public Builder(int port) {
			this.port = port;
		}
		
		public ApiServerConnector build() {

			if (secure) {
				if (keystoreFile == null || keystoreFile.length() == 0) {
					throw new IllegalArgumentException("Secure port " + port + " was specified, but no keystoreFile. Fix secure service configuration");
				} else if (keystorePassword == null || keystorePassword.length() == 0) {
					throw new IllegalArgumentException("Secure port " + port + " was specified, but no keystorePassword. Fix secure service configuration");
				}
			}
			
			return new ApiServerConnector(this);
		}
		
		public Builder secure(boolean secure) {
			this.secure = secure;
			return this;
		}
		
		public Builder maxThreads(int maxThreads) {
			this.maxThreads = maxThreads;
			return this;
		}
		
		public Builder keystorePassword(String keystorePassword) {
			this.keystorePassword = keystorePassword;
			return this;
		}
		
		public Builder keystoreFile(String keystoreFile) {
			this.keystoreFile = keystoreFile;
			return this;
		}
		
		public Builder keyAlias(String keyAlias) {
			this.keyAlias = keyAlias;
			return this;
		}

		// timeouts: The AT&T CSO team believes that a long time window between socket connect
		// and byte send makes DoS attacks more effective. (If you're thinking "that doesn't
		// make any sense", you're correct.) However, the vulnerability scanner they run flags
		// tomcat sockets running at default timeouts causing email from people responsible
		// for closing the vulnerability flag regardless of its making any sense. So, we
		// default the connectionTimeout (time between connect and byte send) to 5 seconds unless
		// someone sets it explicitly via builder. That same value is used for the keep alive
		// timeout (HTTP 1.1) unless explicitly set, and 5 seconds is very short for a long-polling
		// cambria client (for example), so we jack that back up to a fairly high value by default.

		public Builder connectionTimingOutAfter ( int timeoutMs )
		{
			this.socketTimeoutMs = timeoutMs;
			return this;
		}
		
		public Builder keepAliveTimingOutAfter ( int timeoutMs )
		{
			this.keepAliveTimeoutMs = timeoutMs;
			return this;
		}

		private boolean isSecure() { return secure; }
		private int port() { return port; }
		private String keystorePassword() { return keystorePassword; }
		private String keystoreFile() { return keystoreFile; }
		private String keyAlias() { return keyAlias; }
		private int maxThreads() { return maxThreads; }
		private int socketTimeoutMs() { return socketTimeoutMs; }
		private int keepAliveTimeoutMs() { return keepAliveTimeoutMs; }
	}
	
	private ApiServerConnector(Builder builder) {
		this.isSecure = builder.isSecure();
		this.port = builder.port();
		this.keystorePassword = builder.keystorePassword();
		this.keystoreFile = builder.keystoreFile();
		this.keyAlias = builder.keyAlias();
		this.maxThreads = builder.maxThreads();
		this.socketTimeoutMs = builder.socketTimeoutMs ();
		this.keepAliveTimeoutMs = builder.keepAliveTimeoutMs ();
	}
	
	public boolean isSecure() { return isSecure; }
	public int port() { return port; }
	public int maxThreads() { return maxThreads; }
	public String keystorePassword() { return keystorePassword; }
	public String keystoreFile() { return keystoreFile; }
	public String keyAlias() { return keyAlias; }

	public int socketTimeoutMs() { return socketTimeoutMs; }
	public int keepAliveTimeoutMs() { return keepAliveTimeoutMs; }
}
