/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */


package de.uplanet.lucy.connectorapi.examples.google.drive.datagroup;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.lucy.connectorapi.examples.google.drive.GoogleDriveItem;
import de.uplanet.lucy.connectorapi.examples.google.drive.GoogleDriveJSONParser;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderHelper;
import de.uplanet.lucy.server.odata.connector.api.v1.Field;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.odata.connector.api.v1.Record;
import de.uplanet.util.Preconditions;


public class GoogleDriveDataGroupService
{
	private static final Logger ms_log = LoggerFactory.getLogger(GoogleDriveDataGroupService.class);
	private static final String ms_rootUri = "https://www.googleapis.com/drive/v3/";

	/**
	 * Returns all folders in root.
	 */
	public List<IConnectorRecord> getRootFolders(HttpClient p_httpClient, List<IConnectorField> p_fields)
	{
		final List<IConnectorRecord> l_result = new ArrayList<IConnectorRecord>();

		final String l_parameters = "?q=mimeType = 'application/vnd.google-apps.folder' and 'root' in parents";

		final URI l_uri = URI.create(String.format("%sfiles%sfields=files(%s)",
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

			for (GoogleDriveItem l_googleDriveItem : l_items)
			{
				l_result.add(_getRecordFromGoogleDriveItem(l_googleDriveItem, p_fields));
			}
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}

		return l_result;
	}

	/**
	 * Get all subfolders of the parent folder.
	 *
	 * @param p_httpClient
	 * @param p_fields The fields to select.
	 * @param p_folderId The parent folder ID.
	 * @return List<{@link IConnectorRecord}> Google Drive Folders.
	 */
	public List<IConnectorRecord> getSubFolders(HttpClient            p_httpClient,
	                                            List<IConnectorField> p_fields,
	                                            String                p_folderId)
	{
		final List<IConnectorRecord> l_result = new ArrayList<IConnectorRecord>();

		final String l_parameters = String.format("?q=mimeType = 'application/vnd.google-apps.folder' and '%s' in parents",
			p_folderId);

		final URI l_uri = URI.create(String.format("%sfiles%sfields=files(%s)",
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

			for (GoogleDriveItem l_googleDriveItem : l_items)
			{
				l_result.add(_getRecordFromGoogleDriveItem(l_googleDriveItem, p_fields));
			}
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}

		return l_result;
	}

	/**
	 * Get all drive items in your google drive account.
	 * The boolean flag p_onlyFiles 
	 * @param p_httpClient
	 * @param p_fields The fields to select.
	 * @param p_onlyFiles Excludes all folder items.
	 * @return List<{@link IConnectorRecord}> Google drive items.
	 */
	public List<IConnectorRecord> getAllItems(HttpClient            p_httpClient,
	                                          List<IConnectorField> p_fields,
	                                          boolean               p_onlyFiles)
	{
		final String l_parameters = String.format("?fields=files(%s)", GoogleDriveItem.FIELDS);
		final URI l_uri = URI.create(ms_rootUri + "files" + l_parameters);
		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();

		ms_log.debug(l_request.toString());

		final String l_jsonResponse;
		try
		{
			l_jsonResponse = _getStringFromResponse(p_httpClient.execute(l_request));

			ms_log.debug(l_jsonResponse);

			return _getRecordsFromGoogleDriveItems(p_fields, p_onlyFiles, l_jsonResponse);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	/**
	 * Returns all items in the parent folder.
	 *
	 * @param p_httpClient The client for the REST request.
	 * @param p_parentId Google drive item id of parent folder
	 * @param p_fields The fields to select.
	 * @param p_onlyFiles If true, the request will return only file items.
	 * @return List of {@link IConnectorRecord}
	 */
	public List<IConnectorRecord> getItemsInFolder(HttpClient            p_httpClient,
	                                               String                p_parentId,
	                                               List<IConnectorField> p_fields,
	                                               boolean               p_onlyFiles)
	{
		try
		{
			final String l_parameters = String.format("?q='%s' in parents&fields=files(%s)",
			                                          p_parentId,
			                                          GoogleDriveItem.FIELDS);

			final URI l_uri = URI.create(ms_rootUri + "files" + l_parameters.replaceAll(" ", "%20"));
			final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();
			final String l_jsonResponse;

			l_jsonResponse = _getStringFromResponse(p_httpClient.execute(l_request));

			return _getRecordsFromGoogleDriveItems(p_fields, p_onlyFiles, l_jsonResponse);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	public IConnectorRecord getRecord(HttpClient p_httpClient, String p_recordId, List<IConnectorField> p_fields)
	{
		final String l_parameters = String.format("?fields=%s", GoogleDriveItem.FIELDS);

		final URI l_uri = URI.create(ms_rootUri + "files/" + p_recordId + l_parameters);
		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();
		final String l_jsonResponse;
		try
		{
			l_jsonResponse = _getStringFromResponse(p_httpClient.execute(l_request));

			return _getRecordFromJSON(p_fields, false, l_jsonResponse);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	public void deleteRecord(HttpClient p_httpClient, String p_recordId)
	{
		final URI l_uri = URI.create(ms_rootUri + "files/" + p_recordId);
		final HttpUriRequest l_request = RequestBuilder.delete(l_uri).build();

		try
		{
			HttpResponse l_response = p_httpClient.execute(l_request);

			if (l_response.getStatusLine().getStatusCode() != 204)
				throw new RuntimeException("Delete faild: " + _getStringFromResponse(l_response));
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	/**
	 * Updates metadata of a Google drive item. Supported metadata are name, id, description.
	 * @param p_httpClient
	 * @param p_record
	 * @return item id
	 */
	@SuppressWarnings("unchecked")
	public String updateMetaDataItem(HttpClient p_httpClient, IConnectorRecord p_record)
	{
		final String l_name = _extractStringFieldValue(p_record, "name");
		final String l_description = _extractStringFieldValue(p_record, "description");

		String l_id = _extractStringFieldValue(p_record, "id");

		if (l_id == null || l_id.isEmpty() || l_id.equals("-1"))
			l_id = p_record.getId();

		Preconditions.requireNonNullNonEmpty(l_id, "Record id must be set and not empty");

		try
		{
			final URI l_uri = URI.create(ms_rootUri + "files/" + l_id);

			JSONObject l_json = new JSONObject();

			l_json.put("name", l_name);
			l_json.put("description", l_description);

			StringEntity entity;

			entity = new StringEntity(l_json.toJSONString());
			entity.setContentType("application/json");
			HttpUriRequest l_request = RequestBuilder.patch(l_uri).setEntity(entity).build();
			HttpResponse l_response = p_httpClient.execute(l_request);

			if (l_response.getStatusLine().getStatusCode() != 200)
				throw new RuntimeException("Update of MetaData Faild with Error Message: " + _getStringFromResponse(
					l_response));
		}
		catch (IOException l_e)
		{
			throw new RuntimeException(l_e);
		}

		if (l_id == null || l_id.isEmpty())
			return "-1";
		else
			return l_id;
	}

	/**
	 * Parse response json to a list of {@link GoogleDriveItem}.
	 * This list of {@link GoogleDriveItem}s will be parsed then to a list of {@link IConnectorRecord}.
	 * @param p_fields
	 * @param p_onlyFiles
	 * @param p_jsonResponse
	 * @return List of {@link IConnectorRecord}
	 * @throws Exception
	 */
	private List<IConnectorRecord> _getRecordsFromGoogleDriveItems(List<IConnectorField> p_fields,
	                                                               boolean               p_onlyFiles,
		final String p_jsonResponse) throws Exception
	{
		final List<IConnectorRecord> l_result = new ArrayList<IConnectorRecord>();
		List<GoogleDriveItem> l_items = new GoogleDriveJSONParser().parseJSONStringToGoogleDriveItems(p_jsonResponse);

		for (GoogleDriveItem l_googleDriveItem : l_items)
		{
			// If mimeType is not folder it will add the Record or if onlyFiles Flag is false it will add the Record.
			if (!(p_onlyFiles && "application/vnd.google-apps.folder".equals(l_googleDriveItem.getMimeType())))
				l_result.add(_getRecordFromGoogleDriveItem(l_googleDriveItem, p_fields));
		}

		return l_result;
	}

	/**
	 * Parse JSON response string to {@link GoogleDriveItem} and then to {@link IConnectorRecord}.
	 *
	 * @param p_fields
	 * @param p_onlyFiles
	 * @param p_jsonResponse
	 * @return {@link IConnectorRecord}
	 * @throws ParseException
	 */
	private IConnectorRecord _getRecordFromJSON(List<IConnectorField> p_fields, boolean p_onlyFiles,
		final String p_jsonResponse) throws ParseException
	{
		JSONObject l_json = (JSONObject) new JSONParser().parse(p_jsonResponse);

		return _getRecordFromGoogleDriveItem(new GoogleDriveJSONParser().parseJsonToGoogleDriveItem(l_json), p_fields);
	}

	/**
	 * Parse {@link GoogleDriveItem} to {@link IConnectorRecord}.
	 * @param p_item
	 * @param p_fields
	 * @return {@link IConnectorRecord}
	 */
	private IConnectorRecord _getRecordFromGoogleDriveItem(GoogleDriveItem p_item, List<IConnectorField> p_fields)
	{
		List<IConnectorField> l_fields = new ArrayList<IConnectorField>();

		for (IConnectorField l_conField : p_fields)
		{
			if ("id".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item.getId())));
			}
			else if ("name".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item.getName())));
			}
			else if ("thumbnailLink".equalsIgnoreCase(l_conField.getName()))
			{
				if (p_item.getThumbnailLink() != null)
					l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item
						.getThumbnailLink().toASCIIString())));
			}
			else if ("kind".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item.getKind())));
			}
			else if ("webViewLink".equalsIgnoreCase(l_conField.getName()))
			{
				if (p_item.getWebViewLink() != null)
					l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item
						.getWebViewLink().toASCIIString())));
			}
			else if ("size".equalsIgnoreCase(l_conField.getName()))
			{
				if (p_item.getSize() != null)
					l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(_renderSize(p_item
						.getSize()))));
			}
			else if ("mimeType".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item.getMimeType())));
			}
			else if ("description".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item
					.getDescription())));
			}
			else if ("createdTime".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item
					.getCreatedTime())));
			}
			else if ("modifiedTime".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item
					.getModifiedTime())));
			}
			else if ("webContentLink".equalsIgnoreCase(l_conField.getName()))
			{
				if (p_item.getDownloadLink() != null)
					l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_item
						.getDownloadLink().toASCIIString())));
			}
		}

		return new Record(p_item.getId(), l_fields);
	}

	private String _renderSize(long p_size)
	{
		if (p_size <= 0)
			return "0";
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(p_size) / Math.log10(1024));

		return new DecimalFormat("#,##0.#").format(p_size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	private String _getStringFromResponse(final HttpResponse p_response) throws IOException
	{
		InputStream l_inputStream = p_response.getEntity().getContent();
		StringWriter writer = new StringWriter();
		IOUtils.copy(l_inputStream, writer, "UTF-8");
		String l_string = writer.toString();
		return l_string;
	}

	private String _extractStringFieldValue(IConnectorRecord p_record, String p_fieldName)
	{
		IConnectorField l_field;
		try
		{
			l_field = p_record.getFieldByName(p_fieldName);
		}
		catch (Exception l_e)
		{
			return null;
		}

		if (l_field == null)
			return null;

		return ValueHolderHelper.getStringFromVH(l_field.getValue());
	}
}
