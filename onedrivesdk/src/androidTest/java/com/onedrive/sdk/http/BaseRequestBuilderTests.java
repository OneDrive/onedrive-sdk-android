package com.onedrive.sdk.http;

import android.test.AndroidTestCase;

import com.onedrive.sdk.core.MockClient;
import com.onedrive.sdk.extensions.IOneDriveClient;

import junit.framework.Assert;

import java.net.URLEncoder;

/**
 * Test cases for (@see BaseRequestBuilder)
 */
public class BaseRequestBuilderTests extends AndroidTestCase{
    private BaseRequestBuilder mRequestBuilder;
    private IOneDriveClient mockClient;
    private final String baseUrl = "https://localhost:8080/";
    private final String[] testingSegments = {"Hello World", "你好世界", "Καλημέρα κόσμε", "안녕하세요", "コンニチハ", "แผ่นดินฮั่นเสื่อมโทรมแสนสังเวช"};

    @Override
    public void setUp() {
        mockClient = new MockClient();
        mRequestBuilder = new BaseRequestBuilder(baseUrl, mockClient, /*options:*/ null) {
            @Override
            public IOneDriveClient getClient() {
                return mockClient;
            }
        };
    }
    public void testGetRequestUrlWithAdditionalSegment() throws Exception {
        for (String segment: testingSegments) {
            Assert.assertEquals(baseUrl + "/" + URLEncoder.encode(segment, "UTF-8"), mRequestBuilder.getRequestUrlWithAdditionalSegment(segment));
        }
    }
}
