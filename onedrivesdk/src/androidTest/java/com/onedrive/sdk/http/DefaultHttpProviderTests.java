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
import com.onedrive.sdk.concurrency.AsyncMonitorResponseHandler;
import com.onedrive.sdk.concurrency.ChunkedUploadResponseHandler;
import com.onedrive.sdk.concurrency.IProgressCallback;
import com.onedrive.sdk.concurrency.MockExecutors;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.extensions.AsyncOperationStatus;
import com.onedrive.sdk.extensions.ChunkedUploadResult;
import com.onedrive.sdk.extensions.Drive;
import com.onedrive.sdk.extensions.Item;
import com.onedrive.sdk.extensions.UploadSession;
import com.onedrive.sdk.logger.MockLogger;
import com.onedrive.sdk.serializer.MockSerializer;

import android.test.AndroidTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for {@see DefaultHttpProvider}
 */
public class DefaultHttpProviderTests extends AndroidTestCase {

    private MockInterceptor mInterceptor;
    private DefaultHttpProvider mProvider;

    public void testAsyncSessionResult() throws Exception {
        final String expectedLocation = "http://localhost:1234";
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 200;
            }

            @Override
            public String getJsonResponse() {
                return "{ \"operation\": \"Copy\", \"percentageComplete\": 100.0, \"status\": \"inProgress\" }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> map = new HashMap<>();
                map.put("Location", expectedLocation);
                return map;
            }
        };
        setDefaultHttpProvider(new AsyncOperationStatus());
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        AsyncOperationStatus response = mProvider.send(new MockRequest(), AsyncOperationStatus.class, null, new AsyncMonitorResponseHandler());

        assertEquals(expectedLocation, response.seeOther);
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testAsyncSessionResultCompleted() throws Exception {
        final String expectedLocation = "http://localhost:1111";
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 303;
            }

            @Override
            public String getJsonResponse() {
                return "";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> map = new HashMap<>();
                map.put("Location", expectedLocation);
                return map;
            }
        };
        setDefaultHttpProvider(new AsyncOperationStatus());
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        AsyncOperationStatus response = mProvider.send(new MockRequest(), AsyncOperationStatus.class, null, new AsyncMonitorResponseHandler());

        assertEquals(expectedLocation, response.seeOther);
        assertEquals("Completed", response.status);
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testNoContentType() throws Exception {
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 200;
            }

            @Override
            public String getJsonResponse() {
                return "{ \"id\": \"zzz\" }";
            }

            @Override
            public Map<String, String> getHeaders() {
                return new HashMap<>();
            }
        };
        setDefaultHttpProvider(null);
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        try {
            mProvider.send(new MockRequest(), Drive.class, null);
            fail("Expected exception");
        } catch (final ClientException ce) {
            if (!(ce.getCause() instanceof NullPointerException)) {
                fail("Wrong inner exception!");
            }
        }
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testDriveResponse() throws Exception {
        final String driveId = "driveId";
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 200;
            }

            @Override
            public String getJsonResponse() {
                return "{ \"id\": \"zzz\" }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> map = new HashMap<>();
                map.put("Content-Type", "application/json");
                return map;
            }
        };
        final Drive expectedDrive = new Drive();
        expectedDrive.id = driveId;
        setDefaultHttpProvider(expectedDrive);
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        final Drive drive = mProvider.send(new MockRequest(), Drive.class, null);

        assertEquals(driveId, drive.id);
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testBinaryResponse() throws Exception {
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 200;
            }

            @Override
            public String getJsonResponse() {
                return "{ \"id\": \"zzz\" }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> map = new HashMap<>();
                map.put("Content-Type", "application/octet-stream");
                return map;
            }
        };
        setDefaultHttpProvider(null);
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));
        mProvider.send(new MockRequest(), InputStream.class, null);
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testPostItem() throws Exception {
        final String itemId = "itemId";
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 200;
            }

            @Override
            public String getJsonResponse() {
                return "{ \"id\": \"zzz\" }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> map = new HashMap<>();
                map.put("Content-Type", "application/json");
                return map;
            }
        };

        final Item expectedItem = new Item();
        expectedItem.id = itemId;
        setDefaultHttpProvider(expectedItem);
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        final Item item = mProvider.send(new MockRequest(), Item.class, new Item());

        assertEquals(itemId, item.id);
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testPostByte() throws Exception {
        final String itemId = "itemId";
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 200;
            }

            @Override
            public String getJsonResponse() {
                return "{ \"id\": \"zzz\" }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> map = new HashMap<>();
                map.put("Content-Type", "application/json");
                return map;
            }
        };
        final Item expectedItem = new Item();
        expectedItem.id = itemId;
        setDefaultHttpProvider(expectedItem);
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        final AtomicBoolean progress = new AtomicBoolean(false);
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicBoolean failure = new AtomicBoolean(false);
        final IProgressCallback<Item> progressCallback = new IProgressCallback<Item>() {
            @Override
            public void progress(final long current, final long max) {
                progress.set(true);
            }

            @Override
            public void success(final Item item) {
                success.set(true);
            }

            @Override
            public void failure(final ClientException ex) {
                failure.set(true);
            }
        };

        mProvider.send(new MockRequest(), progressCallback, Item.class, new byte[]{1, 2, 3, 4});

        assertTrue(progress.get());
        assertTrue(success.get());
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testErrorResponse() throws Exception {
        final OneDriveErrorCodes expectedErrorCode = OneDriveErrorCodes.InvalidRequest;
        final String expectedMessage = "Test error!";
        final OneDriveErrorResponse toSerialize = new OneDriveErrorResponse();
        toSerialize.error = new OneDriveError();
        toSerialize.error.code = expectedErrorCode.toString();
        toSerialize.error.message = expectedMessage;
        toSerialize.error.innererror = null;

        setDefaultHttpProvider(toSerialize);
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 415;
            }

            @Override
            public String getJsonResponse() {
                return "{}";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        try {
            mProvider.send(new MockRequest(), Item.class, null);
            fail("Expected exception in previous statement");
        } catch (final OneDriveServiceException e) {
            assertTrue(e.isError(expectedErrorCode));
            assertEquals(expectedMessage, e.getServiceError().message);
        }
    }

    public void testBodyLessResponse() throws Exception {
        final int[] codes = new int[] {204, 304 };
        final AtomicInteger currentCode = new AtomicInteger(0);
        setDefaultHttpProvider(null);
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return codes[currentCode.get()];
            }

            @Override
            public String getJsonResponse() {
                throw new UnsupportedOperationException("Should not ever hit this");
            }

            @Override
            public Map<String, String> getHeaders() {
                return new HashMap<>();
            }
        };
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        for (final int ignored : codes) {
            Item result = mProvider.send(new MockRequest(), Item.class, null);
            currentCode.incrementAndGet();
            assertNull(result);
        }
        assertEquals(codes.length, mInterceptor.getInterceptionCount());
    }

    public void testMonitorCreation() throws Exception {
        final String expectedLocation = "http://localhost/monitorlocation";
        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 202;
            }

            @Override
            public String getJsonResponse() {
                return "{ }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> map = new HashMap<>();
                map.put("Location", expectedLocation);
                return map;
            }
        };

        setDefaultHttpProvider(new AsyncMonitorLocation(""));
        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));

        final AsyncMonitorLocation monitorLocation = mProvider.send(new MockRequest(), AsyncMonitorLocation.class, new Item());

        assertEquals(expectedLocation, monitorLocation.getLocation());
        assertEquals(1, mInterceptor.getInterceptionCount());
    }

    public void testUploadReturnNextSession() throws  Exception {
        final String expectedLocation = "http://localhost/up/uploadlocation";
        final byte[] chunk = new byte[100];
        final UploadSession<Item> toSerialize = new UploadSession<Item>();
        toSerialize.uploadUrl = expectedLocation;
        toSerialize.nextExpectedRanges = Arrays.asList("100-199");
        setDefaultHttpProvider(toSerialize);

        final ChunkedUploadResponseHandler<Item> handler = new ChunkedUploadResponseHandler(Item.class);

        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 202;
            }

            @Override
            public String getJsonResponse() {
                return "{ }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));
        ChunkedUploadResult result = mProvider.send(new MockRequest(), ChunkedUploadResult.class, chunk, handler);

        assertTrue(result.chunkCompleted());
        assertEquals(result.getSession(), toSerialize);
    }

    public void testUploadReturnUploadedItem() throws  Exception {
        final String expectedLocation = "http://localhost/up/uploadlocation";
        final byte[] chunk = new byte[30];
        final Item toSerialize = new Item();
        toSerialize.id = "abc!123";
        setDefaultHttpProvider(toSerialize);

        final ChunkedUploadResponseHandler<Item> handler = new ChunkedUploadResponseHandler(Item.class);

        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 201;
            }

            @Override
            public String getJsonResponse() {
                return "{ }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));
        ChunkedUploadResult result = mProvider.send(new MockRequest(), ChunkedUploadResult.class, chunk, handler);

        assertTrue(result.uploadCompleted());
        assertEquals(toSerialize, result.getItem());
    }

    public void testUploadReturnError() throws  Exception {
        final String expectedLocation = "http://localhost/up/uploadlocation";
        final byte[] chunk = new byte[30];
        final OneDriveErrorCodes errorCode = OneDriveErrorCodes.UploadSessionFailed;
        final OneDriveError toSerialize = new OneDriveError();
        toSerialize.code = errorCode.toString();
        setDefaultHttpProvider(toSerialize);

        final ChunkedUploadResponseHandler<Item> handler = new ChunkedUploadResponseHandler(Item.class);

        final ITestData data = new ITestData() {
            @Override
            public int getRequestCode() {
                return 500;
            }

            @Override
            public String getJsonResponse() {
                return "{ }";
            }

            @Override
            public Map<String, String> getHeaders() {
                final HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        mProvider.setConnectionFactory(new MockSingleConnectionFactory(new TestDataConnection(data)));
        ChunkedUploadResult result = mProvider.send(new MockRequest(), ChunkedUploadResult.class, chunk, handler);

        assertFalse(result.chunkCompleted());
        assertTrue(result.getError().isError(errorCode));
    }

    /**
     * Mock {@see IConnection} backed with test data
     */
    private class TestDataConnection implements IConnection {

        private final ITestData mData;

        public TestDataConnection(ITestData data) {
            mData = data;
        }

        @Override
        public void setFollowRedirects(final boolean followRedirects) {

        }

        @Override
        public void addRequestHeader(final String headerName, final String headerValue) {

        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(mData.getJsonResponse().getBytes());
        }

        @Override
        public int getResponseCode() throws IOException {
            return mData.getRequestCode();
        }

        @Override
        public String getResponseMessage() throws IOException {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public Map<String, String> getHeaders() {
            return mData.getHeaders();
        }

        @Override
        public String getRequestMethod() {
            return null;
        }

        @Override
        public void setContentLength(int length) {

        }
    }

    /**
     * Test data to use in configuring the mock connection object
     */
    private interface ITestData {
        int getRequestCode();

        String getJsonResponse();

        Map<String,String> getHeaders();
    }

    /**
     * Configures the http provider for test cases
     * @param toSerialize The object to serialize
     */
    private void setDefaultHttpProvider(final Object toSerialize) {
        mProvider = new DefaultHttpProvider(new MockSerializer(toSerialize, ""),
                mInterceptor = new MockInterceptor(),
                new MockExecutors(),
                new MockLogger());
    }
}
