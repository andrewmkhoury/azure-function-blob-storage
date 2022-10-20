package com.functions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BlobInput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

/**
 * Azure Durable Functions with HTTP trigger.
 */
public class AddFileExtensionToBlobFunction {
	private static final String ENV_STORAGE_ACCOUNT_CONNECTION_STRING = "Storage_Account_Connection_String";

	private static final String ENV_BLOB_CONTAINER = "Blob_Container";

	private static final String[] SUPPORTED_EXTENSIONS = new String[] { "3G2", "3GP", "AAC", "AFM", "AI", "AIFF", "AVI",
			"BMP", "CSS", "DOC", "DOCX", "DVI", "EPS", "EPUB", "F4V", "FLA", "FLAC", "FLV", "FPX", "GIF", "HTML", "ICC",
			"ICM", "INDD", "JAR", "JPEG", "JPG", "M2V", "M4V", "MIDI", "MJ2", "MKV", "MOV", "MP2", "MP3", "MP4", "MPEG",
			"MPG", "MTS", "MXF", "ODP", "ODS", "ODT", "OGA", "OGG", "OGV", "OTF", "PBM", "PDF", "PFB", "PFM", "PGM",
			"PICT", "PNG", "PNM", "PPM", "PPT", "PPTX", "PS", "PSB", "PSD", "QT", "QXP", "R3D", "RA", "RAM", "RAR",
			"RM", "RTF", "SVG", "SWF", "TAR", "TGZ", "TIF", "TIFF", "TTC", "TTF", "TXT", "VOB", "VTT", "WAV", "WebM",
			"WMA", "WMV", "XLS", "XLSX", "XML", "ZIP" };

	HashSet<String> SUPPORTED_EXTENSIONS_SET_ALLCAPS = new HashSet<>(Arrays.stream(SUPPORTED_EXTENSIONS).collect(Collectors.toSet()));
	
	@FunctionName("addFileExtension")
	public HttpResponseMessage addFileExtension(
			@HttpTrigger(name = "req",
					methods = {
					HttpMethod.GET 
					},
					authLevel = AuthorizationLevel.ANONYMOUS) 
					HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {
		String connectionString = System.getenv(ENV_STORAGE_ACCOUNT_CONNECTION_STRING);
		CloudStorageAccount storageAccount = getCloudStorageAccount(connectionString);
		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
		CloudBlobContainer blobContainer = getBlobContainer(blobClient);
		String filePath = request.getQueryParameters().get("file");

		return processRequest(request, blobContainer, filePath);
	}

	private HttpResponseMessage processRequest(HttpRequestMessage<Optional<String>> request,
			CloudBlobContainer blobContainer, String filePath) {
		// build HTTP response with size of requested blob
		if (hasFileExtension(filePath)) {
			return request.createResponseBuilder(HttpStatus.OK).body("File has an extension").build();
		} else {
			byte[] content = getFileAsByteArray(blobContainer, filePath);
			if (content != null) {
				String ext = getExtension(content);
				if (isSupportedExtension(ext)) {
					rename(blobContainer, filePath, filePath + ext, content);
					return request.createResponseBuilder(HttpStatus.OK)
							.body("Copied file " + filePath + "\" is: " + ext + " " + content.length + " bytes").build();
				} else {
					return request.createResponseBuilder(HttpStatus.OK).body("Unsupported file type: " + ext).build();				
				}
			} else {
				return request.createResponseBuilder(HttpStatus.NOT_FOUND)
						.body("File \"" + request.getQueryParameters().get("file") + "\" was not found").build();
			}
		}
	}

	private boolean isSupportedExtension(String ext) {
		return ext != null && SUPPORTED_EXTENSIONS_SET_ALLCAPS.contains(ext.substring(1).toUpperCase());
	}

	private byte[] getFileAsByteArray(CloudBlobContainer container, String filePath) {
		byte[] content = null;
		CloudBlob blob;
		try {
			blob = container.getBlobReferenceFromServer(filePath);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			blob.download(baos);
			content = baos.toByteArray();
			baos.close();
			baos = null;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (StorageException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	private boolean hasFileExtension(String filePath) {
		String extension = "";

		int i = filePath.lastIndexOf('.');
		int p = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

		if (i > p) {
		    extension = filePath.substring(i+1);
		}
		return extension.length() > 0;
	}
	
	private CloudBlobContainer getBlobContainer(CloudBlobClient blobClient) {
		CloudBlobContainer container = null;
		try {
			container = blobClient.getContainerReference(System.getenv(ENV_BLOB_CONTAINER));
			return container;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return container;
	}

	private String getExtension(byte[] content) {
		String extension = null;
		// Load your Tika config, find all the Tika classes etc
		TikaConfig config = TikaConfig.getDefaultConfig();

		// Do the detection. Use DefaultDetector / getDetector() for more advanced
		// detection
		Metadata metadata = new Metadata();
		InputStream stream = TikaInputStream.get(content, metadata);
		MediaType mediaType;
		try {
			mediaType = config.getMimeRepository().detect(stream, metadata);

			// Fest the most common extension for the detected type
			MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
			extension = mimeType.getExtension();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (MimeTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return extension;
	}

	public CloudStorageAccount getCloudStorageAccount(String connectionString) {
		CloudStorageAccount account = null;
		try {
			account = CloudStorageAccount.parse(connectionString);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return account;
	}

	public void rename(CloudBlobContainer container, String oldFilename, String newFilename, byte[] content) {
		CloudBlob oldBlob;
		try {
			oldBlob = container.getBlobReferenceFromServer(oldFilename);
			CloudBlob newBlob = container.getAppendBlobReference(newFilename);

			newBlob.upload(new ByteArrayInputStream(content), content.length);
			oldBlob.delete();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
