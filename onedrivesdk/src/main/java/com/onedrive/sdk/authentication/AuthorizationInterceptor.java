// ------------------------------------------------------------------------------
// Copyright (c) 2015 Microsoft Corporation
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
// ------------------------------------------------------------------------------

package com.onedrive.sdk.authentication;

import com.onedrive.sdk.http.IHttpRequest;
import com.onedrive.sdk.http.IRequestInterceptor;
import com.onedrive.sdk.logger.ILogger;
import com.onedrive.sdk.options.HeaderOption;

/**
 * Intercepts a request and adds authorization headers.
 */
public class AuthorizationInterceptor implements IRequestInterceptor {

    /**
     * The authorization header name.
     */
    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    /**
     * The bearer prefix.
     */
    public static final String OAUTH_BEARER_PREFIX = "bearer ";

    /**
     * The authenticator.
     */
    private final IAuthenticator mAuthenticator;

    /**
     * The logger.
     */
    private final ILogger mLogger;

    /**
     * Creates the authorization interceptor.
     * @param authenticator The authenticator.
     * @param logger The logger.
     */
    public AuthorizationInterceptor(final IAuthenticator authenticator, final ILogger logger) {
        mAuthenticator = authenticator;
        mLogger = logger;
    }

    /**
     * Intercepts the request.
     * @param request The request to intercept.
     */
    @Override
    public void intercept(final IHttpRequest request) {
        mLogger.logDebug("Intercepting request, " + request.getRequestUrl());

        // If the request already has an authorization header, do not intercept it.
        for (final HeaderOption option : request.getHeaders()) {
            if (option.getName().equals(AUTHORIZATION_HEADER_NAME)) {
                mLogger.logDebug("Found an existing authorization header!");
                return;
            }
        }

        if (mAuthenticator.getAccountInfo() != null) {
            mLogger.logDebug("Found account information");
            if (mAuthenticator.getAccountInfo().isExpired()) {
                mLogger.logDebug("Account access token is expired, refreshing");
                mAuthenticator.getAccountInfo().refresh();
            }

            final String accessToken = mAuthenticator.getAccountInfo().getAccessToken();
            request.addHeader(AUTHORIZATION_HEADER_NAME, OAUTH_BEARER_PREFIX + accessToken);
        } else {
            mLogger.logDebug("No active account found, skipping writing auth header");
        }
    }
}
