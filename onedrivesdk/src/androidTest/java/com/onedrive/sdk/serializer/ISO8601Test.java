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

import junit.framework.Assert;

import android.test.AndroidTestCase;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Test cases for the {@see ISO8601} class
 */
public class ISO8601Test extends AndroidTestCase {

    /**
     * Make sure that dates with and without millis can be converted properly into strings
     * @throws Exception If there is an exception during the test
     */
    public void testFromDate() throws Exception {
        // I sure hope this works in other timezones...
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        final Calendar date = Calendar.getInstance();
        date.setTime(new Date(123456789012345L));
        Assert.assertEquals("5882-03-11T00:30:12.345Z", CalendarSerializer.serialize(date));

        final Calendar dateNoMillis = Calendar.getInstance();
        dateNoMillis.setTime(new Date(123456789012000L));
        Assert.assertEquals("5882-03-11T00:30:12.000Z", CalendarSerializer.serialize(dateNoMillis));
    }

    /**
     * Make sure that dates in string format with and without millis can be converted properly into date objects
     * @throws Exception If there is an exception during the test
     */
    public void testToDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        final long toTheSecondDate = 123456789012000L;
        final Calendar dateToSecond = CalendarSerializer.deserialize("5882-03-11T00:30:12Z");
        Assert.assertEquals(toTheSecondDate, dateToSecond.getTimeInMillis());

        final long toTheMillisecondDate = 123456789012345L;
        final Calendar dateToTheMillisecond = CalendarSerializer.deserialize("5882-03-11T00:30:12.345Z");
        Assert.assertEquals(toTheMillisecondDate, dateToTheMillisecond.getTimeInMillis());

        final Calendar dateToTheExtremeMillisecond = CalendarSerializer.deserialize("5882-03-11T00:30:12.3456789Z");
        Assert.assertEquals(toTheMillisecondDate, dateToTheExtremeMillisecond.getTimeInMillis());
    }
}
