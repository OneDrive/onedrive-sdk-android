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

import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.OneDriveErrorCodes;

import java.util.Locale;

/**
 * The web view to host the disambiguation UI in.
 */
class DisambiguationWebView extends WebViewClient {

    /**
     * The hosting dialog.
     */
    private final DisambiguationDialog mDisambiguationDialog;

    /**
     * The callback when the UI has completed.
     */
    private final ICallback<DisambiguationResponse> mCallback;

    /**
     * Creates the disambiguation web view.
     * @param disambiguationDialog The hosting dialog.
     * @param callback The callback when the UI has completed.
     */
    public DisambiguationWebView(final DisambiguationDialog disambiguationDialog,
                                 final ICallback<DisambiguationResponse> callback) {
        mDisambiguationDialog = disambiguationDialog;
        mCallback = callback;
    }

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        mDisambiguationDialog.getLogger().logDebug("onPageStarted for url '" + url + "'");
        super.onPageStarted(view, url, favicon);
        final Uri uri = Uri.parse(url);
        if (uri.getAuthority().equalsIgnoreCase("localhost:777")) {
            mDisambiguationDialog.getLogger().logDebug("Found callback from disambiguation service");
            final AccountType accountType = AccountType.fromRepresentation(uri.getQueryParameter("account_type"));
            final String account = uri.getQueryParameter("user_email");
            mCallback.success(new DisambiguationResponse(accountType, account));
            view.stopLoading();
            mDisambiguationDialog.dismiss();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(final WebView view,
                                final int errorCode,
                                final String description,
                                final String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);

        final String errorMessage = String.format(Locale.ROOT,
                                                  "Url %s, Error code: %d, Description %s",
                                                  failingUrl,
                                                  errorCode,
                                                  description);
        mCallback.failure(new ClientAuthenticatorException(errorMessage, OneDriveErrorCodes.AuthenticationFailure));
        mDisambiguationDialog.dismiss();
    }
}
