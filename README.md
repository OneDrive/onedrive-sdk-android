# OneDrive SDK for Android

[ ![Download](https://api.bintray.com/packages/onedrive/Maven/onedrive-sdk-android/images/download.svg) ](https://bintray.com/onedrive/Maven/onedrive-sdk-android/_latestVersion)
[![Build Status](https://travis-ci.org/OneDrive/onedrive-sdk-android.svg?branch=master)](https://travis-ci.org/OneDrive/onedrive-sdk-android)

Integrate the [OneDrive API](https://dev.onedrive.com/README.htm) into your Android application!

## 1. Installation
### 1.1 Install AAR via Gradle
Add the maven central repository to your projects build.gradle file then add a compile dependency for com.onedrive.sdk:onedrive-sdk-android:1.2+

```gradle
repository {
    jcenter()
}

dependency {
    // Include the sdk as a dependency
    compile ('com.onedrive.sdk:onedrive-sdk-android:1.2+') {
        transitive = false
    }

    // Include the gson dependency
    compile ('com.google.code.gson:gson:2.3.1')

    // Include supported authentication methods for your application
    compile ('com.microsoft.services.msa:msa-auth:0.8.+')
    compile ('com.microsoft.aad:adal:1.1.+')
}
```

## 2. Getting started

### 2.1 Register your application

Register your application by following [these](https://dev.onedrive.com/app-registration.htm) steps.

### 2.2 Set your application Id and scopes

The OneDrive SDK for Android comes with Authenticator objects that have already been initialized for OneDrive with Microsoft accounts and Azure Activity Directory accounts. Replace the current settings with the required settings to start authenticating.

Note that your _msa-client-id_ should look like `0000000000000000` and _adal-client-id_ should look like `00000000-0000-0000-0000-000000000000`.

```java
final MSAAuthenticator msaAuthenticator = new MSAAuthenticator() {
    @Override
    public String getClientId() {
        return "<msa-client-id>";
    }

    @Override
    public String[] getScopes() {
        return new String[] { "onedrive.appfolder" };
    }
}

final ADALAuthenticator adalAuthenticator = new ADALAuthenticator() {
    @Override
    public String getClientId() {
        return "<adal-client-id>";
    }

    @Override
    protected String getRedirectUrl() {
        return "https://localhost";
    }
}
```

### 2.3 Get a OneDriveClient object

Once you have set the correct application Id and scopes, you must get a **OneDriveClient** object to make requests against the service. The SDK will store the account information for you, but when a user logs on for the first time, it will invoke UI to get the user's account information.

```java
final IClientConfig oneDriveConfig = DefaultClientConfig.createWithAuthenticators(
                                            msaAuthenticator,
                                            adalAuthenticator);

final IOneDriveClient oneDriveClient = new OneDriveClient.Builder()
                                            .fromConfig(oneDriveConfig)
                                            .loginAndBuildClient(getActivity());

```

## 3. Make requests against the service

Once you have an OneDriveClient that is authenticated you can begin making calls against the service. The requests against the service look like our [REST API](https://dev.onedrive.com/README.htm).

### Get the drive

To retrieve a user's drive:

```java
oneDriveClient
    .getDrive()
    .buildRequest()
    .get(new ICallback<Drive>() {
  @Override
  public void success(final Drive result) {
    final String msg = "Found Drive " + result.id;
    Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT)
        .show();
  }
  ...
  // Handle failure case
});
```

### Get the root folder

To get a user's root folder of their drive:

```java
oneDriveClient
    .getDrive()
    .getRoot()
    .buildRequest()
    .get(new ICallback<Item>() {
  @Override
  public void success(final Item result) {
    final String msg = "Found Root " + result.id;
    Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT)
        .show();
  }
  ...
  // Handle failure case
});
```

For a general overview of how the SDK is designed, see [overview](docs/overview.md).

## 4. Documentation

For a more detailed documentation see:

* [Overview](docs/overview.md)
* [Authentication](docs/authentication.md)
* [Extensibility](docs/extensibility.md)
* [Items](docs/items.md)
* [Collections](docs/collections.md)
* [Errors](docs/errors.md)
* [Contributions](docs/contributions.md)

## 5. Issues

For known issues, see [issues](https://github.com/OneDrive/onedrive-sdk-android/issues).

## 6. Contributions

The OneDrive SDK is open for contribution. Please read how to contribute to this project [here](docs/contributions.md).

## 7. Supported Android Versions
The OneDrive SDK for Android library is supported at runtime for [Android API revision 15](http://source.android.com/source/build-numbers.html) and greater. To build the sdk you need to install Android API revision 23 or greater.

## 8. License

[License](LICENSE)

## 9. Third Party Notices

[Third Party Notices](THIRD PARTY NOTICES)
