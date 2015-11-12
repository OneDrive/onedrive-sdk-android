# Items in the OneDrive SDK for Android
Items in the OneDrive SDK behave just like items through the OneDrive API. All actions on items described in the OneDrive API are available through the SDK. For more information, see the [Items Reference](https://dev.onedrive.com/README.htm#item-resource).

The examples in this topic all use a previously created `oneDriveClient` object.

* [Get an item](#get-an-item)
* [Delete an item](#delete-an-item)
* [Upload a file](#upload-a-file)
* [Download a file](#download-a-file)
* [Move an item](#move-an-item)
* [Rename an item](#rename-an-item)
* [Copy an item](#copy-an-item)

## Get an item

To get an item, you construct request builders `getDrive` and `getItems`, you call `buildRequest` to build the request, and then make a final call to `get`.

#### Parameters

|Name|Description|
|----|-----------|
|_itemId_|The item id of the item to retrieve.|
|_callback_|The callback for when the get call is returned.|

#### Example

```java
//Enter the itemId here.
final String itemId = "0000000000000000000000";
final ICallback<Item> callback = new ICallback<Item> {
    @Override
    public void success(final Item result) {
        Toast.makeText(getActivity(), "Got item", Toast.LENGTH_LONG).show();
    }
    ...
    // Handle failure
}

oneDriveClient
    .getDrive()
    .getItems(itemId)
    .buildRequest()
    .get(callback);
```

## Delete an item

To delete an item, you construct request builders to get the item you want to delete, and then call `delete` on the item.

#### Parameters

|Name|Description|
|--------------|-----------|
|_itemId_|The item id of the item to delete.|
|_callback_|The callback for when the get call is completed.|

#### Example

```java
final String itemId = "0000000000000000000000";
final ICallback<Item> callback = new ICallback<Item> {
    @Override
    public void success(final Item result) {
        Toast.makeText(getActivity(), "Deleted item", Toast.LENGTH_LONG).show();
    }
    ...
    // Handle failure
}

oneDriveClient
    .getDrive()
    .getItems(itemId)
    .buildRequest()
    .delete(callback);
```

## Upload a file

To upload a file, you chain together build requests in this order: `getDrive`, `getItems`, `getChildren`, `byID`, and then `getContent`. You then call `buildRequest` to build the requests, and finally, `put` to complete the upload.

#### Parameters

|Name|Description|
|----|-----------|
|_filename_|The name of the file to upload.|
|_fileContents_|The byte array (byte[]) that contains the file contents.|
|_callback_|The callback when the file is uploaded, as well as progress reported.|

#### Example

```java
final String filename = "The Filename.txt";
final byte[] fileContents = new byte[] { /* The File contents to upload */};
final IProgressCallback<Item> callback = new IProgressCallback<Item>() {
    public void success(final Item item) {
        Toast.makeText(getActivity(), "Item uploaded", Toast.LENGTH_LONG).show();
    }
    ...
    // Handle progress
    // Handle failure
}

oneDriveClient
    .getDrive()
    .getItems(ItemId)
    .getChildren()
    .byId(filename)
    .getContent()
    .buildRequest()
    .put(fileContents, callback);
```

## Download a file

To download a file, you construct the request builders `getDrive`, `getItems`, and `getContent`, and then call `buildRequest` to build the request. Finally, you call `get` to retrieve the item to download.

#### Parameters

|Name|Description|
|----|-----------|
|_itemId_|The item id of the item to download.|

#### Example

```java
final String itemId = "0000000000000000000000";

final InputStream inputStream = oneDriveClient
    .getDrive()
    .getItems(itemId)
    .getContent()
    .buildRequest()
    .get();
// Use the input stream
// Close the input stream

```

## Move an item

To move an item, construct request builders to get the item with `getItem`, and then call `update` with the new location.

#### Parameters

|Name|Description|
|----|-----------|
|_newParentId_|The new parent's item id.|
|_itemId_|The item id of the item to move.|
|_newLocation_|The specific aspects of the item to update.|
|_callback_|The callback when the item update has returned.|

#### Example

```java
final String newParentId = "0000000000000000000000";
final String itemId = "0000000000000000000000";
final Item newLocation = new Item();
newLocation.parentReference = new ItemReference();
newLocation.parentReference.id = newParentId;

final ICallback<Item> callback = new ICallback<Item> {
    @Override
    public void success(final Item result) {
        Toast.makeText(getActivity(), "Update the item location", Toast.LENGTH_LONG).show();
    }
    ...
    // Handle failure
}

oneDriveClient
    .getDrive()
    .getItems(itemId)
    .buildRequest()
    .update(newLocation, callback);
```

## Rename an item

Like all other operations, you rename an item by constructing request builders `getDrive` and `getItems` on the item, and then calling `update` with the new name.

#### Parameters

|Name|Description|
|----|-----------|
|_newItemName_|The new name for the item.|
|_itemId_|The item id of the item to rename.|
|_newName_|The specific aspects of the item to update.|
|_callback_|The callback when the item update has returned.|

#### Example

```java
final String newItemName = "My File.txt";
final String itemId = "0000000000000000000000";
final Item newName = new Item();
newName.name = newItemName;

final ICallback<Item> callback = new ICallback<Item> {
    @Override
    public void success(final Item result) {
        Toast.makeText(getActivity(), "Update the item name", Toast.LENGTH_LONG).show();
    }
    ...
    // Handle failure
}

oneDriveClient
    .getDrive()
    .getItems(itemId)
    .buildRequest()
    .update(newName, callback);
```

## Copy an item

Copy requests are processed asynchronously on the service, so the pattern is slightly different, and can require multiple sets of requests.

### Starting a copy request

You create a copy request for an item by constructing request builders `getDrive`, `getItems` and 'getCopy' on the item, and then calling `create` to start the request.

#### Parameters

|Name|Description|
|----|-----------|
|_itemId_|The item id of the item to copy.|
|_copiedItemName_|The name for the copied item.|
|_parentId_|The parent's item id for the copied item.|
|_callback_|The callback when the copy request has started.|

#### Example

```java
final String itemId = "0000000000000000000000";
final String copiedItemName = "My File Copy.txt"
final String newParentId = "0000000000000000000000";

final ItemReference parentReference = new ItemReference();
parentReference.id = newParentId;

final ICallback<AsyncMonitor<Item>> callback = new ICallback<AsyncMonitor<Item>>() {
    @Override
    public void success(final AsyncMonitor<Item> itemAsyncMonitor) {
        Toast.makeText(getActivity(), "Started the copy session", Toast.LENGTH_LONG).show();
    }
    ...
    // Handle failure
};

oneDriveClient
    .getDrive()
    .getItems(itemId)
    .getCopy(copiedItemName, parentReference)
    .buildRequest()
    .create(callback);
```

### Waiting for a copy request to finish
The result from the copy requests creation is a monitor that can be checked for status updates, return result of the operation, and automatically poll for the result.

Most applications will want to get the result to the user as soon as possible via a polling approach, the following shows how to get the resulting item from with a process notifications.

#### Parameters

|Name|Description|
|----|-----------|
|_millisBetweenPoll_|The time in milliseconds between progress updates.|
|_asyncMonitor_|The name for the copied item.|
|_callback_|The callback when the copy request has finished and progressed.|

#### Example

```java
final int millisBetweenPoll = 1000; // 1 second
final AsyncMonitor<Item> asyncMonitor = ...;

final IProgressCallback<Item> callback = new IProgressCallback<Item>() {
    public void success(final Item item) {
        Toast.makeText(getActivity(), "Item copied!", Toast.LENGTH_LONG).show();
    }

    public void progress(final int progress, final int progressMax) {
        Toast.makeText(getActivity(), "Item copy process " + progress, Toast.LENGTH_SHORT).show();
    }
    ...
    // Handle failure
}

asyncMonitor
    .pollForResult(millisBetweenPoll, callback);

```
