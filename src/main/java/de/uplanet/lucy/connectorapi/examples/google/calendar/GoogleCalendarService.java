/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */


package de.uplanet.lucy.connectorapi.examples.google.calendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.lucy.connectorapi.examples.google.GoogleCalendarJSONParser;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory;
import de.uplanet.lucy.server.odata.connector.api.v1.Field;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.odata.connector.api.v1.Record;


public class GoogleCalendarService
{
	private static final String ms_rootUri = "https://www.googleapis.com/calendar/v3/";
	private static final Logger ms_log = LoggerFactory.getLogger(GoogleCalendarService.class);

	public GoogleCalendarService()
	{
	}

	public List<IConnectorRecord> getCalendarList(HttpClient p_httpClient, List<IConnectorField> p_fields)
		throws Exception
	{
		final List<IConnectorRecord> l_result = new ArrayList<IConnectorRecord>();
		final HttpResponse l_response;
		final URI l_uri = URI.create("https://www.googleapis.com/calendar/v3/users/me/calendarList");
		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();
		l_response = p_httpClient.execute(l_request);

		List<GoogleCalendar> l_calendars = new GoogleCalendarJSONParser().parseJSONToGoogleCalendars(_getStringFromResponse(
			l_response));

		for (GoogleCalendar l_calendar : l_calendars)
		{
			List<IConnectorField> l_fields = new ArrayList<IConnectorField>();
			for (IConnectorField l_field : p_fields)
			{
				if ("id".equalsIgnoreCase(l_field.getName()))
				{
					l_fields.add(new Field(l_field.getGuid(), ValueHolderFactory.getValueHolder(l_calendar.getId())));
				}
				else if ("Name".equals(l_field.getName()))
				{
					l_fields.add(new Field(l_field.getGuid(), ValueHolderFactory.getValueHolder(l_calendar
						.getSummary())));
				}
			}
			l_result.add(new Record(l_calendar.getId(), l_fields));
		}

		return l_result;
	}

	public List<IConnectorRecord> getEventList(HttpClient            p_httpClient,
	                                           List<IConnectorField> p_fields,
	                                           String                p_calId,
	                                           String                p_start,
		String p_end) throws Exception
	{
		final HttpResponse l_response;
		final List<GoogleEvent> l_events;
		final List<IConnectorRecord> l_result = new ArrayList<IConnectorRecord>();

		final String l_timeMax = "timeMin=" + p_start;
		final String l_timeMin = "timeMax=" + p_end;
		final String l_timezone = "timeZone=UTC";
		final String params = "?" + l_timeMax + "&" + l_timeMin + "&" + l_timezone;

		final URI l_uri = URI.create(ms_rootUri + "calendars/" + p_calId + "/events" + params);
		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();

		l_response = p_httpClient.execute(l_request);

		l_events = new GoogleCalendarJSONParser().parseJSONtoGoogleEvents(_getStringFromResponse(l_response));

		for (GoogleEvent l_event : l_events)
		{
			List<IConnectorField> l_fields = _getConnectorFields(p_fields, l_event);

			l_result.add(new Record(l_event.getId(), l_fields));
		}

		return l_result;
	}

	public IConnectorRecord getEvent(HttpClient            p_httpClient,
	                                 List<IConnectorField> p_fields,
	                                 String                p_calId,
		String p_record) throws Exception
	{
		final HttpResponse l_response;
		final URI l_uri = URI.create(ms_rootUri + "calendars/" + p_calId + "/events/" + p_record + "?timeZone=UTC");
		final HttpUriRequest l_request = RequestBuilder.get(l_uri).build();
		l_response = p_httpClient.execute(l_request);

		GoogleEvent l_event = _getGoogleEventFromResponse(l_response);

		return new Record(l_event.getId(), _getConnectorFields(p_fields, l_event));
	}

	public String createEvent(HttpClient p_httpClient, String p_calId, IConnectorRecord p_record) throws Exception
	{
		GoogleEvent l_event = _parseRecordToGoogleEvent(p_record);

		final URI l_uri = URI.create(ms_rootUri + "calendars/" + p_calId + "/events");

		StringEntity entity = new StringEntity(new GoogleCalendarJSONParser().parseGoogleEventToJSON(l_event));
		entity.setContentType("application/json");
		final HttpUriRequest l_request = RequestBuilder.post(l_uri).setEntity(entity).build();

		HttpResponse l_response = p_httpClient.execute(l_request);

		return _getGoogleEventFromResponse(l_response).getId();
	}

	public boolean updateEvent(HttpClient p_httpClient, IConnectorRecord p_record, String p_calId, String p_recId)
		throws Exception
	{
		GoogleEvent l_event = _parseRecordToGoogleEvent(p_record);

		final URI l_uri = URI.create(ms_rootUri + "calendars/" + p_calId + "/events/"
			+ p_recId);

		StringEntity entity = new StringEntity(new GoogleCalendarJSONParser().parseGoogleEventToJSON(l_event));
		entity.setContentType("application/json");
		final HttpUriRequest l_request = RequestBuilder.patch(l_uri).setEntity(entity).build();

		HttpResponse l_response = p_httpClient.execute(l_request);

		ms_log.info("Response:" + l_response.getStatusLine() + "\n" + _getStringFromResponse(l_response));

		return true;
	}

	public boolean deleteEvent(HttpClient p_httpClient, String p_calId, String p_recordId) throws Exception
	{
		final URI l_uri = URI.create(ms_rootUri + "calendars/" + p_calId + "/events/"
			+ p_recordId);
		final HttpUriRequest l_request = RequestBuilder.delete(l_uri).build();
		HttpResponse l_response = p_httpClient.execute(l_request);

		if (l_response.getStatusLine().getStatusCode() == 204)
			return true;
		else
			return false;
	}

	private GoogleEvent _parseRecordToGoogleEvent(IConnectorRecord p_record)
	{
		String l_id = null;
		String l_subject = null;
		Date l_end = null;
		Date l_start = null;
		String l_body = null;

		for (IConnectorField l_field : p_record.getFields())
		{
			if ("id".equalsIgnoreCase(l_field.getName()))
			{
				l_id = (String) l_field.getValue().getValue();
			}
			else if ("subject".equalsIgnoreCase(l_field.getName()))
			{
				l_subject = (String) l_field.getValue().getValue();
			}
			else if ("endDate".equalsIgnoreCase(l_field.getName()))
			{
				l_end = (Date) l_field.getValue().getValue();
			}
			else if ("startDate".equalsIgnoreCase(l_field.getName()))
			{
				l_start = (Date) l_field.getValue().getValue();
			}
			else if ("body".equalsIgnoreCase(l_field.getName()))
			{
				l_body = (String) l_field.getValue().getValue();
			}
		}

		return new GoogleEvent(l_id, l_subject, l_start, l_end, l_body);
	}

	private GoogleEvent _getGoogleEventFromResponse(final HttpResponse l_response) throws Exception
	{
		String l_json = _getStringFromResponse(l_response);
		if (l_response.getStatusLine().getStatusCode() > 205)
			throw new RuntimeException("Failure at Response: " + l_response.getStatusLine().toString() + "\n" + l_json);

		return new GoogleCalendarJSONParser().parseJSONtoGoogleEvent((JSONObject) new JSONParser().parse(l_json));
	}

	private String _getStringFromResponse(final HttpResponse l_response) throws IOException
	{
		InputStream l_inputStream = l_response.getEntity().getContent();
		StringWriter writer = new StringWriter();
		IOUtils.copy(l_inputStream, writer, "UTF-8");
		String l_string = writer.toString();
		return l_string;
	}

	private List<IConnectorField> _getConnectorFields(List<IConnectorField> p_fields, GoogleEvent l_event)
	{
		List<IConnectorField> l_fields = new ArrayList<>();
		for (IConnectorField l_conField : p_fields)
		{
			if ("id".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(l_event.getId())));
			}
			else if ("Subject".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(l_event.getSummary())));
			}
			else if ("WebUrl".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(l_event.getHtmlLink())));
			}
			else if ("body".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(),
				                       ValueHolderFactory.getValueHolder(l_event.getDescription())));
			}
			else if ("startDate".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(l_event.getStart())));
			}
			else if ("endDate".equalsIgnoreCase(l_conField.getName()))
			{
				l_fields.add(new Field(l_conField.getGuid(), ValueHolderFactory.getValueHolder(l_event.getEnd())));
			}
		}
		return l_fields;
	}
}
