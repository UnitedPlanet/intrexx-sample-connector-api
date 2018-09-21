/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */

package de.uplanet.lucy.connectorapi.examples.google.calendar;


import java.util.List;

import org.apache.http.client.HttpClient;
import org.odata4j.expression.PrintExpressionVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.lucy.connectorapi.examples.calendar.CalendarFilterVisitor;
import de.uplanet.lucy.server.IProcessingContext;
import de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter;
import de.uplanet.lucy.server.odata.connector.api.v1.ConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryCriteria;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.property.IPropertyCollection;


public final class GoogleCalendarEventDataGroupAdapter extends AbstractConnectorDataGroupAdapter
{
	private static final Logger ms_log = LoggerFactory.getLogger(GoogleCalendarEventDataGroupAdapter.class);

	private final GoogleCalendarService m_googleService;

	private final String m_googleCalendarId;

	public GoogleCalendarEventDataGroupAdapter(IProcessingContext        p_ctx,
	                                         String                    p_strDataGroupGuid,
	                                         IPropertyCollection       p_properties,
		String p_strImpersonationGuid)
	{
		super(p_ctx, p_strDataGroupGuid, p_properties, p_strImpersonationGuid);

		m_googleCalendarId = getProperties().getValue("connector.google.calendar.id").asString();
		m_googleService = new GoogleCalendarService();
	}

	@Override
	public IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)
	{
		final PrintExpressionVisitor l_visit = new PrintExpressionVisitor();
		l_visit.visitNode(p_criteria.getFilterExpression());
		ms_log.error("Unexpected filter expression: " + l_visit.toString());

		final CalendarFilterVisitor l_cvisit = new CalendarFilterVisitor();
		l_cvisit.visitNode(p_criteria.getFilterExpression());

		final String l_startDate = l_cvisit.getStart().toString("yyyy-MM-dd'T'HH:mm:ss'Z'");
		final String l_endDate = l_cvisit.getEnd().toString("yyyy-MM-dd'T'HH:mm:ss'Z'");

		HttpClient l_httpClient = createHttpClient(getConnectorGuid(), null);

		try
		{
			List<IConnectorRecord> l_records = m_googleService.getEventList(l_httpClient,
			                                                                p_criteria.getFields(),
			                                                                m_googleCalendarId,
			                                                                l_startDate,
			                                                                l_endDate);

			return new ConnectorQueryResult(l_records,
			                                l_records.size(),
			                                l_records.size());
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
			return m_googleService.getEvent(l_client,
			                                p_fields,
			                                m_googleCalendarId,
			                                p_strRecordId);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	@Override
	public String insert(IConnectorRecord p_record)
	{
		try
		{
			return m_googleService.createEvent(createHttpClient(getConnectorGuid(), null),
			                                   m_googleCalendarId,
			                                   p_record);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	@Override
	public boolean update(IConnectorRecord p_record)
	{
		String l_id = p_record.getId();
		try
		{
			return m_googleService.updateEvent(createHttpClient(getConnectorGuid(), null),
			                                   p_record,
			                                   m_googleCalendarId,
			                                   l_id);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}

	@Override
	public void delete(String p_strRecordId)
	{
		try
		{
			m_googleService.deleteEvent(createHttpClient(getConnectorGuid(), null),
			                            m_googleCalendarId,
			                            p_strRecordId);
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}
}
