package com.onedrive.sdk.http;

import android.graphics.Path;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.onedrive.sdk.core.MockClient;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.options.Option;
import com.onedrive.sdk.options.QueryOption;

import junit.framework.Assert;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for (@see BaseRequest)
 */
public class BaseRequestTests extends AndroidTestCase{
    private IOneDriveClient mockClient;
    private BaseRequest mRequest;

    private final String baseUrl = "https://localhost:8080";
    private final String[] testingSegments = { "Hello World", "你好世界", "Καλημέρα κόσμε", "안녕하세요", "コンニチハ", "แผ่นดินฮั่นเสื่อมโทรมแสนสังเวช" };

    @Override
    public void setUp() {
        mockClient = new MockClient();
        StringBuilder sb = new StringBuilder(baseUrl);

        for (String segment : testingSegments) {
            sb.append("/");
            sb.append(segment);
        }

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

    public void testUrlWithQuery() throws Exception {
        final String queryInUrl = "expand=foo&$select=id,name";
        final String testUrl = baseUrl + "?" + queryInUrl;

        final List<Option> options = new ArrayList<Option>();
        final QueryOption queryInOption = new QueryOption("queryWithEncode", "!");
        options.add(queryInOption);

        mRequest = new BaseRequest(testUrl, mockClient, options, null) {
            public IOneDriveClient getClient() {
                return mockClient;
            }
        };

        URL requestUrl = mRequest.getRequestUrl();

        final Uri.Builder expectBuilder = Uri.parse(testUrl).buildUpon();
        for (final Option option: options) {
            expectBuilder.appendQueryParameter(option.getName(), option.getValue());
        }

        Assert.assertEquals(expectBuilder.build().toString(), requestUrl.toString());
    }
}
