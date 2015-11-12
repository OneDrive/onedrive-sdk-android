# Handling errors in the OneDrive SDK for Android

Errors in the OneDrive SDK for Android behave just like errors returned from the service. You can read more about them [here](https://github.com/OneDrive/onedrive-api-docs/blob/master/misc/errors.md).

Anytime you make a request against the service there is the potential for an error. You will see that all requests to the service can return an error. The errors are returned as `ClientExcepion`, with possible subclasses `ClientAuthenticationException` and `OneDriveServiceException` which your application will want to handle.

## Checking the error

There are a few different types of errors that can occur during a network call. We have provided some helper methods to make it easy to check what kind of error occurred. These error types are defined in [OneDriveErrorCodes.java](../OneDriveSDK/src/main/java/com/onedrive/sdk/core/OneDriveErrorCodes.java).

```java
try {
    // ...
} catch (final ClientExcepion ex) {
    if (ex.isError(OneDriveErrorCodes.AuthenticationCancelled)) {
        // Handle the specific authentication cancelled case
    }
    // Handle the authentication exception
}
```

### Client authentication exceptions

These exceptions represent errors during the authentication flow. The two exceptions are `AuthenticationCancelled` for interactive user cancelation and `AuthenticationFailure` for a problem with the underlying authentication system.

### OneDrive service exceptions

These are exceptions from the OneDrive service, that contain extra error diagnostic information. The standard error codes should give your application more than enough detail to message users. However, there is useful debug information contained in the response.

__Note__: Sometimes you might see a `OneDriveFatalServiceException`. If you do, please open a [new issue](https://github.com/OneDrive/onedrive-sdk-android/issues/new) so that we can fix it.
