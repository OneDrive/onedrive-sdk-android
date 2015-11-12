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

import com.onedrive.sdk.authentication.IAuthenticator;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.http.IHttpProvider;
import com.onedrive.sdk.logger.ILogger;
import com.onedrive.sdk.serializer.ISerializer;

/**
 * A client that communications with an OData service.
 */
public class BaseClient implements IBaseClient {

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
    private IHttpProvider mHttpProvider;

    /**
     * The logger.
     */
    private ILogger mLogger;

    /**
     * The serializer instance.
     */
    private ISerializer mSerializer;

    /**
     * Gets the authenticator.
     * @return The authenticator.
     */
    @Override
    public IAuthenticator getAuthenticator() {
        return mAuthenticator;
    }

    /**
     * Gets the service root.
     * @return The service root.
     */
    @Override
    public String getServiceRoot() {
        return getAuthenticator().getAccountInfo().getServiceRoot();
    }

    /**
     * Gets the executors.
     * @return The executors.
     */
    @Override
    public IExecutors getExecutors() {
        return mExecutors;
    }

    /**
     * Gets the http provider.
     * @return The http provider.
     */
    @Override
    public IHttpProvider getHttpProvider() {
        return mHttpProvider;
    }

    /**
     * Gets the logger.
     * @return The logger.
     */
    public ILogger getLogger() {
        return mLogger;
    }

    /**
     * Gets the serializer.
     * @return The serializer.
     */
    @Override
    public ISerializer getSerializer() {
        return mSerializer;
    }

    /**
     * Validates this client.
     */
    @Override
    public void validate() {
        if (mAuthenticator == null) {
            throw new NullPointerException("Authenticator");
        }

        if (mExecutors == null) {
            throw new NullPointerException("Executors");
        }

        if (mHttpProvider == null) {
            throw new NullPointerException("HttpProvider");
        }

        if (mSerializer == null) {
            throw new NullPointerException("Serializer");
        }
    }

    /**
     * Sets the logger.
     * @param logger The logger.
     */
    protected void setLogger(final ILogger logger) {
        mLogger = logger;
    }

    /**
     * Sets the executors.
     * @param executors The executors.
     */
    protected void setExecutors(final IExecutors executors) {
        mExecutors = executors;
    }

    /**
     * Sets the authenticator.
     * @param authenticator The authenticator.
     */
    protected void setAuthenticator(final IAuthenticator authenticator) {
        mAuthenticator = authenticator;
    }

    /**
     * Sets the http provider.
     * @param httpProvider The http provider.
     */
    protected void setHttpProvider(final IHttpProvider httpProvider) {
        mHttpProvider = httpProvider;
    }

    /**
     * Sets the serializer.
     * @param serializer The serializer.
     */
    public void setSerializer(final ISerializer serializer) {
        mSerializer = serializer;
    }
}
