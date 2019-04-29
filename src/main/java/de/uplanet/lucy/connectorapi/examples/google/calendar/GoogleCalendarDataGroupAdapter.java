
package de.uplanet.lucy.connectorapi.examples.google.calendar;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.lucy.server.IProcessingContext;
import de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter;
import de.uplanet.lucy.server.odata.connector.api.v1.ConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryCriteria;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.property.IPropertyCollection;


public class GoogleCalendarDataGroupAdapter extends AbstractConnectorDataGroupAdapter
{
	private static final Logger ms_log = LoggerFactory.getLogger(GoogleCalendarEventDataGroupAdapter.class);

	private final GoogleCalendarService m_googleService;

	public GoogleCalendarDataGroupAdapter(IProcessingContext p_ctx, String p_strDataGroupGuid,
		IPropertyCollection p_properties, String p_strImpersonationGuid)
	{
		super(p_ctx, p_strDataGroupGuid, p_properties, p_strImpersonationGuid);

		m_googleService = new GoogleCalendarService();
	}

	@Override
	public IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)
	{
		HttpClient l_httpClient = createHttpClient(getConnectorGuid(), null);
		try
		{
			List<IConnectorRecord> l_records = m_googleService.getCalendarList(l_httpClient, p_criteria.getFields());

			return new ConnectorQueryResult(l_records, l_records.size(), l_records.size());
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	@Override
	public IConnectorRecord queryDataRecord(String p_strRecordId, List<IConnectorField> p_fields)
	{
		HttpClient l_client = createHttpClient(getConnectorGuid(), null);

		try
		{
			return m_googleService.getEvent(l_client, p_fields, "primary", p_strRecordId);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	@Override
	public String insert(IConnectorRecord p_record)
	{
		return null;
	}

	@Override
	public boolean update(IConnectorRecord p_record)
	{
		return false;
	}

	@Override
	public void delete(String p_strRecordId)
	{
	}
}
