/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.util.standards;

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
	public static final int k429_tooManyRequests = 429;

	public static final int k500_internalServerError = 500;
	public static final int k501_notImplemented = 501;
	public static final int k502_badGateway = 502;
	public static final int k503_serviceUnavailable = 503;
	public static final int k504_gatewayTimeout = 504;
	public static final int k505_httpVersionNotSupported = 505;

	public static boolean isSuccess ( int code )
	{
		return code >= k200_ok && code < k300_multipleChoices;
	}

	public static boolean isClientFailure ( int code )
	{
		return code >= k400_badRequest && code < k500_internalServerError;
	}

	public static boolean isServerFailure ( int code )
	{
		return code >= k500_internalServerError;
	}

	public static boolean isFailure ( int code )
	{
		return isClientFailure ( code ) || isServerFailure ( code );
	}
	
	protected HttpStatusCodes()
	{
	}
}
