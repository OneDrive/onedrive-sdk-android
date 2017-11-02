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
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.microsoft.onedrivesdk.BuildConfig;
import com.microsoft.services.msa.LiveAuthClient;
import com.microsoft.services.msa.LiveAuthException;
import com.microsoft.services.msa.LiveAuthListener;
import com.microsoft.services.msa.LiveConnectSession;
import com.microsoft.services.msa.LiveStatus;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.concurrency.SimpleWaiter;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.http.IHttpProvider;
import com.onedrive.sdk.logger.ILogger;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper around the MSA authentication library.
 * https://github.com/MSOpenTech/msa-auth-for-android
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public abstract class MSAAuthenticator implements IAuthenticator {

    /**
     * The sign in cancellation message.
     */
    private static final String SIGN_IN_CANCELLED_MESSAGE = "The user cancelled the login operation.";

    /**
     * The preferences for this authenticator.
     */
    private static final String MSA_AUTHENTICATOR_PREFS = "MSAAuthenticatorPrefs";

    /**
     * The key for the user id.
     */
    private static final String USER_ID_KEY = "userId";

    /**
     * The key for the version code
     */
    public static final String VERSION_CODE_KEY = "versionCode";

    /**
     * The default user id
     */
    private static final String DEFAULT_USER_ID = "@@defaultUser";

    /**
     * The active user id.
     */
    private final AtomicReference<String> mUserId = new AtomicReference<>();

    /**
     * The executors.
     */
    private IExecutors mExecutors;

    /**
     * Indicates whether this authenticator has been initialized.
     */
    private boolean mInitialized;

    /**
     * The context to initialize components with.
     */
    private Context mContext;

    /**
     * The logger.
     */
    private ILogger mLogger;

    /**
     * The client id for this authenticator.
     * https://dev.onedrive.com/auth/msa_oauth.htm#to-register-your-app
     * @return The client id.
     */
    public abstract String getClientId();

    /**
     * The scopes for this application.
     * https://dev.onedrive.com/auth/msa_oauth.htm#authentication-scopes
     * @return The scopes for this application.
     */
    public abstract String[] getScopes();

    /**
     * The live authentication client.
     */
    private LiveAuthClient mAuthClient;

    /**
     * Initializes the authenticator.
     * @param executors The executors to schedule foreground and background tasks.
     * @param httpProvider The http provider for sending requests.
     * @param context The context to initialize components with.
     * @param logger The logger for diagnostic information.
     */
    @Override
    public synchronized void init(final IExecutors executors,
                     final IHttpProvider httpProvider,
                     final Context context,
                     final ILogger logger) {
        if (mInitialized) {
            return;
        }

        mExecutors = executors;
        mContext = context;
        mLogger = logger;
        mInitialized = true;
        mAuthClient = new LiveAuthClient(context, getClientId(), Arrays.asList(getScopes()));

        final SharedPreferences prefs = getSharedPreferences();
        mUserId.set(prefs.getString(USER_ID_KEY, null));
    }

    /**
     * Starts an interactive login asynchronously.
     * @param activity The activity to create interactive UI on.
     * @param emailAddressHint The hint for the email address during the interactive login.
     * @param loginCallback The callback to be called when the login is complete.
     */
    @Override
    public void login(final Activity activity, final String emailAddressHint, final ICallback<IAccountInfo> loginCallback) {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        if (loginCallback == null) {
            throw new InvalidParameterException("loginCallback");
        }

        mLogger.logDebug("Starting login async");

        mExecutors.performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    mExecutors.performOnForeground(login(activity, emailAddressHint), loginCallback);
                } catch (final ClientException e) {
                    mExecutors.performOnForeground(e, loginCallback);
                }
            }
        });
    }

    /**
     * Starts an interactive login.
     * @param activity The activity to create interactive UI on.
     * @param emailAddressHint The hint for the email address during the interactive login.
     * @return The account info.
     * @throws ClientException An exception occurs if the login was unable to complete for any reason.
     */
    @Override
    public synchronized IAccountInfo login(final Activity activity, final String emailAddressHint) throws ClientException {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        mLogger.logDebug("Starting login");

        final AtomicReference<ClientException> error = new AtomicReference<>();
        final SimpleWaiter waiter = new SimpleWaiter();

        final LiveAuthListener listener = new LiveAuthListener() {
            @Override
            public void onAuthComplete(final LiveStatus liveStatus,
                                       final LiveConnectSession liveConnectSession,
                                       final Object o) {
                if (liveStatus == LiveStatus.NOT_CONNECTED) {
                    mLogger.logDebug("Received invalid login failure from silent authentication with MSA, ignoring.");
                } else {
                    mLogger.logDebug("Successful interactive login");
                    waiter.signal();
                }
            }

            @Override
            public void onAuthError(final LiveAuthException e,
                                    final Object o) {
                OneDriveErrorCodes code = OneDriveErrorCodes.AuthenticationFailure;
                if (e.getError().equals(SIGN_IN_CANCELLED_MESSAGE)) {
                    code = OneDriveErrorCodes.AuthenticationCancelled;
                }

                error.set(new ClientAuthenticatorException("Unable to login with MSA", e, code));
                mLogger.logError(error.get().getMessage(), error.get());
                waiter.signal();
            }
        };

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mAuthClient.login(activity, /* scopes */null, /* user object */ null, emailAddressHint, listener);
            }
        });

        mLogger.logDebug("Waiting for MSA callback");
        waiter.waitForSignal();

        final ClientException exception = error.get();
        if (exception != null) {
            throw exception;
        }

        final String userId;
        if (emailAddressHint != null) {
            userId = emailAddressHint;
        } else {
            userId = DEFAULT_USER_ID;
        }

        mUserId.set(userId);

        final SharedPreferences prefs = getSharedPreferences();
        prefs.edit()
             .putString(USER_ID_KEY, mUserId.get())
             .putInt(VERSION_CODE_KEY, BuildConfig.VERSION_CODE)
             .apply();

        return getAccountInfo();
    }

    /**
     * Starts a silent login asynchronously.
     * @param loginCallback The callback to be called when the login is complete.
     */
    @Override
    public void loginSilent(final ICallback<IAccountInfo> loginCallback) {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        if (loginCallback == null) {
            throw new InvalidParameterException("loginCallback");
        }

        mLogger.logDebug("Starting login silent async");

        mExecutors.performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    mExecutors.performOnForeground(loginSilent(), loginCallback);
                } catch (final ClientException e) {
                    mExecutors.performOnForeground(e, loginCallback);
                }
            }
        });
    }

    /**
     * Starts a silent login.
     * @return The account info.
     * @throws ClientException An exception occurs if the login was unable to complete for any reason.
     */
    @Override
    public synchronized IAccountInfo loginSilent() throws ClientException {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        mLogger.logDebug("Starting login silent");

        final int userIdStoredMinVersion = 10112;
        if (getSharedPreferences().getInt(VERSION_CODE_KEY, 0) >= userIdStoredMinVersion
                && mUserId.get() == null) {
            mLogger.logDebug("No login information found for silent authentication");
            return null;
        }

        final SimpleWaiter loginSilentWaiter = new SimpleWaiter();
        final AtomicReference<ClientException> error = new AtomicReference<>();

        final boolean waitForCallback = mAuthClient.loginSilent(new LiveAuthListener() {
            @Override
            public void onAuthComplete(final LiveStatus liveStatus,
                                       final LiveConnectSession liveConnectSession,
                                       final Object o) {
                if (liveStatus == LiveStatus.NOT_CONNECTED) {
                    error.set(new ClientAuthenticatorException("Failed silent login, interactive login required",
                            OneDriveErrorCodes.AuthenticationFailure));
                    mLogger.logError(error.get().getMessage(), error.get());
                } else {
                    mLogger.logDebug("Successful silent login");
                }
                loginSilentWaiter.signal();
            }

            @Override
            public void onAuthError(final LiveAuthException e,
                                    final Object o) {
                OneDriveErrorCodes code = OneDriveErrorCodes.AuthenticationFailure;
                if (e.getError().equals(SIGN_IN_CANCELLED_MESSAGE)) {
                    code = OneDriveErrorCodes.AuthenticationCancelled;
                }

                error.set(new ClientAuthenticatorException("Login silent authentication error", e, code));
                mLogger.logError(error.get().getMessage(), error.get());
                loginSilentWaiter.signal();
            }
        });

        if (!waitForCallback) {
            mLogger.logDebug("MSA silent auth fast-failed");
            return null;
        }

        mLogger.logDebug("Waiting for MSA callback");
        loginSilentWaiter.waitForSignal();
        final ClientException exception = error.get();
        if (exception != null) {
            throw exception;
        }

        return getAccountInfo();
    }

    /**
     * Log the current user out.
     * @param logoutCallback The callback to be called when the logout is complete.
     */
    @Override
    public void logout(final ICallback<Void> logoutCallback) {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        if (logoutCallback == null) {
            throw new InvalidParameterException("logoutCallback");
        }

        mLogger.logDebug("Starting logout async");

        mExecutors.performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    logout();
                    mExecutors.performOnForeground((Void) null, logoutCallback);
                } catch (final ClientException e) {
                    mExecutors.performOnForeground(e, logoutCallback);
                }
            }
        });
    }

    /**
     * Log the current user out.
     * @throws ClientException An exception occurs if the logout was unable to complete for any reason.
     */
    @Override
    public synchronized void logout() throws ClientException {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        mLogger.logDebug("Starting logout");

        final SimpleWaiter logoutWaiter = new SimpleWaiter();
        final AtomicReference<ClientException> error = new AtomicReference<>();
        mAuthClient.logout(new LiveAuthListener() {
            @Override
            public void onAuthComplete(final LiveStatus liveStatus,
                                       final LiveConnectSession liveConnectSession,
                                       final Object o) {
                mLogger.logDebug("Logout completed");
                logoutWaiter.signal();
            }

            @Override
            public void onAuthError(final LiveAuthException e, final Object o) {
                error.set(new ClientAuthenticatorException("MSA Logout failed",
                                                           e,
                                                           OneDriveErrorCodes.AuthenticationFailure));
                mLogger.logError(error.get().getMessage(), error.get());
                logoutWaiter.signal();
            }
        });

        mLogger.logDebug("Waiting for logout to complete");
        logoutWaiter.waitForSignal();

        mLogger.logDebug("Clearing all MSA Authenticator shared preferences");
        final SharedPreferences prefs = getSharedPreferences();
        prefs.edit()
             .clear()
             .putInt(VERSION_CODE_KEY, BuildConfig.VERSION_CODE)
             .apply();
        mUserId.set(null);

        final ClientException exception = error.get();
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Gets the current account info for this authenticator.
     * @return NULL if no account is available.
     */
    @Override
    public IAccountInfo getAccountInfo() {
        final LiveConnectSession session = mAuthClient.getSession();
        if (session == null) {
            return null;
        }

        return new MSAAccountInfo(this, session, mLogger);
    }

    /**
     * Gets the shared preferences for this authenticator.
     * @return The shared preferences.
     */
    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(MSA_AUTHENTICATOR_PREFS, Context.MODE_PRIVATE);
    }

}
