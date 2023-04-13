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

Since azure functions aren't allowed long running times you would need to run this for each individual file.

To update all the files on an Azure Blob Storage account you would need to get a list of all the file names:

Here's an example script:
```bash
#!/bin/bash

STORAGE_ACCOUNT=storageaccount
STORAGE_ACCOUNT_KEY=key
STORAGE_CONTAINER=container
AZ_FUNCTION_HOST=http://localhost:7071

# Output list of extensionless files to extensionless-files.txt
az storage blob list --account-name STORAGE_ACCOUNT --account-key STORAGE_ACCOUNT_KEY --container-name STORAGE_CONTAINER --query '[].name' --output tsv | grep -E '^[^.]+$' > extensionless-files.txt

# Run request on each extension-less path
cat extensionless-files.txt | xargs -I {} curl "$AZ_FUNCTION_HOST/api/addFileExtension?file={}"
```
