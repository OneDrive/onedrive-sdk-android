# Overview for the OneDrive SDK for Android
The OneDrive SDK for Android is designed to look just like the OneDrive API.

## OneDriveClient
A `OneDriveClient` is always associated with a authentication session. This can be created by using the **Builder** subclass of the `OneDriveClient` implementation, `loginAndBuildClient(..)`.  All requests use this object to send information to the service and it should be constructed once for your applications life cycle.

## Resource model
Resources, like [items](/docs/items.md) or drives, are represented by `Item` and `Drive`, respectively. These objects contain properties that represent the properties of a resource. These objects can't make calls against the service-they are purely models.

To get the name of an item you address the `name` property. It is possible for any of these properties to be null at any time. To check if an item is a folder you address the `folder` property. If the item is a folder an `ODFolder` facet on that object will be returned, and it contains all of the properties described by the [folder](https://github.com/OneDrive/onedrive-api-docs/blob/master/facets/folder_facet.md) facet.

See [Resource model](https://github.com/onedrive/onedrive-api-docs/#resource-model) for more info.

The resources that are generated map to the json model described by the $metadata service document. There are items that might not be exposed because they expire very quickly or represent functionality that is not featured in this SDK as of yet. To access these fields use `getRawObject()` on the model items and access the specific properties that are not in the model.

## Issuing requests
To make requests against the service, first build a request with **RequestBuilder** and then build it into a Request object, which is then sent against the service. This follows the URL scheme that the OneDrive API uses for all its resources.

### Request builders
To generate requests you chain together calls on request builder objects. You get the first request builder from the `OneDriveClient` object. To get a drive, create a request builder by calling **OneDriveClient.getDrive**.

|Task            | SDK               | URL                             |
|:---------------|:-----------------:|:--------------------------------|
|Get a drive     | `oneDriveClient.getDrive()` | GET api.onedrive.com/v1.0/drive/|

`getDrive` will return an `IDriveRequestBuilder` object. From `getDrive`, you can continue to chain the requests to get everything else in the API, like an item.

|Task            | SDK                                | URL                                       |
|:---------------|:----------------------------------:|:------------------------------------------|
|Get an item     | `oneDriveClient.getDrive().getItems("1234")` | GET api.onedrive.com/v1.0/drive/items/1234|


Here, `oneDriveClient.getDrive()` returns an `IDriveRequestBuilder` that contains a method `getItems(...)` to get an `IItemRequestBuilder`.

Similarly, to get thumbnails, you chain together the request builders `getThumbnails` and `getItems`.

|Task            | SDK                            | URL                      |
|----------------|--------------------------------|--------------------------|
| Get thumbnails | `... .getItems("1234").getThumbnails()` | .../items/1234/thumbnails|


Here, `oneDriveClient.getDrive().getItems("1234")` returns an `IItemRequestBuilder` that contains the method `getThumbnails()`.

This returns a collection of [thumbnail sets](https://github.com/OneDrive/onedrive-api-docs/blob/master/resources/thumbnailSet.md). To index the collection directly you can call:

|Task               | SDK                                 | URL                        |
|-------------------|-------------------------------------|----------------------------|
| Get thumbnail Set |  `... .getItems("1234").getThumbnails("0")` | ...items/1234/thumbnails/0 |

To return a thumbnail set, and to get a specific [thumbnail](https://github.com/OneDrive/onedrive-api-docs/blob/master/resources/thumbnail.md), you can add the name of the thumbnail to the URL like this:

|Task             | SDK                         | URL                    |
|-----------------|-----------------------------|------------------------|
| Get a thumbnail | `... .getThumbnails("0").getThumbnailSize("small")` | .../thumbnails/0/small |

### Requests
Once you have constructed the request you call the `buildRequest()` method on the request builder. This will construct the request object needed to make calls against the service.

For an item you call:

```java
final IItemRequest itemRequest = OneDriveClient.getDrive().getItems("1234").buildRequest();
```

All request builders have a `buildRequest()` method that can generate a `IHttpRequest` object. Request objects may have different methods on them depending on the type of request. To get an item you call:

```java
itemRequest.get(new ICallback<Item>{
    @Override
    public void success(final Item result) {
        // This will make the network request and return the item
    }
    @Override
    public void failure(final ClientException ex) {
        // or an error if there was one
    }
});
```

You could also chain this together with call above :
```java

OneDriveClient.getDrive().getItems("1234").buildRequest().get(new ICallback<Item>{
    @Override
    public void success(final Item result) {
        // This will make the network request and return the item
    }
    @Override
    public void failure(final ClientException ex) {
        // or an error if there was one
    }
});
```

See [items](/docs/items.md) for more info on items and [errors](/docs/errors.md) for more info on errors.

## Query options

If you only want to retrieve certain properties of a resource, you use `select` specify them. Here's how to get only the names and ids of an item:

```java
oneDriveClient.getDrive().getItems("1234").buildRequest().select("name,id").get(new ICallback<Item>() {
    @Override
    public void success(final Item result) {
        // The item object will have null properties for everything except name and id
    }
});
```

To expand certain properties on resources you can call a similar `expand` method, like this:

```java
oneDriveClient.getDrive().getItems("1234").buildRequest().expand("thumbnails").get(new ICallback<Item>() {
    @Override
    public void success(final Item result) {
        // the item object will have collection page of thumbnails for its thumbnails property if thumbnails exist.
    }
});
```
