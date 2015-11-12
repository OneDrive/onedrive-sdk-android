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

import com.microsoft.aad.adal.AuthenticationResult;
import com.onedrive.sdk.logger.ILogger;

/**
 * Account information for an ADAL based account.
 */
public class ADALAccountInfo implements IAccountInfo {

    /**
     * The authenticator that can refresh this account.
     */
    private final ADALAuthenticator mAuthenticator;

    /**
     * The authentication result for this account.
     */
    private AuthenticationResult mAuthenticationResult;

    /**
     * The service info for OneDrive.
     */
    private final ServiceInfo mOneDriveServiceInfo;

    /**
     * The logger.
     */
    private final ILogger mLogger;

    /**
     * Creates an ADALAccountInfo object.
     * @param authenticator The authenticator that this account info was created from.
     * @param authenticationResult The authentication result for this account.
     * @param oneDriveServiceInfo The service info for OneDrive.
     * @param logger The logger
     */
    public ADALAccountInfo(final ADALAuthenticator authenticator,
                           final AuthenticationResult authenticationResult,
                           final ServiceInfo oneDriveServiceInfo,
                           final ILogger logger) {
        mAuthenticator = authenticator;
        mAuthenticationResult = authenticationResult;
        mOneDriveServiceInfo = oneDriveServiceInfo;
        mLogger = logger;
    }

    /**
     * Get the type of the account.
     * @return The ActiveDirectory account type.
     */
    @Override
    public AccountType getAccountType() {
        return AccountType.ActiveDirectory;
    }

    /**
     * Get the access token for requests against the service root.
     * @return The access token for requests against the service root.
     */
    @Override
    public String getAccessToken() {
        return mAuthenticationResult.getAccessToken();
    }

    /**
     * Get the OneDrive service root for this account.
     * @return The OneDrive service root for this account.
     */
    @Override
    public String getServiceRoot() {
        return mOneDriveServiceInfo.serviceEndpointUri;
    }

    /**
     * Determines if the access token is expired and needs to be refreshed.
     * @return true if the refresh() needs to be called and
     *         false if the account is still valid.
     */
    @Override
    public boolean isExpired() {
        return mAuthenticationResult.isExpired();
    }

    /**
     * Refreshes the access token for this Account info.
     */
    @Override
    public void refresh() {
        mLogger.logDebug("Refreshing access token...");
        final ADALAccountInfo newInfo = (ADALAccountInfo)mAuthenticator.loginSilent();
        mAuthenticationResult = newInfo.mAuthenticationResult;
    }
}
