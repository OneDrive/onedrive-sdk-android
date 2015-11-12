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

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An executor that runs only on the main thread of an Android application.
 */
public class SynchronousExecutor implements Executor {

    /**
     * The current number of synchronously executing actions.
     */
    private AtomicInteger mActiveCount = new AtomicInteger(0);

    /**
     * Executes the given Runnable task.
     * @param runnable The task to run on the main thread.
     */
    @Override public void execute(@NonNull final Runnable runnable) {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                return null;
            }
            @Override
            protected void onPostExecute(final Void result) {
                mActiveCount.incrementAndGet();
                runnable.run();
                mActiveCount.decrementAndGet();
            }
        };
        asyncTask.execute();
    }

    /**
     * Get the account number of executing actions.
     * @return The count.
     */
    public int getActiveCount() {
        return mActiveCount.get();
    }
}
