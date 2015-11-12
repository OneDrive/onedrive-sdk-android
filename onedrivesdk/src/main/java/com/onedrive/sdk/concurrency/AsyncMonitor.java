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

import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.extensions.AsyncOperationStatus;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.http.BaseRequest;
import com.onedrive.sdk.http.HttpMethod;

/**
 * Monitors an asynchronous action from the service.
 * @param <T> The result time when the action has completed.
 */
public class AsyncMonitor<T> {

    /**
     * The client.
     */
    private final IOneDriveClient mClient;

    /**
     * The monitor's location.
     */
    private final AsyncMonitorLocation mMonitorLocation;

    /**
     * The way to retrieve a result from the service.
     */
    private final ResultGetter<T> mResultGetter;

    /**
     * Create a new async monitor.
     * @param client The client.
     * @param monitorLocation The monitor location.
     * @param resultGetter The way to retrieve the result.
     */
    public AsyncMonitor(final IOneDriveClient client,
                        final AsyncMonitorLocation monitorLocation,
                        final ResultGetter<T> resultGetter) {
        mClient = client;
        mMonitorLocation = monitorLocation;
        mResultGetter = resultGetter;
    }

    /**
     * Gets the status of the monitor from the service asynchronously.
     * @param callback The callback.
     */
    public void getStatus(final ICallback<AsyncOperationStatus> callback) {
        mClient.getExecutors().performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    mClient.getExecutors().performOnForeground(getStatus(), callback);
                } catch (final ClientException e) {
                    mClient.getExecutors().performOnForeground(e, callback);
                }
            }
        });
    }

    /**
     * Gets the status of the monitor from the service.
     * @return The status.
     * @throws ClientException Exception occurs if there was a problem retrieving the status from the service.
     */
    public AsyncOperationStatus getStatus() throws ClientException {
        final BaseRequest monitorStatusRequest = new BaseRequest(mMonitorLocation.getLocation(),
                                                                 mClient,
                                                                 /* options */ null,
                                                                 /* response class */ null) { };
        monitorStatusRequest.setHttpMethod(HttpMethod.GET);

        return mClient.getHttpProvider().send(monitorStatusRequest,
                                              AsyncOperationStatus.class,
                                              /* serialization object*/ null);
    }

    /**
     * Gets the result from the service asynchronously.
     * @param callback The callback.
     */
    public void getResult(final ICallback<T> callback) {
        mClient.getExecutors().performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    mClient.getExecutors().performOnForeground(getResult(), callback);
                } catch (final ClientException e) {
                    mClient.getExecutors().performOnForeground(e, callback);
                }
            }
        });
    }

    /**
     * Gets the result from the service.
     * @return The result.
     * @throws ClientException An exception occurs if there was an problem retrieving the status from the service.
     */
    public T getResult() throws ClientException {
        final AsyncOperationStatus status = getStatus();
        if (status.seeOther == null) {
            throw new ClientException("Async operation '" + status.operation + "' has not completed!",
                                      /* throwable */ null,
                                      OneDriveErrorCodes.AsyncTaskNotCompleted);
        }
        return mResultGetter.getResultFrom(status.seeOther, mClient);
    }

    /**
     * Polls the service for the monitored operation to complete.
     * @param millisBetweenPoll The milliseconds between polls.
     * @param callback The progress callback.
     */
    public void pollForResult(final long millisBetweenPoll, final IProgressCallback<T> callback) {
        final int progressMax = 100;
        mClient.getLogger().logDebug("Starting to poll for request " + mMonitorLocation.getLocation());
        mClient.getExecutors().performOnBackground(new Runnable() {
            @Override
            public void run() {
                AsyncOperationStatus status = null;
                do {
                    if (status  != null) {
                        try {
                            Thread.sleep(millisBetweenPoll);
                        } catch (final InterruptedException ignored) {
                            mClient.getLogger().logDebug("InterruptedException ignored");
                        }
                    }
                    status = getStatus();
                    if (status.percentageComplete != null) {
                        mClient.getExecutors().performOnForeground(status.percentageComplete.intValue(),
                                                                   progressMax,
                                                                   callback);
                    }
                } while (!(isCompleted(status) || isFailed(status)));
                mClient.getLogger().logDebug("Polling has completed, got final status: " + status.status);
                if (isFailed(status)) {
                    mClient.getExecutors().performOnForeground(new AsyncOperationException(status),
                                                                  callback);
                }

                mClient.getExecutors().performOnForeground(getResult(), callback);
            }
        });
    }

    /**
     * Checks if the status is completed.
     * @param status The status.
     * @return True if the state is completed, and false if its not.
     */
    private boolean isCompleted(final AsyncOperationStatus status) {
        return status.status.equalsIgnoreCase("completed");
    }

    /**
     * Checks if the status failed.
     * @param status The status.
     * @return True if the state is failed, and false if its not.
     */
    private boolean isFailed(final AsyncOperationStatus status) {
        return status.status.equalsIgnoreCase("failed");
    }
}

