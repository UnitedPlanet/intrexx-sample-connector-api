/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */

package de.uplanet.lucy.connectorapi.examples.sonarqube;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory;
import de.uplanet.lucy.server.odata.connector.api.v1.Field;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.odata.connector.api.v1.Record;


public class SonarQubeService
{
	private static final Logger ms_log = LoggerFactory.getLogger(SonarQubeService.class);
	private final String m_sonarApiUri;

	private final SonarQubeJSONParser m_sonarQubeJSONParser;

	public SonarQubeService(String p_sonarApiUri, String p_sonarIssuesUri)
	{
		m_sonarQubeJSONParser = new SonarQubeJSONParser(p_sonarIssuesUri);
		m_sonarApiUri = p_sonarApiUri;
	}

	public List<IConnectorRecord> getAssigneesWithIssues(HttpClient              p_httpClient,
	                                                     List<IConnectorField>   p_fields,
	                                                     String                  p_authToken,
	                                                     FilterExpressionVisitor p_filterVisitor) throws Exception
	{
		final List<IConnectorRecord> l_result = new ArrayList<IConnectorRecord>();

		String l_json = _getSonarUsers(p_httpClient, p_authToken);

		List<AssigneeWithIssues> l_assignees = m_sonarQubeJSONParser.parseJSONToAssigneesWithIssues(l_json);

		List<String> l_issues;

		for (AssigneeWithIssues l_assignee : l_assignees)
		{
			l_issues = _getIssuesOfAssignee(l_assignee.getLogin(), p_httpClient);
			m_sonarQubeJSONParser.parseJSONAndCompleteAssigneeWithIssues(l_issues, l_assignee);
		}

		List<IConnectorField> l_fields;

		if (!p_filterVisitor.getLogin().isEmpty())
		{
			l_assignees = l_assignees.stream()
				.filter(l_assignee -> !l_assignee.getLogin().equals(p_filterVisitor.getLogin().get(0)))
				.filter(l_assignee -> !l_assignee.getLogin().equals(p_filterVisitor.getLogin().get(1)))
				.filter(l_assignee -> l_assignee.getEmail() != null)
				.filter(l_assignee -> l_assignee.getNewIssues() > p_filterVisitor.getNewIssues())
				.collect(Collectors.toList());
		}

		for (AssigneeWithIssues l_assignee : l_assignees)
		{
			l_fields = _getIssuesFields(p_fields, l_assignee);
			l_result.add(new Record(l_assignee.getLogin(), l_fields));
		}

		return l_result;
	}

	public IConnectorRecord getSonarAssigneeWithIssues(HttpClient p_httpClient, List<IConnectorField> p_fields,
		String p_record, String p_authToken) throws Exception
	{
		String l_json = _getSonarUsers(p_httpClient, p_authToken);

		AssigneeWithIssues l_assignee = m_sonarQubeJSONParser.getAssigneeWithIssues(l_json, p_record);

		List<String> l_issues = _getIssuesOfAssignee(p_record, p_httpClient);

		m_sonarQubeJSONParser.parseJSONAndCompleteAssigneeWithIssues(l_issues, l_assignee);

		final List<IConnectorField> l_fields = _getIssuesFields(p_fields, l_assignee);

		return new Record(l_assignee.getLogin(), l_fields);
	}

	private String _getSonarUsers(HttpClient p_httpClient, String p_authToken) throws Exception
	{
		final URI l_uri = URI.create(m_sonarApiUri + "users/search");

		final String l_base64Auth = Base64.getEncoder().encodeToString((p_authToken + ":").getBytes());

		final Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", "Basic " + l_base64Auth);

		return _getResponseString(p_httpClient, l_uri, headers);
	}

	private List<String> _getIssuesOfAssignee(String p_uid, HttpClient p_httpClient) throws Exception
	{
		final String l_apiUrl = m_sonarApiUri
			+ "issues/search?statuses=OPEN&assigned=true&assignees={assignee}&pageSize=-1&pageIndex={pageIndex}"
				.replace("{assignee}", p_uid);

		final URI l_uri = URI.create(l_apiUrl.replace("{pageIndex}", "1"));
		final String l_paging = _getResponseString(p_httpClient, l_uri);
		int l_pages = _getPages(l_paging);

		final List<String> l_json = new ArrayList<>();
		URI l_indexUri;

		while (l_pages > 0)
		{
			l_indexUri = URI.create(l_apiUrl.replace("{pageIndex}", Integer.toString(l_pages)));
			l_pages--;
			l_json.add(_getResponseString(p_httpClient, l_indexUri));
		}

		return l_json;
	}

	private String _getResponseString(HttpClient p_httpClient, URI p_uri, Map<String, String> p_headers)
		throws Exception
	{
		final RequestBuilder l_request = RequestBuilder.get(p_uri);

		if (p_headers != null)
		{
			for (Map.Entry<String, String> header : p_headers.entrySet())
			{
				l_request.addHeader(header.getKey(), header.getValue());
			}
		}

		final HttpUriRequest l_get = l_request.build();
		final HttpResponse l_response = p_httpClient.execute(l_get);

		return EntityUtils.toString(l_response.getEntity());
	}

	private String _getResponseString(HttpClient p_httpClient, URI p_uri) throws Exception
	{
		return _getResponseString(p_httpClient, p_uri, null);
	}

	private int _getPages(String p_json) throws Exception
	{
		final Map<String, Integer> l_paging = m_sonarQubeJSONParser.getSonarPaging(p_json);

		if (l_paging.get("total") == 0)
		{
			return 0;
		}

		final int l_total = l_paging.get("total") < 10000 ? l_paging.get("total") : 10000;

		return (int) Math.ceil((double) l_total / l_paging.get("pageSize"));
	}

	private List<IConnectorField> _getIssuesFields(List<IConnectorField> p_fields,
		AssigneeWithIssues p_assigneeWithIssues)
	{
		final List<IConnectorField> l_fields = new ArrayList<>();

		for (IConnectorField l_conField : p_fields)
		{
			if ("login".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getLogin())));
			}
			else if ("name".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getName())));
			}
			else if ("email".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getEmail())));
			}
			else if ("newIssues".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getNewIssues())));
			}
			else if ("newIssuesUrl".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getNewIssuesUrl())));
			}
			else if ("openIssues".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getOpenIssues())));
			}
			else if ("openIssuesUrl".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getOpenIssuesUrl())));
			}
			else if ("changeDate".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(p_assigneeWithIssues
					.getChangeDate())));
			}
		}

		return l_fields;
	}
}
