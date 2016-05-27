package com.onedrive.sdk.core;

import com.onedrive.sdk.authentication.IAuthenticator;
import com.onedrive.sdk.concurrency.IExecutors;
import com.onedrive.sdk.extensions.IDriveCollectionRequestBuilder;
import com.onedrive.sdk.extensions.IDriveRequestBuilder;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.IShareCollectionRequestBuilder;
import com.onedrive.sdk.extensions.IShareRequestBuilder;
import com.onedrive.sdk.http.IHttpProvider;
import com.onedrive.sdk.logger.ILogger;
import com.onedrive.sdk.serializer.ISerializer;

public class MockClient implements IOneDriveClient{
    private ILogger mLogger;

    @Override
    public IDriveRequestBuilder getDrive() {
        return null;
    }

    @Override
    public IDriveCollectionRequestBuilder getDrives() {
        return null;
    }

    @Override
    public IDriveRequestBuilder getDrive(String id) {
        return null;
    }

    @Override
    public IShareCollectionRequestBuilder getShares() {
        return null;
    }

    @Override
    public IShareRequestBuilder getShare(String id) {
        return null;
    }

    @Override
    public IAuthenticator getAuthenticator() {
        return null;
    }

    @Override
    public String getServiceRoot() {
        return null;
    }

    @Override
    public IExecutors getExecutors() {
        return null;
    }

    @Override
    public IHttpProvider getHttpProvider() {
        return null;
    }

    @Override
    public ILogger getLogger() {
        return mLogger;
    }

    @Override
    public ISerializer getSerializer() {
        return null;
    }

    @Override
    public void validate() {

    }
}
