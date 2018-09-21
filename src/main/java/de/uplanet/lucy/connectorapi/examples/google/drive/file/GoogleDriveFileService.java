/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */


package de.uplanet.lucy.connectorapi.examples.google.drive.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.security.oauth2.client.OAuth2ClientContext;

import de.uplanet.lucy.odata.ODATA_CONSTANT;
import de.uplanet.lucy.odata.ODataAuth;
import de.uplanet.lucy.odata.ODataAuthHolder;
import de.uplanet.lucy.server.ContextSession;
import de.uplanet.lucy.connectorapi.examples.google.drive.GoogleDriveItem;
import de.uplanet.lucy.connectorapi.examples.google.drive.GoogleDriveJSONParser;
import de.uplanet.lucy.server.file.action.IOperationFile;
import de.uplanet.lucy.server.session.ISession;
import de.uplanet.util.Preconditions;


public class GoogleDriveFileService
{
	private static final String ms_rootUri = "https://www.googleapis.com/drive/v3/files/";

	private static final String ms_folderMimeType = "application/vnd.google-apps.folder";
	private static final int ONE_MB = 1024 * 1024;

	private final GoogleDriveJSONParser m_googleDriveParser = new GoogleDriveJSONParser();

	/**
	 * Creates folders. For creating main folders use parentFolderId = 'root'
	 * @return {@link GoogleDriveItem}
	 */
	public GoogleDriveItem createFolder(HttpClient p_httpClient, String p_parentId, String p_folderName)
		throws RuntimeException
	{
		String l_parameter = "?fields=" + GoogleDriveItem.FIELDS;
		URI l_deleteURI = URI.create(ms_rootUri + l_parameter);

		try
		{
			StringEntity entity = new StringEntity(_getDriveItemJSON(p_folderName, p_parentId, ms_folderMimeType));

			HttpUriRequest l_request = RequestBuilder.post(l_deleteURI).setEntity(entity).build();
			String l_jsonString = _getStringFromResponse(p_httpClient.execute(l_request));
			JSONObject l_json = (JSONObject) new JSONParser().parse(l_jsonString);

			return m_googleDriveParser.parseJsonToGoogleDriveItem(l_json);
		}
		catch (IOException | ParseException l_e)
		{
			throw new RuntimeException("Creating folder '" + p_folderName + "' failed.", l_e);
		}
	}

	/**
	 * Deletes a drive item.
	 */
	public void deleteFile(HttpClient p_httpClient, String p_recordId) throws RuntimeException
	{
		URI l_deleteURI = URI.create(String.format(ms_rootUri + "%s", p_recordId));

		HttpUriRequest l_request = RequestBuilder.delete(l_deleteURI).build();

		try
		{
			p_httpClient.execute(l_request);
		}
		catch (IOException l_e)
		{
			throw new RuntimeException("Deleting file failed!", l_e);
		}
	}

	/**
	 * Downloads file from Google Drive.
	 */
	public void downloadFile(HttpClient p_httpClient, String p_downloadUrl, File p_fileTarget) throws Exception
	{
		Preconditions.requireNonNullNonEmpty(p_downloadUrl, "URI is null.");
		Preconditions.requireNonNull(p_fileTarget, "File is null.");

		final HttpUriRequest l_request = RequestBuilder.get(p_downloadUrl).build();

		try (FileOutputStream l_fos = new FileOutputStream(p_fileTarget))
		{
			HttpEntity l_entity = p_httpClient.execute(l_request).getEntity();
			l_entity.writeTo(l_fos);
		}

		HttpClientUtils.closeQuietly(p_httpClient);
	}

	/**
	 * Get drive item from Google Drive.
	 * @return {@link GoogleDriveItem}
	 */
	public GoogleDriveItem getDriveItem(HttpClient p_httpClient, String p_recordId)
	{
		final String l_parameters = String.format("?fields=%s", GoogleDriveItem.FIELDS);

		final URI l_uri = URI.create(ms_rootUri + p_recordId + l_parameters);
		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();
		final String l_jsonResponse;
		try
		{
			l_jsonResponse = _getStringFromResponse(p_httpClient.execute(l_request));


			JSONObject l_record = (JSONObject) new JSONParser().parse(l_jsonResponse);
			return m_googleDriveParser.parseJsonToGoogleDriveItem(l_record);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	/**
	 * Check Google Dive for existing file. Returns true, if drive item with filename exists.
	 */
	public boolean fileNameExists(HttpClient p_httpClient, String p_parentId, String p_fileName)
	{
		try
		{
			final String l_rootUri = "https://www.googleapis.com/drive/v3/";
			final String l_jsonResponse;
			final String l_parameters = String.format("?q='%s' in parents&fields=files(%s)", p_parentId,
				GoogleDriveItem.FIELDS);

			final URI l_uri = URI.create(l_rootUri + "files" + l_parameters.replaceAll(" ", "%20"));
			final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();

			l_jsonResponse = _getStringFromResponse(p_httpClient.execute(l_request));

			List<GoogleDriveItem> l_items = m_googleDriveParser.parseJSONStringToGoogleDriveItems(l_jsonResponse);

			Optional<GoogleDriveItem> l_driveItem;

			l_driveItem = l_items.stream().filter(p_item -> p_item.getName().equalsIgnoreCase(p_fileName)).findFirst();

			if (l_driveItem.isPresent())
				return true;
			else
				return false;
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	/**
	 * Search subfolder by name.
	 * @return Drive item id of a subfolder.
	 */
	public Optional<String> getSubFolderIdByName(HttpClient p_httpClient, String p_parentId, String p_folderName)
	{
		final String l_parameters = String.format(
			"?q=mimeType = 'application/vnd.google-apps.folder' and '%s' in parents", p_parentId);

		final URI l_uri = URI.create(String.format("%sfiles%sfields=files(%s)", ms_rootUri, l_parameters,
			GoogleDriveItem.FIELDS));

		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();
		final String l_jsonResponse;
		try
		{
			l_jsonResponse = _getStringFromResponse(p_httpClient.execute(l_request));

			List<GoogleDriveItem> l_items = m_googleDriveParser.parseJSONStringToGoogleDriveItems(l_jsonResponse);

			Optional<GoogleDriveItem> l_driveItem;

			l_driveItem = l_items.stream().filter(p_item -> p_item.getName().equalsIgnoreCase(p_folderName))
				.findFirst();

			if (l_driveItem.isPresent())
				return Optional.of(l_driveItem.get().getId());
			else
				return Optional.empty();
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	/**
	 * Check authentication status of current user.
	 * @param p_connectorCfgId
	 * @param p_usrGuid
	 * @return True, if user is already authenticated.
	 */
	public boolean isAuthenticated(String p_connectorCfgId, String p_usrGuid)
	{
		final ISession l_session = ContextSession.get();

		if (l_session == null)
			return false;

		ODataAuthHolder l_authHolder = (ODataAuthHolder) l_session.get("odataAuth");
		ODataAuth l_auth;

		if (l_authHolder == null)
			return false;

		if (l_authHolder.contains(p_usrGuid, p_connectorCfgId))
			l_auth = l_authHolder.get(p_usrGuid, p_connectorCfgId);
		else
			return false;

		final OAuth2ClientContext l_context;

		l_context = (OAuth2ClientContext) l_auth.getCustomProperty(ODATA_CONSTANT.OAUTH2_CONTEXT);

		return l_context != null && l_context.getAccessToken() != null && !l_context.getAccessToken().isExpired();
	}

	/**
	 * Replace a file on Google Drive.
	 */
	public GoogleDriveItem replaceFile(HttpClient p_httpClient,
			                           IOperationFile p_file,
			                           String         p_recId,
			                           String         p_fileName,
			                           String         p_parentId)
	{
		Preconditions.requireNonNull(p_httpClient, "HttpClient is required.");
		Preconditions.requireNonNull(p_file, "File is required.");
		final String l_json;

		try
		{
			if (p_file.getFile().length() <= 0)
				throw new IllegalArgumentException("Empty files are not allowed.");

			if (p_file.getFile().length() > 4 * ONE_MB)
			{
				l_json = _uploadBigFile(p_httpClient, p_file, p_fileName, p_parentId, p_recId);
			}
			else
			{
				l_json = _uploadSmallFile(p_httpClient, p_file, p_fileName, p_parentId, p_recId);
			}

			return m_googleDriveParser.parseJsonToGoogleDriveItem((JSONObject) new JSONParser().parse(l_json));
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}


	public GoogleDriveItem uploadFile(HttpClient     p_httpClient,
	                                  IOperationFile p_file,
	                                  String         p_fileName,
	                                  String         p_parentId)
	{
		Preconditions.requireNonNull(p_httpClient, "HttpClient is required.");
		Preconditions.requireNonNull(p_file, "File is required.");
		final String l_json;

		try
		{
			if (p_file.getFile().length() <= 0)
				throw new IllegalArgumentException("Empty files are not allowed.");

			if (p_file.getFile().length() > 4 * ONE_MB)
			{
				l_json = _uploadBigFile(p_httpClient, p_file, p_fileName, p_parentId, null);
			}
			else
			{
				l_json = _uploadSmallFile(p_httpClient, p_file, p_fileName, p_parentId, null);
			}

			return m_googleDriveParser.parseJsonToGoogleDriveItem((JSONObject) new JSONParser().parse(l_json));
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	private String _uploadSmallFile(HttpClient p_client,
	                               IOperationFile p_file,
	                               String         p_fileName,
	                               String         p_parentId,
	                               String         p_recId)
	{
		final String l_recId;
		final String l_json;
		final String l_uploadURI;
		final HttpUriRequest l_request;

		if (p_recId == null)
		{
			l_recId = "";
			l_json = String.format("{\"name\": \"%s\", \"parents\": [\"%s\"]}", p_fileName, p_parentId);
		}
		else
		{
			l_recId = "/" + p_recId;
			l_json = String.format("{\"name\": \"%s\"}", p_fileName);
		}

		l_uploadURI = String.format("https://www.googleapis.com/upload/drive/v3/files%s?uploadType=multipart", l_recId);

		ContentType l_type = ContentType.getByMimeType(p_file.getContentType());

		if(l_type == null)
		{
			l_type = ContentType.APPLICATION_OCTET_STREAM;
		}

		FileBody fileBody = new FileBody(p_file.getFile(), l_type);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.STRICT);
		builder.addPart("file_upload", new StringBody(l_json, ContentType.APPLICATION_JSON));
		builder.addPart("file_upload", fileBody);
		builder.setContentType(ContentType.create("multipart/related"));
		builder.setBoundary("file_upload");

		HttpEntity l_entity = builder.build();

		if (p_recId != null && !p_recId.isEmpty())
		{
			l_request = RequestBuilder.patch(l_uploadURI).setEntity(l_entity).build();
		}
		else
		{
			l_request = RequestBuilder.post(l_uploadURI).setEntity(l_entity).build();
		}

		final String l_upResultJson;
		try
		{
			final HttpResponse l_resp = p_client.execute(l_request);
			l_upResultJson = EntityUtils.toString(l_resp.getEntity());

			if (l_resp.getStatusLine().getStatusCode() < 200 || l_resp.getStatusLine().getStatusCode() > 201)
				throw new RuntimeException(l_upResultJson);
		}
		catch (IOException l_e)
		{
			throw new RuntimeException(l_e);
		}

		return l_upResultJson;
	}

	private String _uploadBigFile(HttpClient p_client,
	                             IOperationFile p_file,
	                             String         p_fileName,
	                             String         p_parentId,
	                             String         p_recId)
	{
		final String l_recId;
		final String l_uploadURI;
		final String l_resumableURI;
		final String l_json;
		final HttpUriRequest l_initRequest;
		final HttpUriRequest l_putRequest;

		if (p_recId == null)
		{
			l_recId = "";
			l_json = String.format("{\"name\": \"%s\", \"parents\": [\"%s\"]}", p_fileName, p_parentId);
		}
		else
		{
			l_recId = "/" + p_recId;
			l_json = String.format("{\"name\": \"%s\"}", p_fileName);
		}

		l_uploadURI = String.format("https://www.googleapis.com/upload/drive/v3/files%s?uploadType=resumable", l_recId);

		ContentType l_type = ContentType.getByMimeType(p_file.getContentType());

		if (l_type == null)
		{
			l_type = ContentType.APPLICATION_OCTET_STREAM;
		}

		try
		{
			StringEntity l_strEntity = new StringEntity(l_json);

			if (p_recId != null && !p_recId.isEmpty())
			{
				l_initRequest = RequestBuilder.patch(l_uploadURI).setEntity(l_strEntity).build();
			}
			else
			{
				l_initRequest = RequestBuilder.post(l_uploadURI).setEntity(l_strEntity).build();
			}

			l_initRequest.addHeader("Content-Type", "application/json; charset=UTF-8");
			l_initRequest.addHeader("X-Upload-Content-Type", l_type.getMimeType());
			l_initRequest.addHeader("X-Upload-Content-Length", Long.toString(p_file.getFile().length()));
			HttpResponse l_initResponse = p_client.execute(l_initRequest);

			Header[] l_headers = l_initResponse.getHeaders("Location");
			if (l_headers.length != 1)
				throw new RuntimeException("LocationHeader is Requried: Conut of Location Header = "
					+ l_headers.length);

			l_resumableURI = l_headers[0].getValue();
		}
		catch (IOException l_e1)
		{
			throw new RuntimeException(l_e1);
		}

		try
		{
			FileEntity l_fileEntity = new FileEntity(p_file.getFile());

			l_putRequest = RequestBuilder.put(l_resumableURI).setEntity(l_fileEntity).build();

			HttpResponse l_response = p_client.execute(l_putRequest);

			String l_upResultJson = EntityUtils.toString(l_response.getEntity());

			if (l_response.getStatusLine().getStatusCode() < 200 || l_response.getStatusLine().getStatusCode() > 201)
				throw new RuntimeException(_getStringFromResponse(l_response));

			return l_upResultJson;
		}
		catch (IOException l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	/**
	 * @return Count of files in parent folder.
	 */
	public int getFileCount(HttpClient p_httpClient, String p_parentId)
	{
		final String l_parameters = String.format("?q=mimeType != '%s' and '%s' in parents",
		                                          ms_folderMimeType,
		                                          p_parentId);

		final URI l_uri = URI.create(String.format("%sfiles%s&fields=files(%s)",
			                                        ms_rootUri,
			                                        l_parameters.replaceAll(" ", "%20"),
			                                        GoogleDriveItem.FIELDS));

		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();
		final String l_jsonResponse;
		try
		{
			l_jsonResponse = _getStringFromResponse(p_httpClient.execute(l_request));

			List<GoogleDriveItem> l_items = new GoogleDriveJSONParser().parseJSONStringToGoogleDriveItems(
				l_jsonResponse);

			return l_items.size();
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	@SuppressWarnings("unchecked")
	private String _getDriveItemJSON(String p_name, String p_parentId, String p_mimeType)
	{
		JSONObject l_obj = new JSONObject();
		l_obj.put("mimeType", p_mimeType);
		l_obj.put("name", p_name);

		JSONArray l_parents = new JSONArray();
		l_parents.add(p_parentId);
		l_obj.put("parents", l_parents);

		return l_obj.toJSONString();
	}

	private String _getStringFromResponse(final HttpResponse p_response) throws IOException
	{
		InputStream l_inputStream = p_response.getEntity().getContent();
		StringWriter l_writer = new StringWriter();
		IOUtils.copy(l_inputStream, l_writer, "UTF-8");
		return l_writer.toString();
	}
}