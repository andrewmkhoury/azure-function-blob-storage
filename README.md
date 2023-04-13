# Azure Function that Access Blob Storage

Adapted from sample project [here](https://github.com/Azure-Samples/azure-functions-samples-java)

The com.functions.AddFileExtensionToBlobFunction azure function does the following:
1. Takes a file path in the blob container
2. Uses Apache Tika to detect what the file type is
3. Renames the file on the blobstorage container to the proper file extension

Here is an example of how to call the function:
http://localhost:7071/api/addFileExtension?file=testnoext/noextjpg

## Setup
1. Update the configurations in [local.settings.json]
2. Update the functionResourceGroup and functionResourceGroup configurations in [pom.xml](https://github.com/andrewmkhoury/azure-function-blob-storage/blob/e278fc29e726816262ed32f79c0df47a727ac96e/pom.xml#L20)
3. Run maven to build per the instructions [here](https://github.com/Azure-Samples/azure-functions-samples-java)
