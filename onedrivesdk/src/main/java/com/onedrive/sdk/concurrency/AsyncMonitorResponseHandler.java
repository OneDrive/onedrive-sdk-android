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

package com.onedrive.sdk.concurrency;

import com.onedrive.sdk.extensions.AsyncOperationStatus;
import com.onedrive.sdk.http.DefaultHttpProvider;
import com.onedrive.sdk.http.HttpResponseCode;
import com.onedrive.sdk.http.IConnection;
import com.onedrive.sdk.http.IHttpRequest;
import com.onedrive.sdk.http.IStatefulResponseHandler;
import com.onedrive.sdk.logger.ILogger;
import com.onedrive.sdk.serializer.ISerializer;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * The handler class for async monitor response from server.
 */
public class AsyncMonitorResponseHandler implements IStatefulResponseHandler<AsyncOperationStatus, String> {

    /**
     * Configure the connection before get response.
     *
     * @param connection The http connection.
     */
    @Override
    public void configConnection(final IConnection connection) {
        connection.setFollowRedirects(false);
    }

    /**
     * Generate the async operation result based on server response.
     *
     * @param request    The http request.
     * @param connection The http connection.
     * @param serializer The serializer.
     * @param logger     The logger.
     * @return The async operation status.
     * @throws Exception An exception occurs if the request was unable to complete for any reason.
     */
    @Override
    public AsyncOperationStatus generateResult(final IHttpRequest request,
                                               final IConnection connection,
                                               final ISerializer serializer,
                                               final ILogger logger)
            throws Exception {
        if (connection.getResponseCode() == HttpResponseCode.HTTP_SEE_OTHER) {
            logger.logDebug("Item copy job has completed.");
            return AsyncOperationStatus.createdCompleted(connection.getHeaders().get("Location"));
        }

        InputStream in = null;

        try {
            in = new BufferedInputStream(connection.getInputStream());
            final AsyncOperationStatus result = serializer.deserializeObject(
                    DefaultHttpProvider.streamToString(in), AsyncOperationStatus.class);
            result.seeOther = connection.getHeaders().get("Location");
            return result;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
