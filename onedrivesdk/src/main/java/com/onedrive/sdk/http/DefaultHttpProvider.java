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

package com.onedrive.sdk.http;

import com.onedrive.sdk.concurrency.AsyncMonitorLocation;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.concurrency.IProgressCallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.logger.ILogger;
import com.onedrive.sdk.logger.LoggerLevel;
import com.onedrive.sdk.serializer.ISerializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

/**
 * Http provider based off of URLConnection.
 */
public class DefaultHttpProvider implements IHttpProvider {

    /**
     * The content type header
     */
    static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";

    /**
     * The content type for json responses
     */
    static final String JSON_CONTENT_TYPE = "application/json";

    /**
     * The serializer.
     */
    private final ISerializer mSerializer;

    /**
     * The request interceptor.
     */
    private final IRequestInterceptor mRequestInterceptor;

    /**
     * The executors.
     */
    private final IExecutors mExecutors;

    /**
     * The logger.
     */
    private final ILogger mLogger;

    /**
     * The connection factory.
     */
    private IConnectionFactory mConnectionFactory;

    /**
     * Creates the DefaultHttpProvider.
     * @param serializer The serializer.
     * @param requestInterceptor The request interceptor.
     * @param executors The executors.
     * @param logger The logger for diagnostic information.
     */
    public DefaultHttpProvider(final ISerializer serializer,
                               final IRequestInterceptor requestInterceptor,
                               final IExecutors executors,
                               final ILogger logger) {
        mSerializer = serializer;
        mRequestInterceptor = requestInterceptor;
        mExecutors = executors;
        mLogger = logger;
        mConnectionFactory = new DefaultConnectionFactory();
    }

    /**
     * Gets the serializer for this http provider.
     *
     * @return The serializer for this provider.
     */
    @Override
    public ISerializer getSerializer() {
        return mSerializer;
    }

    /**
     * Sends the http request asynchronously.
     * @param request The request description.
     * @param callback The callback to be called after success or failure.
     * @param resultClass The class of the response from the service.
     * @param serializable The object to send to the service in the body of the request.
     * @param <Result> The type of the response object.
     * @param <Body> The type of the object to send to the service in the body of the request.
     */
    @Override
    public <Result, Body> void send(final IHttpRequest request,
                                    final ICallback<Result> callback,
                                    final Class<Result> resultClass,
                                    final Body serializable) {
        final IProgressCallback<Result> progressCallback;
        if (callback instanceof IProgressCallback) {
            progressCallback = (IProgressCallback<Result>) callback;
        } else {
            progressCallback = null;
        }

        mExecutors.performOnBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    mExecutors.performOnForeground(sendRequestInternal(request,
                            resultClass,
                            serializable,
                            progressCallback,
                            null),
                            callback);
                } catch (final ClientException e) {
                    mExecutors.performOnForeground(e, callback);
                }
            }
        });
    }

    /**
     * Sends the http request.
     * @param request The request description.
     * @param resultClass The class of the response from the service.
     * @param serializable The object to send to the service in the body of the request.
     * @param <Result> The type of the response object.
     * @param <Body> The type of the object to send to the service in the body of the request.
     * @return The result from the request.
     * @throws ClientException An exception occurs if the request was unable to complete for any reason.
     */
    @Override
    public <Result, Body> Result send(final IHttpRequest request,
                                      final Class<Result> resultClass,
                                      final Body serializable)
            throws ClientException {
        return send(request, resultClass, serializable, null);
    }

    /**
     * Sends the http request.
     * @param request The request description.
     * @param resultClass The class of the response from the service.
     * @param serializable The object to send to the service in the body of the request.
     * @param handler The handler for stateful response.
     * @param <Result> The type of the response object.
     * @param <Body> The type of the object to send to the service in the body of the request.
     * @param <DeserializeType> The response handler for stateful response.
     * @return The result from the request.
     * @throws ClientException This exception occurs if the request was unable to complete for any reason.
     */
    public <Result, Body, DeserializeType> Result send(final IHttpRequest request,
                               final Class<Result> resultClass,
                               final Body serializable,
                               final IStatefulResponseHandler<Result, DeserializeType> handler) throws ClientException {
        return sendRequestInternal(request, resultClass, serializable, null, handler);
    }

    /**
     * Sends the http request.
     * @param request The request description.
     * @param resultClass The class of the response from the service.
     * @param serializable The object to send to the service in the body of the request.
     * @param progress The progress callback for the request.
     * @param handler The handler for stateful response.
     * @param <Result> The type of the response object.
     * @param <Body> The type of the object to send to the service in the body of the request.
     * @param <DeserializeType> The response handler for stateful response.
     * @return The result from the request.
     * @throws ClientException An exception occurs if the request was unable to complete for any reason.
     */
    private <Result, Body, DeserializeType> Result sendRequestInternal(final IHttpRequest request,
                                                      final Class<Result> resultClass,
                                                      final Body serializable,
                                                      final IProgressCallback<Result> progress,
                                                      final IStatefulResponseHandler<Result, DeserializeType> handler)
            throws ClientException {
        final int defaultBufferSize = 4096;
        final String contentLengthHeaderName = "Content-Length";
        final String binaryContentType = "application/octet-stream";

        try {
            if (mRequestInterceptor != null) {
                mRequestInterceptor.intercept(request);
            }

            OutputStream out = null;
            InputStream in = null;
            boolean isBinaryStreamInput = false;
            final URL requestUrl = request.getRequestUrl();
            mLogger.logDebug("Starting to send request, URL " + requestUrl.toString());
            final IConnection connection = mConnectionFactory.createFromRequest(request);

            try {
                mLogger.logDebug("Request Method " + request.getHttpMethod().toString());

                InputStream body = null;
                byte[] bytesToWrite = null;
                if (serializable == null) {
                    body = null;
                } else if (serializable instanceof byte[]) {
                    mLogger.logDebug("Sending byte[] as request body");
                    bytesToWrite = (byte[]) serializable;
                    connection.addRequestHeader(CONTENT_TYPE_HEADER_NAME, binaryContentType);
                    connection.setContentLength(bytesToWrite.length);
                } else if (serializable instanceof InputStream) {
                    mLogger.logDebug("Using InputStream as request body");
                    body = (InputStream) serializable;
                    connection.addRequestHeader(CONTENT_TYPE_HEADER_NAME, binaryContentType);
                } else {
                    mLogger.logDebug("Sending " + serializable.getClass().getName() + " as request body");
                    final String serializeObject = mSerializer.serializeObject(serializable);
                    bytesToWrite = serializeObject.getBytes();
                    connection.addRequestHeader(CONTENT_TYPE_HEADER_NAME, JSON_CONTENT_TYPE);
                    connection.setContentLength(bytesToWrite.length);
                }

                if (body == null && bytesToWrite != null) {
                    body = new ByteArrayInputStream(bytesToWrite);
                }

                // Handle cases where we've got a body to process.
                if (body != null) {
                    out = connection.getOutputStream();

                    int writtenSoFar = 0;
                    int toWrite;
                    byte[] buffer = new byte[defaultBufferSize];
                    BufferedOutputStream bos = new BufferedOutputStream(out);

                    while ((toWrite = body.read(buffer)) != -1) {
                        bos.write(buffer, writtenSoFar, toWrite);
                        if (progress != null) {
                            mExecutors.performOnForeground(writtenSoFar, buffer.length, progress);
                        }
                    }
                    bos.close();
                }

                if (handler != null) {
                    handler.configConnection(connection);
                }

                mLogger.logDebug(String.format("Response code %d, %s",
                        connection.getResponseCode(),
                        connection.getResponseMessage()));

                if (handler != null) {
                    mLogger.logDebug("StatefulResponse is handling the HTTP response.");
                    return handler.generateResult(
                            request, connection, this.getSerializer(), this.mLogger);
                }

                if (connection.getResponseCode() >= HttpResponseCode.HTTP_CLIENT_ERROR) {
                    mLogger.logDebug("Handling error response");
                    in = connection.getInputStream();
                    handleErrorResponse(request, serializable, connection);
                }

                if (connection.getResponseCode() == HttpResponseCode.HTTP_NOBODY
                        || connection.getResponseCode() == HttpResponseCode.HTTP_NOT_MODIFIED) {
                    mLogger.logDebug("Handling response with no body");
                    return null;
                }

                if (connection.getResponseCode() == HttpResponseCode.HTTP_ACCEPTED) {
                    mLogger.logDebug("Handling accepted response");
                    if (resultClass == AsyncMonitorLocation.class) {
                        //noinspection unchecked
                        return (Result) new AsyncMonitorLocation(connection.getHeaders().get("Location"));
                    }
                }

                in = new BufferedInputStream(connection.getInputStream());

                final Map<String, String> headers = connection.getHeaders();

                final String contentType = headers.get(CONTENT_TYPE_HEADER_NAME);
                if (contentType.contains(JSON_CONTENT_TYPE)) {
                    mLogger.logDebug("Response json");
                    return handleJsonResponse(in, resultClass);
                } else {
                    mLogger.logDebug("Response binary");
                    isBinaryStreamInput = true;
                    //noinspection unchecked
                    return (Result) handleBinaryStream(in);
                }
            } finally {
                if (out != null) {
                    out.close();
                }
                if (!isBinaryStreamInput && in != null) {
                    in.close();
                    connection.close();
                }
            }
        } catch (final OneDriveServiceException ex) {
            final boolean shouldLogVerbosely = mLogger.getLoggingLevel() == LoggerLevel.Debug;
            mLogger.logError("OneDrive Service exception " + ex.getMessage(shouldLogVerbosely), ex);
            throw ex;
        } catch (final Exception ex) {
            final ClientException clientException = new ClientException("Error during http request",
                    ex,
                    OneDriveErrorCodes.GeneralException);
            mLogger.logError("Error during http request", clientException);
            throw clientException;
        }
    }

    /**
     * Handles the event of an error response.
     * @param request The request that caused the failed response.
     * @param serializable The body of the request.
     * @param connection The url connection.
     * @param <Body> The type of the request body.
     * @throws IOException An exception occurs if there were any problems interacting with the connection object.
     */
    private <Body> void handleErrorResponse(final IHttpRequest request,
                                            final Body serializable,
                                            final IConnection connection)
            throws IOException {
        throw OneDriveServiceException.createFromConnection(request, serializable, mSerializer,
                connection);
    }

    /**
     * Handles the cause where the response is a binary stream.
     *
     * @param in The input stream from the response.
     * @return The input stream to return to the caller.
     */
    private InputStream handleBinaryStream(final InputStream in) {
        return in;
    }

    /**
     * Handles the cause where the response is a json object.
     * @param in The input stream from the response.
     * @param clazz The class of the response object.
     * @param <Result> The type of the response object.
     * @return The json object.
     */
    private <Result> Result handleJsonResponse(final InputStream in, final Class<Result> clazz) {
        if (clazz == null) {
            return null;
        }

        final String rawJson = streamToString(in);
        return getSerializer().deserializeObject(rawJson, clazz);
    }

    /**
     * Sets the connection factory for this provider.
     *
     * @param factory The new factory.
     */
    void setConnectionFactory(final IConnectionFactory factory) {
        mConnectionFactory = factory;
    }

    /**
     * Reads in a stream and converts it into a string.
     *
     * @param input The response body stream.
     * @return The string result.
     */
    public static String streamToString(final InputStream input) {
        final String httpStreamEncoding = "UTF-8";
        final String endOfFile = "\\A";
        final Scanner scanner = new Scanner(input, httpStreamEncoding).useDelimiter(endOfFile);
        return scanner.next();
    }
}
