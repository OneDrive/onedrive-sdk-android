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
 * The type of account.
 */
public enum AccountType {

    /**
     * A Microsoft account.
     */
    MicrosoftAccount("MSA"),

    /**
     * An Azure Active Directory account.
     */
    ActiveDirectory("AAD");

    /**
     * The shorthand string representation from the disambiguation service.
     */
    private final String[] mRepresentations;

    /**
     * Creates an AccountType object.
     * @param representations The shorthand string representation from the disambiguation service.
     */
    AccountType(final String... representations) {
        mRepresentations = representations;
    }

    /**
     * Gets an AccountType object from a string representation.
     * @param representation The shorthand string representation from the disambiguation service.
     * @return The account type for this string representation.
     */
    public static AccountType fromRepresentation(final String representation) {
        for (final AccountType accountType : AccountType.values()) {
            for (final String possibleRepresentation : accountType.mRepresentations) {
                if (possibleRepresentation.equalsIgnoreCase(representation)) {
                    return accountType;
                }
            }
        }

        final String message = String.format("Unable to find a representation for '%s", representation);
        throw new UnsupportedOperationException(message);
    }
}
