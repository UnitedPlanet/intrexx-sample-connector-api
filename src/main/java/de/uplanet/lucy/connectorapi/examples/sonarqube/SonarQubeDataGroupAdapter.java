/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */


package de.uplanet.lucy.connectorapi.examples.sonarqube;

import java.util.List;

import org.apache.http.client.HttpClient;

import de.uplanet.lucy.server.IProcessingContext;
import de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter;
import de.uplanet.lucy.server.odata.connector.api.v1.ConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryCriteria;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.property.IPropertyCollection;

/**
 * An Intrexx datagroup connector example for retrieving Sonarqube issues and assignees.
 */
public class SonarQubeDataGroupAdapter extends AbstractConnectorDataGroupAdapter
{
	private final SonarQubeService m_sonarService;
	private final String m_authToken;

	public SonarQubeDataGroupAdapter(IProcessingContext p_ctx, String p_strDataGroupGuid,
		IPropertyCollection p_properties, String p_strImpersonationGuid)
	{
		super(p_ctx, p_strDataGroupGuid, p_properties, p_strImpersonationGuid);

		m_authToken = p_properties.getString("sonarqube.auth.token");

		m_sonarService = new SonarQubeService(getProperties().getString("sonarqube.api.uri"), getProperties().getString(
			"sonarqube.issues.uri"));
	}

	@Override
	public IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)
	{
		FilterExpressionVisitor l_filterVisitor = new FilterExpressionVisitor();
		l_filterVisitor.visitNode(p_criteria.getFilterExpression());

		final HttpClient l_client = createHttpClient(getConnectorGuid(), "7312F993D0DA4CECCA9AE5A9D865BE142DE413EA");

		try
		{
			List<IConnectorRecord> l_records = m_sonarService.getAssigneesWithIssues(l_client, p_criteria.getFields(), m_authToken, l_filterVisitor);
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
		final HttpClient l_client = createHttpClient(getConnectorGuid(), "7312F993D0DA4CECCA9AE5A9D865BE142DE413EA");

		try
		{
			return m_sonarService.getSonarAssigneeWithIssues(l_client, p_fields, p_strRecordId, m_authToken);
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