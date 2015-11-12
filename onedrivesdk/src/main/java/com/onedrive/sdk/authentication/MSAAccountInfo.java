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

import com.microsoft.services.msa.LiveConnectSession;
import com.onedrive.sdk.logger.ILogger;

/**
 * Account information for a MSA based account.
 */
public class MSAAccountInfo implements IAccountInfo {

    /**
     * The service root for the OneDrive personal API.
     */
    public static final String ONE_DRIVE_PERSONAL_SERVICE_ROOT = "https://api.onedrive.com/v1.0";

    /**
     * The authenticator that can refresh this account.
     */
    private final MSAAuthenticator mAuthenticator;

    /**
     * The session this account is based off of.
     */
    private LiveConnectSession mSession;

    /**
     * The logger.
     */
    private final ILogger mLogger;

    /**
     * Creates an MSAAccountInfo object.
     * @param authenticator The authenticator that this account info was created from.
     * @param liveConnectSession The session this account is based off of.
     * @param logger The logger.
     */
    public MSAAccountInfo(final MSAAuthenticator authenticator,
                          final LiveConnectSession liveConnectSession,
                          final ILogger logger) {
        mAuthenticator = authenticator;
        mSession = liveConnectSession;
        mLogger = logger;
    }

    /**
     * Get the type of the account.
     * @return The MicrosoftAccount account type.
     */
    @Override
    public AccountType getAccountType() {
        return AccountType.MicrosoftAccount;
    }

    /**
     * Get the access token for requests against the service root.
     * @return The access token for requests against the service root.
     */
    @Override
    public String getAccessToken() {
        return mSession.getAccessToken();
    }

    /**
     * Get the OneDrive service root for this account.
     * @return the OneDrive service root for this account.
     */
    @Override
    public String getServiceRoot() {
        return ONE_DRIVE_PERSONAL_SERVICE_ROOT;
    }

    /**
     * Indicates if the account access token is expired and needs to be refreshed.
     * @return true if refresh() needs to be called and
     *         false if the account is still valid.
     */
    @Override
    public boolean isExpired() {
        return mSession.isExpired();
    }

    /**
     * Refreshes the authentication token for this account info.
     */
    @Override
    public void refresh() {
        mLogger.logDebug("Refreshing access token...");
        final MSAAccountInfo newInfo = (MSAAccountInfo)mAuthenticator.loginSilent();
        mSession = newInfo.mSession;
    }
}
