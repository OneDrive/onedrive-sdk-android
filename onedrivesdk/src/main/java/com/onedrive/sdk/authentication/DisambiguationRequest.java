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

import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.logger.ILogger;

/**
 * A disambiguation request.
 */
class DisambiguationRequest {

    /**
     * The current activity.
     */
    private final Activity mActivity;

    /**
     * The callback when the request has completed.
     */
    private final ICallback<DisambiguationResponse> mCallback;

    /**
     * The logger.
     */
    private final ILogger mLogger;

    /**
     * Creates the disambiguation request.
     * @param activity The context to show the UI in.
     * @param callback The callback when the request has completed.
     * @param logger The logger.
     */
    public DisambiguationRequest(final Activity activity,
                                 final ICallback<DisambiguationResponse> callback,
                                 final ILogger logger) {
        mActivity = activity;
        mCallback = callback;
        mLogger = logger;
    }

    /**
     * Executes the disambiguation request.
     */
    public void execute() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new DisambiguationDialog(mActivity, DisambiguationRequest.this).show();
            }
        });
    }

    /**
     * Gets the callback when the request has completed.
     * @return The callback.
     */
    public ICallback<DisambiguationResponse> getCallback() {
        return mCallback;
    }

    /**
     * Get the logger.
     * @return The logger.
     */
    public ILogger getLogger() {
        return mLogger;
    }
}
