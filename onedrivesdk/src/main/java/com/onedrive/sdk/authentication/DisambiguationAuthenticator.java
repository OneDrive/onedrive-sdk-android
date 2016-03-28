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

import com.microsoft.onedrivesdk.BuildConfig;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.concurrency.SimpleWaiter;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.http.IHttpProvider;
import com.onedrive.sdk.logger.ILogger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.security.InvalidParameterException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper around the ADAL and MSA authenticators which can disambiguate between them.
 */
public class DisambiguationAuthenticator implements IAuthenticator {

    /**
     * The preferences for this authenticator.
     */
    private static final String DISAMBIGUATION_AUTHENTICATOR_PREFS = "DisambiguationAuthenticatorPrefs";

    /**
     * The key for the version code
     */
    public static final String VERSION_CODE_KEY = "versionCode";

    /**
     * The key for the account type
     */
    public static final String ACCOUNT_TYPE_KEY = "accountType";

    /**
     * The current account info object.
     */
    private final AtomicReference<IAccountInfo> mAccountInfo = new AtomicReference<>();

    /**
     * The MSA Authenticator.
     */
    private final MSAAuthenticator mMSAAuthenticator;

    /**
     * The ADAL Authenticator.
     */
    private final ADALAuthenticator mADALAuthenticator;

    /**
     * The executors.
     */
    private IExecutors mExecutors;

    /**
     * The context UI interactions should happen with.
     */
    private Activity mActivity;

    /**
     * Indicates if this authenticator has been initialized.
     */
    private boolean mInitialized;

    /**
     * The logger.
     */
    private ILogger mLogger;

    /**
     * Creates a disambiguation authenticator.
     * @param msaAuthenticator The MSA Authenticator.
     * @param adalAuthenticator The ADAL Authenticator.
     */
    public DisambiguationAuthenticator(final MSAAuthenticator msaAuthenticator,
                                       final ADALAuthenticator adalAuthenticator)
    {
        mMSAAuthenticator = msaAuthenticator;
        mADALAuthenticator = adalAuthenticator;
    }

    /**
     * Initializes the authenticator.
     * @param executors The executors to schedule foreground and background tasks.
     * @param httpProvider The http provider for sending requests.
     * @param activity The activity to create interactive UI on.
     * @param logger The logger for diagnostic information.
     */
    @Override
    public synchronized void init(final IExecutors executors,
                                  final IHttpProvider httpProvider,
                                  final Activity activity,
                                  final ILogger logger) {
        if (mInitialized) {
            return;
        }

        mExecutors = executors;
        mActivity = activity;
        mLogger = logger;
        mLogger.logDebug("Initializing MSA and ADAL authenticators");
        mMSAAuthenticator.init(executors, httpProvider, activity, logger);
        mADALAuthenticator.init(executors, httpProvider, activity, logger);
        mInitialized = true;
    }

    /**
     * Starts an interactive login asynchronously.
     * @param emailAddressHint The hint for the email address during the interactive login.
     * @param loginCallback The callback to be called when the login is complete.
     */
    @Override
    public void login(final String emailAddressHint, final ICallback<IAccountInfo> loginCallback) {
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
                    mExecutors.performOnForeground(login(emailAddressHint), loginCallback);
                } catch (final ClientException e) {
                    mExecutors.performOnForeground(e, loginCallback);
                }
            }
        });
    }

    /**
     * Starts an interactive login.
     * @param emailAddressHint The hint for the email address during the interactive login.
     * @return The account info.
     * @throws ClientException If the login was unable to complete for any reason.
     */
    @Override
    public synchronized IAccountInfo login(final String emailAddressHint) throws ClientException {
        mLogger.logDebug("Starting login");
        final SimpleWaiter disambiguationWaiter = new SimpleWaiter();
        final AtomicReference<DisambiguationResponse> response = new AtomicReference<>();
        final AtomicReference<ClientException> error = new AtomicReference<>();
        final ICallback<DisambiguationResponse> disambiguationCallback = new ICallback<DisambiguationResponse>() {
            @Override
            public void success(final DisambiguationResponse result) {
                mLogger.logDebug(String.format("Successfully disambiguated '%s' as account type '%s'",
                                                  result.getAccount(),
                                                  result.getAccountType()));
                response.set(result);
                disambiguationWaiter.signal();
            }

            @Override
            public void failure(final ClientException error2) {
                error.set(new ClientAuthenticatorException("Unable to disambiguate account type",
                                                           OneDriveErrorCodes.AuthenticationFailure));
                //noinspection ThrowableResultOfMethodCallIgnored
                mLogger.logError(error.get().getMessage(), error.get());
                disambiguationWaiter.signal();
            }
        };

        AccountType accountType = getAccountTypeInPreferences();
        String accountName = null;
        if (accountType != null) {
            mLogger.logDebug(String.format("Found saved account information %s type of account", accountType));
        } else {
            mLogger.logDebug("Creating disambiguation ui, waiting for user to sign in");
            new DisambiguationRequest(mActivity, disambiguationCallback, mLogger).execute();
            disambiguationWaiter.waitForSignal();

            //noinspection ThrowableResultOfMethodCallIgnored
            if (error.get() != null) {
                throw error.get();
            }
            final DisambiguationResponse disambiguationResponse = response.get();
            accountType = disambiguationResponse.getAccountType();
            accountName = disambiguationResponse.getAccount();
        }

        final IAccountInfo accountInfo;
        switch (accountType) {
            case ActiveDirectory:
                accountInfo = mADALAuthenticator.login(accountName);
                break;
            case MicrosoftAccount:
                accountInfo = mMSAAuthenticator.login(accountName);
                break;
            default:
                final UnsupportedOperationException unsupportedOperationException
                    = new UnsupportedOperationException("Unrecognized account type "
                                                        + accountType);
                mLogger.logError("Unrecognized account type", unsupportedOperationException);
                throw unsupportedOperationException;
        }

        setAccountTypeInPreferences(accountType);
        mAccountInfo.set(accountInfo);
        return mAccountInfo.get();
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
     * @throws ClientException Exception occurs if the login was unable to complete for any reason.
     */
    @Override
    public synchronized IAccountInfo loginSilent() throws ClientException {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        mLogger.logDebug("Starting login silent");

        final AccountType accountType = getAccountTypeInPreferences();
        if (accountType != null) {
            mLogger.logDebug(String.format("Expecting %s type of account", accountType));
        }

        mLogger.logDebug("Checking MSA");
        IAccountInfo accountInfo = mMSAAuthenticator.loginSilent();
        if (accountInfo != null) {
            mLogger.logDebug("Found account info in MSA");
            setAccountTypeInPreferences(accountType);
            mAccountInfo.set(accountInfo);
            return accountInfo;
        }

        mLogger.logDebug("Checking ADAL");
        accountInfo = mADALAuthenticator.loginSilent();
        mAccountInfo.set(accountInfo);
        if (accountInfo != null) {
            mLogger.logDebug("Found account info in ADAL");
            setAccountTypeInPreferences(accountType);
        }
        return mAccountInfo.get();
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
                logout();
                mExecutors.performOnForeground((Void) null, logoutCallback);
            }
        });
    }

    /**
     * Log the current user out.
     * @throws ClientException If the logout was unable to complete for any reason.
     */
    @Override
    public synchronized void logout() throws ClientException {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        mLogger.logDebug("Starting logout");
        if (mMSAAuthenticator.getAccountInfo() != null) {
            mLogger.logDebug("Starting logout of MSA account");
            mMSAAuthenticator.logout();
        } else if (mADALAuthenticator.getAccountInfo() != null) {
            mLogger.logDebug("Starting logout of ADAL account");
            mADALAuthenticator.logout();
        }

        getSharedPreferences()
                .edit()
                .clear()
                .putInt(VERSION_CODE_KEY, BuildConfig.VERSION_CODE)
                .commit();
    }

    /**
     * Gets the current account info for this authenticator.
     * @return NULL if no account is available.
     */
    @Override
    public IAccountInfo getAccountInfo() {
        return mAccountInfo.get();
    }

    /**
     * Gets the shared preferences for this authenticator.
     * @return The shared preferences.
     */
    private SharedPreferences getSharedPreferences() {
        return mActivity.getSharedPreferences(DISAMBIGUATION_AUTHENTICATOR_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Sets the AccountType from SharedPreferences
     * @param accountType The account type, can be null
     */
    private void setAccountTypeInPreferences(@Nullable final AccountType accountType) {
        if (accountType == null) {
            return;
        }

        getSharedPreferences()
                .edit()
                .putString(ACCOUNT_TYPE_KEY, accountType.toString())
                .putInt(VERSION_CODE_KEY, BuildConfig.VERSION_CODE)
                .commit();
    }

    /**
     * Gets the AccountType from SharedPreferences
     * @return The account type, null if none was found
     */
    @Nullable
    private AccountType getAccountTypeInPreferences() {
        final String accountTypeString = getSharedPreferences().getString(ACCOUNT_TYPE_KEY, null);
        if (accountTypeString == null) {
            return null;
        }
        return AccountType.valueOf(accountTypeString);
    }
}
