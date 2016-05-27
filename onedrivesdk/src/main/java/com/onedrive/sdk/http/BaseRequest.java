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

import android.net.Uri;

import com.microsoft.onedrivesdk.BuildConfig;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.options.HeaderOption;
import com.onedrive.sdk.options.Option;
import com.onedrive.sdk.options.QueryOption;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * An http request.
 */
public abstract class BaseRequest implements IHttpRequest {

    /**
     * The request stats header name.
     */
    private static final String REQUEST_STATS_HEADER_NAME = "X-RequestStats";

    /**
     * The request stats header value format string.
     */
    public static final String REQUEST_STATS_HEADER_VALUE_FORMAT_STRING = "SDK-Version=Android-v%s";

    /**
     * The http method for this request.
     */
    private HttpMethod mMethod;

    /**
     * The url for this request.
     */
    private final String mRequestUrl;

    /**
     * The backing client for this request.
     */
    private final IOneDriveClient mClient;

    /**
     * The header options for this request.
     */
    private final List<HeaderOption> mHeadersOptions;

    /**
     * The query options for this request.
     */
    private final List<QueryOption> mQueryOptions;

    /**
     * The class for the response.
     */
    private final Class mResponseClass;

    /**
     * Create the request.
     * @param requestUrl The url to make the request against.
     * @param client The client which can issue the request.
     * @param options The options for this request.
     * @param responseClass The class for the response.
     */
    public BaseRequest(final String requestUrl,
                       final IOneDriveClient client,
                       final List<Option> options,
                       final Class responseClass) {
        mRequestUrl = requestUrl;
        mClient = client;
        mResponseClass = responseClass;

        mHeadersOptions = new ArrayList<>();
        mQueryOptions = new ArrayList<>();

        if (options != null) {
            for (final Option option : options) {
                if (option instanceof HeaderOption) {
                    mHeadersOptions.add((HeaderOption)option);
                }
                if (option instanceof QueryOption) {
                    mQueryOptions.add((QueryOption)option);
                }
            }
        }
        final HeaderOption requestStatsHeader = new HeaderOption(REQUEST_STATS_HEADER_NAME,
                String.format(REQUEST_STATS_HEADER_VALUE_FORMAT_STRING, BuildConfig.VERSION_NAME));
        mHeadersOptions.add(requestStatsHeader);
    }

    /**
     * Gets the request url.
     * @return The request url.
     */
    @Override
    public URL getRequestUrl() {
        Uri baseUrl = Uri.parse(mRequestUrl);
        final Uri.Builder uriBuilder = new Uri.Builder().scheme(baseUrl.getScheme()).authority(baseUrl.getAuthority());

        for (final String segment : baseUrl.getPathSegments()) {
            uriBuilder.appendPath(segment);
        }

        for (final QueryOption option : mQueryOptions) {
            uriBuilder.appendQueryParameter(option.getName(), option.getValue());
        }

        final String urlString = uriBuilder.build().toString();
        try {
            return new URL(urlString);
        } catch (final MalformedURLException e) {
            throw new ClientException("Invalid URL: " + urlString, e, OneDriveErrorCodes.InvalidRequest);
        }
    }

    /**
     * Gets the http method.
     * @return The http method.
     */
    @Override
    public HttpMethod getHttpMethod() {
        return mMethod;
    }

    /**
     * Gets the headers.
     * @return The headers.
     */
    @Override
    public List<HeaderOption> getHeaders() {
        return mHeadersOptions;
    }

    /**
     * Adds a header to this request.
     * @param header The name of the header.
     * @param value The value of the header.
     */
    @Override
    public void addHeader(final String header, final String value) {
        mHeadersOptions.add(new HeaderOption(header, value));
    }

    /**
     * Sends this request.
     * @param method The http method.
     * @param callback The callback when this request complements.
     * @param serializedObject The object to serialize as the body.
     * @param <T1> The type of the callback result.
     * @param <T2> The type of the serialized body.
     */
    @SuppressWarnings("unchecked")
    protected <T1, T2> void send(final HttpMethod method,
                                 final ICallback<T1> callback,
                                 final T2 serializedObject) {
        mMethod = method;
        mClient.getHttpProvider().send(this, callback, mResponseClass, serializedObject);
    }

    /**
     * Sends this request.
     * @param method The http method.
     * @param serializedObject The object to serialize as the body.
     * @param <T1> The type of the callback result.
     * @param <T2> The type of the serialized body.
     * @return The response object.
     * @throws ClientException An exception occurs if there was an error while the request was sent.
     */
    @SuppressWarnings("unchecked")
    protected <T1, T2> T1 send(final HttpMethod method,
                               final T2 serializedObject) throws ClientException {
        mMethod = method;
        return (T1) mClient.getHttpProvider().send(this, mResponseClass, serializedObject);
    }

    /**
     * Gets the query options for this request.
     * @return The query options for this request.
     */
    public List<QueryOption> getQueryOptions() {
        return mQueryOptions;
    }

    /**
     * Gets the full list of options for this request.
     * @return The full list of options for this request.
     */
    public List<Option> getOptions() {
        final LinkedList<Option> list = new LinkedList<>();
        list.addAll(mHeadersOptions);
        list.addAll(mQueryOptions);
        return Collections.unmodifiableList(list);
    }

    /**
     * Sets the http method.
     * @param httpMethod The http method.
     */
    public void setHttpMethod(final HttpMethod httpMethod) {
        mMethod = httpMethod;
    }

    /**
     * Gets the client.
     * @return The client.
     */
    public IOneDriveClient getClient() {
        return mClient;
    }

    /**
     * Gets the response type.
     * @return The response type.
     */
    public Class getResponseType() {
        return mResponseClass;
    }
}
