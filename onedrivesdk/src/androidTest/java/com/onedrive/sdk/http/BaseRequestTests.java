package com.onedrive.sdk.http;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.onedrive.sdk.core.MockClient;
import com.onedrive.sdk.extensions.IOneDriveClient;

import junit.framework.Assert;

import java.net.URL;

/**
 * Test cases for (@see BaseRequest)
 */
public class BaseRequestTests extends AndroidTestCase{
    private IOneDriveClient mockClient;
    private BaseRequest mRequest;

    private final String baseUrl = "https://localhost:8080/";
    private final String[] testingSegments = { "Hello World", "你好世界", "Καλημέρα κόσμε", "안녕하세요", "コンニチハ", "แผ่นดินฮั่นเสื่อมโทรมแสนสังเวช" };

    @Override
    public void setUp() {
        mockClient = new MockClient();
        StringBuilder sb = new StringBuilder(baseUrl);

        for (String segment : testingSegments) {
            sb.append(segment);
            sb.append("/");
        }

        sb.deleteCharAt(sb.length()-1);

        mRequest = new BaseRequest(sb.toString(), mockClient, /*options:*/ null, null) {
            @Override
            public IOneDriveClient getClient() {
                return mockClient;
            }
        };
    }
    public void testUrlEncoded() throws Exception {
        URL requestUrl = mRequest.getRequestUrl();
        final Uri.Builder expectBuilder = Uri.parse(baseUrl).buildUpon();

        for (String segment : testingSegments) {
            expectBuilder.appendPath(segment);
        }

        Assert.assertEquals(expectBuilder.build().toString(), requestUrl.toString());
    }
}
