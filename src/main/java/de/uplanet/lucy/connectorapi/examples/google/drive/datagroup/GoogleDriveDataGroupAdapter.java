
package de.uplanet.lucy.connectorapi.examples.google.drive.datagroup;


import java.util.List;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.lucy.connectorapi.examples.google.drive.file.GOOGLE_DRIVE_CONSTANT;
import de.uplanet.lucy.server.IProcessingContext;
import de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter;
import de.uplanet.lucy.server.odata.connector.api.v1.ConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryCriteria;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.property.IPropertyCollection;


public final class GoogleDriveDataGroupAdapter extends AbstractConnectorDataGroupAdapter
{
	private static final Logger ms_log = LoggerFactory.getLogger(GoogleDriveDataGroupAdapter.class);
	private final GoogleDriveDataGroupService m_service;

	public GoogleDriveDataGroupAdapter(IProcessingContext  p_ctx,
	                                   String              p_strDataGroupGuid,
	                                   IPropertyCollection p_properties,
	                                   String              p_strImpersonationGuid)
	{
		super(p_ctx, p_strDataGroupGuid, p_properties, p_strImpersonationGuid);
		m_service = new GoogleDriveDataGroupService();
	}

	/**
	 * Returns a range of data. The result contains a list of records, the count of records, and the total count 
	 * of objects available.
	 * @return {@link IConnectorQueryResult}
	 */
	@Override
	public IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)
	{
		final HttpClient l_httpClient = createHttpClient(getConnectorGuid(), null);
		final List<IConnectorRecord> l_records;

		l_records = m_service.getItemsInFolder(l_httpClient,
		                                       getProperties().getString(GOOGLE_DRIVE_CONSTANT.FOLDER_ID),
		                                       p_criteria.getFields(),
		                                       true);

		return new ConnectorQueryResult(l_records, l_records.size(), l_records.size());
	}

	/**
	 * Returns a single secord.
	 *
	 * @return {@link IConnectorRecord}
	 */
	@Override
	public IConnectorRecord queryDataRecord(String p_strRecordId, List<IConnectorField> p_fields)
	{
		HttpClient l_client = createHttpClient(getConnectorGuid(), null);

		return m_service.getRecord(l_client, p_strRecordId, p_fields);
	}

	/**
	 * Adds the metadata of a file to Google drive.
	 * 
	 * @return item id
	 */
	@Override
	public String insert(IConnectorRecord p_record)
	{
		return m_service.updateMetaDataItem(createHttpClient(getConnectorGuid(), null), p_record);
	}

	/**
	 * Updates the metadata of a file to Google drive.
	 * @return item id
	 */
	@Override
	public boolean update(IConnectorRecord p_record)
	{
		String l_id = m_service.updateMetaDataItem(createHttpClient(getConnectorGuid(), null), p_record);

		if (l_id == null || l_id.isEmpty() || "-1".equals(l_id))
			return false;

		return true;
	}

	/**
	 * Deletes a Google Drive item.
	 */
	@Override
	public void delete(String p_strRecordId)
	{
		HttpClient l_client = createHttpClient(getConnectorGuid(), null);
		m_service.deleteRecord(l_client, p_strRecordId);
	}
}
