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

package com.onedrive.sdk.core;

import com.onedrive.sdk.authentication.ADALAuthenticator;
import com.onedrive.sdk.authentication.AuthorizationInterceptor;
import com.onedrive.sdk.authentication.DisambiguationAuthenticator;
import com.onedrive.sdk.authentication.IAuthenticator;
import com.onedrive.sdk.authentication.MSAAuthenticator;
import com.onedrive.sdk.concurrency.DefaultExecutors;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.http.DefaultHttpProvider;
import com.onedrive.sdk.http.IHttpProvider;
import com.onedrive.sdk.http.IRequestInterceptor;
import com.onedrive.sdk.logger.DefaultLogger;
import com.onedrive.sdk.logger.ILogger;
import com.onedrive.sdk.serializer.DefaultSerializer;
import com.onedrive.sdk.serializer.ISerializer;

/**
 * The default configuration for a OneDrive client.
 */
public abstract class DefaultClientConfig implements IClientConfig {

    /**
     * The authenticator instance.
     */
    private IAuthenticator mAuthenticator;

    /**
     * The executors instance.
     */
    private IExecutors mExecutors;

    /**
     * The http provider instance.
     */
    private DefaultHttpProvider mHttpProvider;

    /**
     * The logger.
     */
    private ILogger mLogger;

    /**
     * The serializer instance.
     */
    private DefaultSerializer mSerializer;

    /**
     * The request interceptor.
     */
    private IRequestInterceptor mRequestInterceptor;

    /**
     * Creates an instance of this OneDrive config with an authenticator.
     * @param authenticator The authenticator.
     * @return The OneDriveConfiguration.
     */
    public static IClientConfig createWithAuthenticator(final IAuthenticator authenticator) {
        DefaultClientConfig config = new DefaultClientConfig() { };
        config.mAuthenticator = authenticator;
        config.getLogger().logDebug("Using provided authenticator");
        return config;
    }

    /**
     * Creates an instance of this OneDrive config that can disambiguate between MSA and ADAL accounts.
     * @param msaAuthenticator The MSA authenticator.
     * @param adalAuthenticator The ADAL authenticator.
     * @return The OneDriveConfiguration.
     */
    public static IClientConfig createWithAuthenticators(
            final MSAAuthenticator msaAuthenticator,
            final ADALAuthenticator adalAuthenticator) {
        DefaultClientConfig config = new DefaultClientConfig() { };
        config.mAuthenticator = new DisambiguationAuthenticator(msaAuthenticator, adalAuthenticator);
        config.getLogger().logDebug("Created DisambiguationAuthenticator");
        return config;
    }

    /**
     * Gets the authenticator.
     * @return The authenticator.
     */
    @Override
    public IAuthenticator getAuthenticator() {
        return mAuthenticator;
    }

    /**
     * Gets the http provider.
     * @return The http provider.
     */
    @Override
    public IHttpProvider getHttpProvider() {
        if (mHttpProvider == null) {
            mHttpProvider = new DefaultHttpProvider(getSerializer(),
                                                    getRequestInterceptor(),
                                                    getExecutors(),
                                                    getLogger());
            mLogger.logDebug("Created DefaultHttpProvider");
        }
        return mHttpProvider;
    }

    /**
     * Gets the serializer.
     * @return The serializer.
     */
    @Override
    public ISerializer getSerializer() {
        if (mSerializer == null) {
            mSerializer = new DefaultSerializer(getLogger());
            mLogger.logDebug("Created DefaultSerializer");
        }
        return mSerializer;
    }

    /**
     * Gets the executors.
     * @return The executors.
     */
    @Override
    public IExecutors getExecutors() {
        if (mExecutors == null) {
            mExecutors = new DefaultExecutors(getLogger());
            mLogger.logDebug("Created DefaultExecutors");
        }
        return mExecutors;
    }

    /**
     * Gets the logger.
     * @return The logger.
     */
    public ILogger getLogger() {
        if (mLogger == null) {
            mLogger = new DefaultLogger();
            mLogger.logDebug("Created DefaultLogger");
        }
        return mLogger;
    }

    /**
     * Gets the request interceptor.
     * @return The request interceptor.
     */
    private IRequestInterceptor getRequestInterceptor() {
        if (mRequestInterceptor == null) {
            mRequestInterceptor = new AuthorizationInterceptor(getAuthenticator(), getLogger());
        }
        return mRequestInterceptor;
    }
}
