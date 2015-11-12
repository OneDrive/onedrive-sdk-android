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

import android.net.Uri;

import com.microsoft.services.msa.OAuthConfig;

/**
 * OAuth configuration for the Microsoft services
 */
class MicrosoftOAuthConfig implements OAuthConfig {

    /**
     * The domain to authenticate against
     */
    public static final String HTTPS_LOGIN_LIVE_COM = "https://login.microsoftonline.com/common/oauth2/";

    /**
     * The authorization uri
     */
    private final Uri mOAuthAuthorizeUri;

    /**
     * The desktop uri
     */
    private final Uri mOAuthDesktopUri;

    /**
     * The logout uri
     */
    private final Uri mOAuthLogoutUri;

    /**
     * The auth token uri
     */
    private final Uri mOAuthTokenUri;

    /**
     * Default Constructor
     */
    public MicrosoftOAuthConfig() {
        mOAuthAuthorizeUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "authorize");
        mOAuthDesktopUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "desktop");
        mOAuthLogoutUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "logout");
        mOAuthTokenUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "token");
    }

    @Override
    public Uri getAuthorizeUri() {
        return mOAuthAuthorizeUri;
    }

    @Override
    public Uri getDesktopUri() {
        return mOAuthDesktopUri;
    }

    @Override
    public Uri getLogoutUri() {
        return mOAuthLogoutUri;
    }

    @Override
    public Uri getTokenUri() {
        return mOAuthTokenUri;
    }
}
