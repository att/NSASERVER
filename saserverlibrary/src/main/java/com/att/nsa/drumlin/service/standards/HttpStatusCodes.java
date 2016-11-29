/*******************************************************************************
 * Copyright (c) 2016 AT&T Intellectual Property. All rights reserved.
 *******************************************************************************/
package com.att.nsa.drumlin.service.standards;

public class HttpStatusCodes
{
	public static final int k100_continue = 100;
	public static final int k101_switchingProtocols = 101;

	public static final int k200_ok = 200;
	public static final int k201_created = 201;
	public static final int k202_accepted = 202;
	public static final int k203_nonAuthoritativeInformation = 203;
	public static final int k204_noContent = 204;		// HTTP/1.1: "MUST NOT include a message-body"
	public static final int k205_resetContent = 205;	// HTTP/1.1: "MUST NOT include an entity"
	public static final int k206_partialContent = 206;

	public static final int k300_multipleChoices = 300;
	public static final int k301_movedPermanently = 301;
	public static final int k302_found = 302;
	public static final int k303_seeOther = 303;
	public static final int k304_notModified = 304;
	public static final int k305_useProxy = 305;
	public static final int k307_temporaryRedirect = 307;

	public static final int k400_badRequest = 400;
	public static final int k401_unauthorized = 401;
	public static final int k402_paymentRequired = 402;
	public static final int k403_forbidden = 403;
	public static final int k404_notFound = 404;
	public static final int k405_methodNotAllowed = 405;
	public static final int k406_notAcceptable = 406;
	public static final int k407_proxyAuthReqd = 407;
	public static final int k408_requestTimeout = 408;
	public static final int k409_conflict = 409;
	public static final int k410_gone = 410;
	public static final int k411_lengthRequired = 411;
	public static final int k412_preconditionFailed = 412;
	public static final int k413_requestEntityTooLarge = 413;
	public static final int k414_requestUriTooLong = 414;
	public static final int k415_unsupportedMediaType = 415;
	public static final int k416_requestedRangeNotSatisfiable = 416;
	public static final int k417_expectationFailed = 417;
	public static final int k428_preconditionRequired = 428;
	public static final int k429_tooManyRequests = 429;
	public static final int k431_requestHeaderFieldsTooLarge = 431;

	public static final int k500_internalServerError = 500;
	public static final int k501_notImplemented = 501;
	public static final int k502_badGateway = 502;
	public static final int k503_serviceUnavailable = 503;
	public static final int k504_gatewayTimeout = 504;
	public static final int k505_httpVersionNotSupported = 505;
	public static final int k511_networkAuthenticationRequired = 511;
}
