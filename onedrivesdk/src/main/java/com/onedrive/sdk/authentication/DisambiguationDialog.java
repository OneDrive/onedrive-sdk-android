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

import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.logger.ILogger;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * A dialog that hosts a the disambiguation flow.
 */
class DisambiguationDialog extends Dialog implements DialogInterface.OnCancelListener {

    /**
     * The url for disambiguation page.
     */
    private static final String DISAMBIGUATION_PAGE_URL = "https://onedrive.live"
        + ".com/picker/accountchooser?ru=https://localhost:777&load_login=false";

    /**
     * The disambiguation request object.
     */
    private final DisambiguationRequest mRequest;

    /**
     * Creates the disambiguation dialog.
     * @param context The context to show the UI in.
     * @param request The disambiguation request.
     */
    public DisambiguationDialog(final Context context,
                                final DisambiguationRequest request) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mRequest = request;
    }

    /**
     * Get the logger.
     * @return The logger.
     */
    public ILogger getLogger() {
        return mRequest.getLogger();
    }

    @Override
    public void onCancel(final DialogInterface dialogInterface) {
        mRequest.getLogger().logDebug("Disambiguation dialog canceled");
        mRequest.getCallback().failure(new ClientAuthenticatorException("Authentication Disambiguation Canceled",
                                                                        OneDriveErrorCodes.AuthenticationCancelled));
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setOnCancelListener(this);

        final FrameLayout content = new FrameLayout(this.getContext());
        final LinearLayout webViewContainer = new LinearLayout(this.getContext());
        final WebView webView = new WebView(this.getContext());

        webView.setWebViewClient(new DisambiguationWebView(this, mRequest.getCallback()));

        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl(DISAMBIGUATION_PAGE_URL);
        final ViewGroup.LayoutParams matchParentLayout = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(matchParentLayout);
        webView.setVisibility(View.VISIBLE);

        webViewContainer.addView(webView);
        webViewContainer.setVisibility(View.VISIBLE);

        content.addView(webViewContainer);
        content.setVisibility(View.VISIBLE);
        content.forceLayout();
        webViewContainer.forceLayout();

        this.addContentView(content, matchParentLayout);
    }
}
