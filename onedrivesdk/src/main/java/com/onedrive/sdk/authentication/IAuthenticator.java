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

import android.app.Activity;
import android.content.Context;

import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.http.IHttpProvider;
import com.onedrive.sdk.logger.ILogger;

/**
 * Authenticates a user interactively and silently.
 */
public interface IAuthenticator {

    /**
     * Gets the current account info for this authenticator.
     * @return NULL if no account is available.
     */
    IAccountInfo getAccountInfo();

    /**
     * Initializes the authenticator.
     * @param executors The executors to schedule foreground and background tasks.
     * @param httpProvider The http provider for sending requests.
     * @param context The context to initialize components with.
     * @param logger The logger for diagnostic information.
     */
    void init(final IExecutors executors,
              final IHttpProvider httpProvider,
              final Context context,
              final ILogger logger);

    /**
     * Starts an interactive login asynchronously.
     * @param activity The activity to create interactive UI on.
     * @param emailAddressHint The hint for the email address during the interactive login.
     * @param loginCallback The callback to be called when the login is complete.
     */
    void login(final Activity activity, final String emailAddressHint, final ICallback<IAccountInfo> loginCallback);

    /**
     * Starts an interactive login.
     * @param activity The activity to create interactive UI on.
     * @param emailAddressHint The hint for the email address during the interactive login.
     * @return The account info.
     * @throws ClientException An exception occurs if the login was unable to complete for any reason.
     */
    IAccountInfo login(final Activity activity, final String emailAddressHint) throws ClientException;

    /**
     * Starts a silent login asynchronously.
     * @param loginCallback The callback to be called when the login is complete.
     */
    void loginSilent(final ICallback<IAccountInfo> loginCallback);

    /**
     * Starts a silent login.
     * @return The account info.
     * @throws ClientException If the login was unable to complete for any reason.
     */
    IAccountInfo loginSilent() throws ClientException;

    /**
     * Log the current user out.
     * @param logoutCallback The callback to be called when the logout is complete.
     */
    void logout(final ICallback<Void> logoutCallback);

    /**
     * Log the current user out.
     * @throws ClientException Indicates if the logout was unable to complete for any reason.
     */
    void logout() throws ClientException;
}
