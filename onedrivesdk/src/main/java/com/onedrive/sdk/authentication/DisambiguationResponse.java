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
 * The response for the disambiguation request.
 */
class DisambiguationResponse {

    /**
     * The type of account.
     */
    private final AccountType mAccountType;

    /**
     * The account's email address.
     */
    private final String mAccount;

    /**
     * Creates the disambiguation response.
     * @param accountType The account type.
     * @param account The account email.
     */
    public DisambiguationResponse(final AccountType accountType, final String account) {
        mAccountType = accountType;
        mAccount = account;
    }

    /**
     * Gets the account type.
     * @return The account type.
     */
    public AccountType getAccountType() {
        return mAccountType;
    }

    /**
     * Gets the account email address.
     * @return The account email address.
     */
    public String getAccount() {
        return mAccount;
    }
}
