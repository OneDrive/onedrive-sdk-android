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

package com.onedrive.sdk.serializer;

import com.onedrive.sdk.extensions.Drive;
import com.onedrive.sdk.logger.DefaultLogger;

import android.test.AndroidTestCase;

/**
 * Test cases for the {@see DefaultSerializer}
 */
public class DefaultSerializerTests extends AndroidTestCase  {

    /**
     * Make sure that deserializing a Drive also returns members from BaseDrive
     * @throws Exception If there is an exception during the test
     */
    public void driveDeserialization() throws Exception {
        final DefaultSerializer serializer = new DefaultSerializer(new DefaultLogger());
        String source = "{\"@odata.context\":\"https://api.onedrive.com/v1.0/$metadata#drives/$entity\",\"id\":\"8bf6ae90006c4a4c\",\"driveType\":\"personal\",\"owner\":{\"user\":{\"displayName\":\"Peter\",\"id\":\"8bf6ae90006c4a4c\"}},\"quota\":{\"deleted\":1485718314,\"remaining\":983887466461,\"state\":\"normal\",\"total\":1142461300736,\"used\":158573834275}}";
        Drive result = serializer.deserializeObject(source, Drive.class);
        assertNotNull(result);
        assertEquals("personal", result.driveType);
        assertEquals(Long.valueOf(983887466461L), result.quota.remaining);
        assertEquals("8bf6ae90006c4a4c", result.id);

    }
}
