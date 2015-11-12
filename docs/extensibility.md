# Extensibility with the OneDrive SDK for Android

The OneDrive SDKs are built to allow for customization and enhancement over time while maintaining backwards compatibility.

## Contribution

Contributions are welcome on the OneDrive SDKs. To contribute, please open a [new issue](https://github.com/OneDrive/onedrive-sdk-android/issues/new) to start a dialog with the team about what you are creating.

__Note__ There is an area of the SDK which is not available for direct contribution, which is anything under the com.onedrive.sdk.generated package. These components are built using automated tools and will be overwritten. As this tooling matures this process will be open to contributions as well.

## Minor modification

OneDrive's service is described in accordance with OData. This service description is [available](https://api.onedrive.com/v1.0/$metadata) and continuously updated as new features become available. However, the OneDrive $metadata description does not encompass all features and functionality. To aid with elements of the service that are not described here, the `com.onedrive.sdk.extensions` package was created. The files are all generated automatically but will not be overwritten. Some functionality is already exposed in this manner, and here is example using the `OneDriveClient`.

The class hierarchy:

```java
class OneDriveClient extends BaseOneDriveClient implements IOneDriveClient {}
class BaseOneDriveClient extends BaseClient implements IBaseOneDriveClient {}
class BaseClient implements IBaseClient {}
```
The companion interface hierarchy:

```java
interface IOneDriveClient extends IBaseOneDriveClient {}
interface IBaseOneDriveClient extends IBaseClient {}
interface IBaseClient {}
```

- The `BaseClient` layer represents the under-pinnings of an OData client. These objects are open for modification and additions.
- The `BaseOneDriveClient` layer is supplied by the OData $metadata description, and is automatically populated with the features described therein.
- The `OneDriveClient` layer is where extra functionality above and beyond the $metadata description can be placed.

The OneDrive team keeps the interfaces up to date to detect backwards compatibility breaking changes.

Most classes generated at this 'top tier' will be empty to support future modification, and OneDriveClient already has been modified to support the 'drive()' behavior.  'drive' is exposed to shorten the `{service-root}/drives/{default-drive-id}` into `{service-root}/drive`.

## Major dependencies

During the construction of the `IOneDriveClient` object an `IClientConfig` is used. This configuration determines how the internal functionality of the SDK is supplied. By creating a new  `IClientConfig` implementation the components can be modified or wholly replaced to best meet the needs of the caller.

### IHttpProvider

Provides the http fabric that is used for all network requests within the SDK.

### IAuthenticator

Provides the facilities to authenticate users and supply an authentication token for requests to the service.

### ISerializer

Serializes and deserializes the structured object from the service.

### IExecutors

Handles executing tasks for the SDK on foreground and background threads.

### ILogger

The logging system used by the SDK to report debug and error messages for debugging purposes.
