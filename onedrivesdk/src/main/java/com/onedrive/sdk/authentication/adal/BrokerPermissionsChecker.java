// ------------------------------------------------------------------------------
// Copyright (c) 2016 Microsoft Corporation
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

package com.onedrive.sdk.authentication.adal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import com.microsoft.aad.adal.AuthenticationSettings;
import com.onedrive.sdk.authentication.ClientAuthenticatorException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.logger.ILogger;

/**
 * Checks if the ADAL broker has the required permissions to be used
 */
public class BrokerPermissionsChecker {

    /**
     * The url to the ADAL project for reference
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String mAdalProjectUrl = "https://github.com/AzureAD/azure-activedirectory-library-for-android";

    /**
     * The permissions need to use a the account broker with ADAL
     */
    private final String[] mBrokerRequirePermissions = new String[] {
            "android.permission.GET_ACCOUNTS",
            "android.permission.MANAGE_ACCOUNTS",
            "android.permission.USE_CREDENTIALS"
    };

    /**
     * The current context
     */
    private final Context mContext;

    /**
     * The logger to use
     */
    private final ILogger mLogger;

    /**
     * Creates a BrokerPermissionsChecker
     * @param context The current context to check permissions against
     * @param logger The logger context
     */
    public BrokerPermissionsChecker(final Context context, final ILogger logger) {
        mContext = context;
        mLogger = logger;
    }

    /**
     * Checks if the Broker has the permissions needed be used.
     *
     * @throws ClientAuthenticatorException If the required permissions are not available
     */
    public void check() throws ClientAuthenticatorException {
        if (!AuthenticationSettings.INSTANCE.getSkipBroker()) {
            mLogger.logDebug("Checking permissions for use with the ADAL Broker.");
            for (final String permission : mBrokerRequirePermissions) {
                if (ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_DENIED) {
                    final String message = String.format(
                            "Required permissions to use the Broker are denied: %s, see %s for more details.",
                            permission,
                            mAdalProjectUrl);
                    mLogger.logDebug(message);
                    throw new ClientAuthenticatorException(message, OneDriveErrorCodes.AuthenicationPermissionsDenied);
                }
            }
            mLogger.logDebug("All required permissions found.");
        }
    }
}
