# Azure Function that Access Blob Storage

Adapted from sample project [here](https://github.com/Azure-Samples/azure-functions-samples-java)

The com.functions.AddFileExtensionToBlobFunction azure function does the following:
1. Takes a file path in the blob container
2. Uses Apache Tika to detect what the file type is
3. Renames the file on the blobstorage container to the proper file extension

Here is an example of how to call the function:
http://localhost:7071/api/addFileExtension?file=testnoext/noextjpg