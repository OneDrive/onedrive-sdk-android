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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationCancelError;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.PromptBehavior;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.http.BaseRequest;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.concurrency.SimpleWaiter;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.http.HttpMethod;
import com.onedrive.sdk.http.IHttpProvider;
import com.onedrive.sdk.logger.ILogger;
import com.onedrive.sdk.options.HeaderOption;
import com.onedrive.sdk.options.Option;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.NoSuchPaddingException;

/**
 * Wrapper for the ADAL authentication library:
 * https://github.com/AzureAD/azure-activedirectory-library-for-android
 */
public abstract class ADALAuthenticator implements IAuthenticator {

    /**
     * The login authority for Azure Active Directory.
     */
    private static final String LOGIN_AUTHORITY
        = "https://login.windows.net/common/oauth2/authorize";

    /**
     * The Discovery Service url.
     */
    private static final String DISCOVERY_SERVICE_URL = "https://api.office.com/discovery/v2.0/me/Services";

    /**
     * The Discovery Service resource id.
     */
    private static final String DISCOVER_SERVICE_RESOURCE_ID = "https://api.office.com/discovery/";

    /**
     * The preferences for this authenticator.
     */
    private static final String ADAL_AUTHENTICATOR_PREFS = "ADALAuthenticatorPrefs";

    /**
     * The key for the user id.
     */
    private static final String USER_ID_KEY = "userId";

    /**
     * The key for the resource url.
     */
    private static final String RESOURCE_URL_KEY = "resourceUrl";

    /**
     * The key for the service info.
     */
    private static final String SERVICE_INFO_KEY = "serviceInfo";

    /**
     * Determines if the authority should be validated.
     */
    private static final boolean VALIDATE_AUTHORITY = true;

    /**
     * The active resource url.
     */
    private final AtomicReference<String> mResourceUrl = new AtomicReference<>();

    /**
     * The active user id.
     */
    private final AtomicReference<String> mUserId = new AtomicReference<>();

    /**
     * The active service info object.
     */
    private final AtomicReference<ServiceInfo>  mOneDriveServiceInfo = new AtomicReference<>();

    /**
     * The active account info.
     */
    private final AtomicReference<IAccountInfo> mAccountInfo = new AtomicReference<>();

    /**
     * Determines if this authenticator has been initialized.
     */
    private boolean mInitialized;

    /**
     * The context UI, with which interactions should happen.
     */
    private Activity mActivity;

    /**
     * The http provider.
     */
    private IHttpProvider mHttpProvider;

    /**
     * The executors.
     */
    private IExecutors mExecutors;

    /**
     * The authentication context for ADAL.
     */
    private AuthenticationContext mAdalContext;

    /**
     * The logger.
     */
    private ILogger mLogger;

    /**
     * The client id for this authenticator.
     * @return The client id.
     */
    protected abstract String getClientId();

    /**
     * The redirect url that corresponds with this client id.
     * @return The redirect url.
     */
    protected abstract String getRedirectUrl();

    /**
     * Gets the current account info for this authenticator.
     * @return NULL if no account is available.
     */
    @Override
    public IAccountInfo getAccountInfo() {
        return mAccountInfo.get();
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
        mHttpProvider = httpProvider;
        mActivity = activity;
        mLogger = logger;
        try {
            mAdalContext = new AuthenticationContext(activity,
                                                    LOGIN_AUTHORITY,
                                                    VALIDATE_AUTHORITY);
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException e) {
            final ClientAuthenticatorException exception = new ClientAuthenticatorException(
                "Unable to access required cryptographic libraries for ADAL",
                e,
                OneDriveErrorCodes.AuthenticationFailure);
            mLogger.logError("Problem creating the AuthenticationContext for ADAL", exception);
            throw exception;
        }

        final SharedPreferences prefs = getSharedPreferences();
        mUserId.set(prefs.getString(USER_ID_KEY, null));
        mResourceUrl.set(prefs.getString(RESOURCE_URL_KEY, null));

        final String serviceInfoAsString = prefs.getString(SERVICE_INFO_KEY, null);
        ServiceInfo serviceInfo = null;
        try {
            if (serviceInfoAsString != null) {
                serviceInfo = mHttpProvider.getSerializer()
                                  .deserializeObject(serviceInfoAsString, ServiceInfo.class);
            }
        } catch (final Exception ex) {
            mLogger.logError("Unable to parse serviceInfo from saved preferences", ex);
        }
        mOneDriveServiceInfo.set(serviceInfo);
        mInitialized = true;

        // If there is incomplete information about the account, clear everything so
        // the application is in a known state.
        if (mUserId.get() != null || mResourceUrl.get() != null || mOneDriveServiceInfo.get() != null) {
            mLogger.logDebug("Found existing login information");
            if (mUserId.get() == null || mResourceUrl.get() == null || mOneDriveServiceInfo.get() == null) {
                mLogger.logDebug("Existing login information was incompletely, flushing sign in state");
                this.logout();
            }
        }
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
            throw new IllegalArgumentException("loginCallback");
        }

        mLogger.logDebug("Starting login async");

        mExecutors.performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    loginCallback.success(login(emailAddressHint));
                } catch (final ClientException e) {
                    loginCallback.failure(e);
                }
            }
        });
    }

    /**
     * Starts an interactive login.
     * @param emailAddressHint The hint for the email address during the interactive login.
     * @return The account info.
     * @throws ClientException An exception occurs if the login was unable to complete for any reason.
     */
    @Override
    public synchronized IAccountInfo login(final String emailAddressHint) throws ClientException {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        mLogger.logDebug("Starting login");

        final AuthenticationResult discoveryServiceAuthToken =
            getDiscoveryServiceAuthResult(emailAddressHint);

        if (discoveryServiceAuthToken.getStatus() != AuthenticationResult.AuthenticationStatus.Succeeded) {
            final ClientAuthenticatorException clientAuthenticatorException
                = new ClientAuthenticatorException("Unable to authenticate user with ADAL, Error Code: "
                                                       + discoveryServiceAuthToken.getErrorCode()
                                                       + " Error Message"
                                                       + discoveryServiceAuthToken
                                                             .getErrorDescription(),
                                                   OneDriveErrorCodes.AuthenticationFailure);
            mLogger.logError("Unsuccessful login attempt", clientAuthenticatorException);
            throw clientAuthenticatorException;
        }

        // Get the resource information for the OneDrive services.
        final ServiceInfo oneDriveServiceInfo =
                getOneDriveServiceInfoFromDiscoveryService(discoveryServiceAuthToken.getAccessToken());

        // Request a fresh auth token for the OneDrive service.
        final AuthenticationResult oneDriveServiceAuthToken =
                getOneDriveServiceAuthResult(discoveryServiceAuthToken, oneDriveServiceInfo);

        // Get the OneDrive auth token and save a reference to it.
        final String serviceInfoAsString = mHttpProvider.getSerializer()
                                               .serializeObject(oneDriveServiceInfo);

        mLogger.logDebug("Successful login, saving information for silent re-auth");
        final SharedPreferences preferences = getSharedPreferences();
        mResourceUrl.set(oneDriveServiceInfo.serviceEndpointUri);
        mUserId.set(discoveryServiceAuthToken.getUserInfo().getUserId());
        mOneDriveServiceInfo.set(oneDriveServiceInfo);
        preferences
                .edit()
                .putString(RESOURCE_URL_KEY, mResourceUrl.get())
                .putString(USER_ID_KEY, mUserId.get())
                .putString(SERVICE_INFO_KEY, serviceInfoAsString)
                .apply();

        mLogger.logDebug("Successfully retrieved login information");
        mLogger.logDebug("   Resource Url: " + mResourceUrl.get());
        mLogger.logDebug("   User ID: " + mUserId.get());
        mLogger.logDebug("   Service Info: " + serviceInfoAsString);
        final ADALAccountInfo adalAccountInfo = new ADALAccountInfo(this,
                                                                    oneDriveServiceAuthToken,
                                                                    oneDriveServiceInfo,
                                                                    mLogger);
        mAccountInfo.set(adalAccountInfo);
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
            throw new IllegalArgumentException("loginCallback");
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

        if (mResourceUrl.get() == null) {
            return null;
        }

        mLogger.logDebug("Starting login silent");

        final SimpleWaiter loginSilentWaiter = new SimpleWaiter();
        final AtomicReference<AuthenticationResult> authResult = new AtomicReference<>();
        final AtomicReference<ClientException> error = new AtomicReference<>();

        final AuthenticationCallback<AuthenticationResult> callback
            = new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(final AuthenticationResult authenticationResult) {
                final String userId;
                if (authenticationResult.getUserInfo() == null) {
                    userId = "Invalid User Id";
                } else {
                    userId = authenticationResult.getUserInfo().getUserId();
                }
                final String tenantId = authenticationResult.getTenantId();
                mLogger.logDebug(String.format("Successful silent auth for user id '%s', tenant id '%s'",
                                               userId,
                                               tenantId));
                authResult.set(authenticationResult);
                loginSilentWaiter.signal();
            }

            @Override
            public void onError(final Exception e) {
                error.set(new ClientAuthenticatorException("Silent authentication failure from ADAL",
                                                           e,
                                                           OneDriveErrorCodes.AuthenticationFailure));
                loginSilentWaiter.signal();
            }
        };
        mAdalContext.acquireTokenSilent(mResourceUrl.get(), getClientId(), mUserId.get(), callback);

        loginSilentWaiter.waitForSignal();
        //noinspection ThrowableResultOfMethodCallIgnored
        if (error.get() != null) {
            throw error.get();
        }

        final ADALAccountInfo adalAccountInfo = new ADALAccountInfo(this,
                                                                    authResult.get(),
                                                                    mOneDriveServiceInfo.get(),
                                                                    mLogger);
        mAccountInfo.set(adalAccountInfo);
        return mAccountInfo.get();
    }

    /**
     * Logs the current user out.
     * @param logoutCallback The callback to be called when the logout is complete.
     */
    @Override
    public void logout(final ICallback<Void> logoutCallback) {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        if (logoutCallback == null) {
            throw new IllegalArgumentException("logoutCallback");
        }

        mLogger.logDebug("Starting logout async");

        mExecutors.performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    logout();
                    mExecutors.performOnForeground((Void) null, logoutCallback);
                } catch (final ClientException e) {
                    mExecutors.performOnForeground((Void) null, logoutCallback);
                }
            }
        });
    }

    /**
     * Logs the current user out.
     * @throws ClientException An exception occurs if the logout was unable to complete for any reason.
     */
    @SuppressWarnings("deprecation")
    @Override
    public synchronized void logout() throws ClientException {
        if (!mInitialized) {
            throw new IllegalStateException("init must be called");
        }

        mLogger.logDebug("Starting logout");
        mLogger.logDebug("Clearing ADAL cache");
        mAdalContext.getCache().removeAll();

        mLogger.logDebug("Clearing all webview cookies");
        CookieSyncManager.createInstance(mActivity);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        CookieSyncManager.getInstance().sync();

        mLogger.logDebug("Clearing all ADAL Authenticator shared preferences");
        final SharedPreferences prefs = getSharedPreferences();
        prefs.edit().clear().apply();
        mUserId.set(null);
        mResourceUrl.set(null);
    }

    /**
     * Get the Discovery Service authentication result.
     * @param emailAddressHint The username to populate in the UI.
     * @return The authentication result.
     */
    private AuthenticationResult getDiscoveryServiceAuthResult(final String emailAddressHint) {
        final SimpleWaiter discoveryCallbackWaiter = new SimpleWaiter();
        final AtomicReference<ClientException> error = new AtomicReference<>();
        final AtomicReference<AuthenticationResult> discoveryServiceToken =
            new AtomicReference<>();
        final AuthenticationCallback<AuthenticationResult> discoveryCallback =
            new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(final AuthenticationResult authenticationResult) {
                final String userId;
                if (authenticationResult.getUserInfo() == null) {
                    userId = "Invalid User Id";
                } else {
                    userId = authenticationResult.getUserInfo().getUserId();
                }
                final String tenantId = authenticationResult.getTenantId();
                mLogger.logDebug(String.format(
                    "Successful response from the discover service for user id '%s', tenant id '%s'",
                    userId,
                    tenantId));
                discoveryServiceToken.set(authenticationResult);
                discoveryCallbackWaiter.signal();
            }

            @Override
            public void onError(final Exception ex) {
                OneDriveErrorCodes code = OneDriveErrorCodes.AuthenticationFailure;
                if (ex instanceof AuthenticationCancelError) {
                    code = OneDriveErrorCodes.AuthenticationCancelled;
                }

                final String message = "Error while retrieving the discovery service auth token";
                error.set(new ClientAuthenticatorException(message, ex, code));
                mLogger.logError("Error while attempting to login interactively", error.get());
                discoveryCallbackWaiter.signal();
            }
            };

        mLogger.logDebug("Starting interactive login for the discover service access token");

        // Initial resource is the Discovery Service.
        mAdalContext.acquireToken(DISCOVER_SERVICE_RESOURCE_ID,
                                    getClientId(),
                                    getRedirectUrl(),
                                    emailAddressHint,
                                    PromptBehavior.Auto,
                                    null,
                                    discoveryCallback);

        mLogger.logDebug("Waiting for interactive login to complete");
        discoveryCallbackWaiter.waitForSignal();
        //noinspection ThrowableResultOfMethodCallIgnored
        if (error.get() != null) {
            throw error.get();
        }
        return discoveryServiceToken.get();
    }

    /**
     * Gets the shared preferences for this authenticator.
     * @return The shared preferences.
     */
    private SharedPreferences getSharedPreferences() {
        return mActivity.getSharedPreferences(ADAL_AUTHENTICATOR_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Finds the OneDrive API service from the list of services.
     * @param services The list of services.
     * @return The service info object for OneDrive.
     */
    private ServiceInfo getOneDriveApiService(final ServiceInfo[] services) {
        for (final ServiceInfo serviceInfo : services) {
            mLogger.logDebug(String.format("Service info resource id%s capabilities %s version %s",
                                           serviceInfo.serviceResourceId,
                                           serviceInfo.capability,
                                           serviceInfo.serviceApiVersion));
            if (serviceInfo.capability.equalsIgnoreCase("MyFiles")
                && serviceInfo.serviceApiVersion.equalsIgnoreCase("v2.0")) {
                return serviceInfo;
            }
        }

        throw new ClientAuthenticatorException("Unable to file the files services from the directory provider",
                                               OneDriveErrorCodes.AuthenticationFailure);
    }

    /**
     * Queries the Discovery Service from the list of services for OneDrive.
     * @param accessToken The access token for the Discovery Service.
     * @return The OneDrive service info.
     */
    private ServiceInfo getOneDriveServiceInfoFromDiscoveryService(final String accessToken) {
        final List<Option> options = new ArrayList<>();
        options.add(new HeaderOption(AuthorizationInterceptor.AUTHORIZATION_HEADER_NAME,
                                     AuthorizationInterceptor.OAUTH_BEARER_PREFIX + accessToken));

        mLogger.logDebug("Starting discovery service request");
        final BaseRequest discoveryServiceRequest = new BaseRequest(DISCOVERY_SERVICE_URL,
                                                                    /* client */ null,
                                                                    options,
                                                                    /* response class */ null) { };
        discoveryServiceRequest.setHttpMethod(HttpMethod.GET);

        final DiscoveryServiceResponse discoveryServiceResponse =
            mHttpProvider.send(discoveryServiceRequest,
                               DiscoveryServiceResponse.class,
                               /* serialization object*/ null);
        return getOneDriveApiService(discoveryServiceResponse.services);
    }

    /**
     * Gets the authentication token for the OneDrive service.
     * @param authenticationResult The authentication result from the Discovery Service.
     * @param oneDriveServiceInfo The OneDrive services info.
     * @return The authentication result for this OneDrive service.
     */
    private AuthenticationResult getOneDriveServiceAuthResult(final AuthenticationResult authenticationResult,
                                                              final ServiceInfo oneDriveServiceInfo) {
        final SimpleWaiter authorityCallbackWaiter = new SimpleWaiter();
        final AtomicReference<ClientException> error = new AtomicReference<>();
        final AtomicReference<AuthenticationResult> oneDriveServiceAuthToken = new AtomicReference<>();
        final AuthenticationCallback<AuthenticationResult> authorityCallback =
            new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(final AuthenticationResult authenticationResult) {
                mLogger.logDebug("Successful refreshed the OneDrive service authentication token");
                oneDriveServiceAuthToken.set(authenticationResult);
                authorityCallbackWaiter.signal();
            }

            @Override
            public void onError(final Exception e) {
                OneDriveErrorCodes code = OneDriveErrorCodes.AuthenticationFailure;
                if (e instanceof AuthenticationCancelError) {
                    code = OneDriveErrorCodes.AuthenticationCancelled;
                }

                error.set(new ClientAuthenticatorException("Error while retrieving the service specific auth token",
                                                           e,
                                                           code));
                mLogger.logError("Unable to refresh token into OneDrive service access token", error.get());
                authorityCallbackWaiter.signal();
            }
        };
        final String refreshToken = authenticationResult.getRefreshToken();

        mLogger.logDebug("Starting OneDrive resource refresh token request");
        mAdalContext.acquireTokenByRefreshToken(
                                                   refreshToken,
                                                   getClientId(),
                                                   oneDriveServiceInfo.serviceResourceId,
                                                   authorityCallback);

        mLogger.logDebug("Waiting for token refresh");
        authorityCallbackWaiter.waitForSignal();
        //noinspection ThrowableResultOfMethodCallIgnored
        if (error.get() != null) {
            throw error.get();
        }
        return oneDriveServiceAuthToken.get();
    }
}
