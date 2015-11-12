# Authenticating with the OneDrive SDK for Android
The OneDrive SDK requires that all requests are authenticated with OneDrive. This SDK is built to make it easy to incorporate any application with the authentication provider.

## Authenticators

Authenticators are provided with the SDK to handle authentication settings.

### Disambiguation Authenticator
In order to support as many different users as possible with a single application, we supplied a disambiguation authenticator to provide a user experience that determines which authenticator that user requires. This is the recommended way to distribute your application.

### MSA Authenticator
Use the **MSAAuthenticator** object for any account that needs to authenticate with the Microsoft account service.

Once you have your client id, you need to determine the appropriate scopes of authority that your application will need. For a complete list of scopes, consult the OAuth [documentation](https://dev.onedrive.com/auth/msa_oauth.htm#authentication-scopes).

* For applications that need to store data, use the __onedrive.appfolder__ scope and interact with /drive/special/approot.
* For applications that need to read only the contents of user's OneDrive, request the __onedrive.readonly__ scope.

Your client id should be formatted like `0000000000000000`.
```java
final MSAAuthenticator msaAuthenticator = new MSAAuthenticator {
    @Override
    public String getClientId() {
        return "0000000000000000";
    }

    @Override
    public String[] getScopes() {
        return new String[] { "onedrive.appfolder" };
    }
}
```

### ADAL Authenticator
Use the **ADALAuthenticator** object for accounts that are hosted on Azure Active Directory.

After you've [configured](https://dev.onedrive.com/auth/aad_oauth.htm) your service to allow access to an application for OneDrive, you will need create an **ADALAuthenticator** object with the client id and the redirect url in your application. Your client id should be formatted like `00000000-0000-0000-0000-000000000000`.

```java
final ADALAuthenticator adalAuthenticator = new ADALAuthenticator {
    @Override
    public String getClientId() {
        return "<client_id>";
    }

    @Override
    protected String getRedirectUrl() {
        return "https://localhost";
    }
}
```

## Using Authenticators in your application
Creating the **OneDriveClient** object enables the silent or interactive login appropriately, but the only functionality that needs be implemented is the sign out user experience. All the provided authenticators will clear preserved tokens after a sign out has completed, ensuring your application has a clean state.

```java
oneDriveClient.getAuthenticator().logout(new ICallback<Void>() {
    @Override
    public void success(final Void result) {
        // Handle any state change your application needs to undergo
    }
    ...
    // Handle failure
```
