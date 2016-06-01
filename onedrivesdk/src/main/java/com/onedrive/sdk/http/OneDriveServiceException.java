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

import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.options.HeaderOption;
import com.onedrive.sdk.serializer.ISerializer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An exception from the OneDrive service.
 */
public class OneDriveServiceException extends ClientException {

    /**
     * New line delimiter.
     */
    protected static final char NEW_LINE = '\n';

    /**
     * How truncated values are shown.
     */
    protected static final String TRUNCATION_MARKER = "[...]";

    /**
     * The maximum length for a single line string when trying to be brief.
     */
    protected static final int MAX_BREVITY_LENGTH = 50;

    /**
     * The number of bytes to display when showing byte array.
     */
    protected static final int MAX_BYTE_COUNT_BEFORE_TRUNCATION = 8;

    /**
     * The intent spacing on json based responses.
     */
    public static final int INDENT_SPACES = 3;

    /**
     * The internal server error threshold defined by http protocol.
     */
    public static final int INTERNAL_SERVER_ERROR = 500;

    /**
     * The throwsite header identifier
     */
    protected static final String X_THROWSITE = "x-throwsite";

    /**
     * The response headers.
     */
    private final List<String> mResponseHeaders;

    /**
     * The OneDriveError response.
     */
    private final OneDriveErrorResponse mError;

    /**
     * The http method.
     */
    private final String mMethod;

    /**
     * The request url.
     */
    private final String mUrl;

    /**
     * The request headers.
     */
    private final List<String> mRequestHeaders;

    /**
     * The request body represented as a string.
     */
    private final String mRequestBody;

    /**
     * The http status code.
     */
    private final int mResponseCode;

    /**
     * The http status message.
     */
    private final String mResponseMessage;

    /**
     * Create a OneDrive service exception.
     * @param method The method that caused the exception.
     * @param url The url.
     * @param requestHeaders The request headers.
     * @param requestBody The request body.
     * @param responseCode The response code.
     * @param responseMessage The response message.
     * @param responseHeaders The response headers.
     * @param error The error response if available.
     */
    protected OneDriveServiceException(final String method,
                                       final String url,
                                       final List<String> requestHeaders,
                                       final String requestBody,
                                       final int responseCode,
                                       final String responseMessage,
                                       final List<String> responseHeaders,
                                       final OneDriveErrorResponse error) {
        super(responseMessage, null, null);
        mMethod = method;
        mUrl = url;
        mRequestHeaders = requestHeaders;
        mRequestBody = requestBody;
        mResponseCode = responseCode;
        mResponseMessage = responseMessage;
        mResponseHeaders = responseHeaders;
        mError = error;
    }

    @Override
    public String getMessage() {
        return getMessage(false);
    }

    /**
     * The response headers.
     * @return The list of response headers
     */
    public List<String> getResponseHeaders() {
        return mResponseHeaders;
    }

    /**
     * Gets the message for this exception.
     * @param verbose If the message should be brief or more verbose.
     * @return The message.
     */
    public String getMessage(final boolean verbose) {
        final StringBuilder sb = new StringBuilder();
        if (mError != null && mError.error != null) {
            sb.append("Error code: ").append(mError.error.code).append(NEW_LINE);
            sb.append("Error message: ").append(mError.error.message).append(NEW_LINE);
            sb.append(NEW_LINE);
        }
        // Request information
        sb.append(mMethod).append(' ').append(mUrl).append(NEW_LINE);
        for (final String header : mRequestHeaders) {
            if (verbose) {
                sb.append(header);
            } else {
                final String truncatedHeader = header.substring(0, Math.min(MAX_BREVITY_LENGTH, header.length()));
                sb.append(truncatedHeader);
                if (truncatedHeader.length() == MAX_BREVITY_LENGTH) {
                    sb.append(TRUNCATION_MARKER);
                }
            }
            sb.append(NEW_LINE);
        }
        if (mRequestBody != null) {
            if (verbose) {
                sb.append(mRequestBody);
            } else {
                final int bodyLength = Math.min(MAX_BREVITY_LENGTH, mRequestBody.length());
                final String truncatedBody = mRequestBody.substring(0, bodyLength);
                sb.append(truncatedBody);
                if (truncatedBody.length() == MAX_BREVITY_LENGTH) {
                    sb.append(TRUNCATION_MARKER);
                }
            }
        }
        sb.append(NEW_LINE).append(NEW_LINE);

        // Response information
        sb.append(mResponseCode).append(" : ").append(mResponseMessage).append(NEW_LINE);
        for (final String header : mResponseHeaders) {
            if (verbose) {
                sb.append(header).append(NEW_LINE);
            } else {
                if (header.toLowerCase(Locale.ROOT).startsWith(X_THROWSITE)) {
                    sb.append(header).append(NEW_LINE);
                }
            }
        }
        if (verbose && mError != null && mError.rawObject != null) {
            try {
                final JSONObject jsonObject = new JSONObject(mError.rawObject.toString());
                sb.append(jsonObject.toString(INDENT_SPACES)).append(NEW_LINE);
            } catch (final JSONException ignored) {
                sb.append("[Warning: Unable to parse error message body]").append(NEW_LINE);
                sb.append(mError.rawObject.toString()).append(NEW_LINE);
            }
        } else {
            sb.append(TRUNCATION_MARKER).append(NEW_LINE).append(NEW_LINE);
            sb.append("[Some information was truncated for brevity, enable debug logging for more details]");
        }
        return sb.toString();
    }

    /**
     * Gets the error message from the OneDrive service object.
     * @return The error message.
     */
    public OneDriveError getServiceError() {
        return mError.error;
    }

    @Override
    public boolean isError(final OneDriveErrorCodes expectedCode) {
        if (getServiceError() != null) {
            return getServiceError().isError(expectedCode);
        }
        return false;
    }

    /**
     * Creates a OneDrive service exception from a given failed http request.
     * @param request The request that resulted in this failure.
     * @param serializable The serialized object that was sent with this request.
     * @param serializer The serializer to re-create the option in its over the wire state.
     * @param connection The connection that was used to extract the response information from.
     * @param <T> The type of the serializable object.
     * @return The new OneDriveServiceException instance.
     * @throws IOException An exception occurs if there were any problems processing the connection.
     */
    public static <T> OneDriveServiceException createFromConnection(final IHttpRequest request,
                                                                    final T serializable,
                                                                    final ISerializer serializer,
                                                                    final IConnection connection)
            throws IOException {
        final String method = connection.getRequestMethod();
        final String url = request.getRequestUrl().toString();
        final List<String> requestHeaders = new LinkedList<>();
        for (final HeaderOption option : request.getHeaders()) {
            requestHeaders.add(option.getName() + " : " + option.getValue());
        }
        final String requestBody;
        if (serializable instanceof byte[]) {
            final byte[] bytes = (byte[])serializable;
            StringBuilder sb = new StringBuilder();
            sb.append("byte[").append(bytes.length).append("]");

            sb.append(" {");
            for (int i = 0; i < MAX_BYTE_COUNT_BEFORE_TRUNCATION && i < bytes.length; i++) {
                sb.append(bytes[i]).append(", ");
            }
            if (bytes.length > MAX_BYTE_COUNT_BEFORE_TRUNCATION) {
                sb.append(TRUNCATION_MARKER).append("}");
            }
            requestBody = sb.toString();
        } else if (serializable != null) {
            requestBody = serializer.serializeObject(serializable);
        } else {
            requestBody = null;
        }

        final int responseCode = connection.getResponseCode();
        final List<String> responseHeaders = new LinkedList<>();
        final Map<String, String> headers = connection.getHeaders();
        for (final String key : headers.keySet()) {
            final String fieldPrefix;
            if (key == null) {
                fieldPrefix = "";
            } else {
                fieldPrefix = key + " : ";
            }
            responseHeaders.add(fieldPrefix + headers.get(key));
        }

        final String responseMessage = connection.getResponseMessage();
        final String rawOutput = DefaultHttpProvider.streamToString(connection.getInputStream());
        OneDriveErrorResponse error = null;
        Exception parsingException = null;

        final String contentType = headers.get(DefaultHttpProvider.CONTENT_TYPE_HEADER_NAME);
        if (contentType != null && contentType.contains(DefaultHttpProvider.JSON_CONTENT_TYPE)) {
            try {
                error = serializer.deserializeObject(rawOutput, OneDriveErrorResponse.class);
            } catch (final Exception ex) {
                parsingException = ex;
            }
        }

        if (error == null) {
            error = new OneDriveErrorResponse();
            error.error = new OneDriveError();
            error.error.code = "Unable to parse error response message";
            error.error.message = "Raw error: " + rawOutput;
            if (parsingException != null) {
                error.error.innererror = new OneDriveInnerError();
                error.error.innererror.code = parsingException.getMessage();
            }
        }

        if (responseCode == INTERNAL_SERVER_ERROR) {
            return new OneDriveFatalServiceException(method,
                                                     url,
                                                     requestHeaders,
                                                     requestBody,
                                                     responseCode,
                                                     responseMessage,
                                                     responseHeaders,
                                                     error);
        }

        return new OneDriveServiceException(method,
                                            url,
                                            requestHeaders,
                                            requestBody,
                                            responseCode,
                                            responseMessage,
                                            responseHeaders,
                                            error);
    }
}
