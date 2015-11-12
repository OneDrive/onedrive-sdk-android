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

package com.onedrive.sdk.authentication;

/**
 * Represents account information used to communicate with the OneDrive service.
 */
public interface IAccountInfo {

    /**
     * Gets the type of the account.
     * @return The account type.
     */
    AccountType getAccountType();

    /**
     * Gets the access token for requests against the service root.
     * @return The access token for requests against the service root.
     */
    String getAccessToken();

    /**
     * Gets the OneDrive service root for this account.
     * @return The OneDrive service root for this account.
     */
    String getServiceRoot();

    /**
     * Indicates if the account access token is expired and needs to be refreshed.
     * @return true if the refresh() method needs to be called and
     *         false if the account is still valid.
     */
    boolean isExpired();

    /**
     * Refreshes the authentication token for this account info.
     */
    void refresh();
}
