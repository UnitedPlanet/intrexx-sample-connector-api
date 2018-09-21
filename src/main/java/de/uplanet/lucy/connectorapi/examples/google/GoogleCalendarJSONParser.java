package de.uplanet.lucy.connectorapi.examples.google;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.uplanet.lucy.connectorapi.examples.google.calendar.GoogleCalendar;
import de.uplanet.lucy.connectorapi.examples.google.calendar.GoogleEvent;


public class GoogleCalendarJSONParser
{
	public GoogleCalendarJSONParser()
	{
	}

	public List<GoogleCalendar> parseJSONToGoogleCalendars(String p_json) throws Exception
	{
		List<GoogleCalendar> l_result = new ArrayList<GoogleCalendar>();
		JSONObject l_root = (JSONObject) new JSONParser().parse(p_json);
		Object l_items = l_root.get("items");
		if (l_items instanceof JSONArray)
		{
			JSONArray l_array = (JSONArray) l_items;

			for (Object l_item : l_array)
			{
				if (l_item instanceof JSONObject)
				{
					JSONObject l_obj = (JSONObject) l_item;
					l_result.add(parseJSONToGoogleCalendar(l_obj));
				}
			}
		}
		else if (l_items instanceof JSONObject)
		{
			JSONObject l_obj = (JSONObject) l_items;
			l_result.add(parseJSONToGoogleCalendar(l_obj));
		}
		return l_result;
	}

	public GoogleCalendar parseJSONToGoogleCalendar(JSONObject p_obj) throws Exception
	{
		String l_id = (String) p_obj.get("id");
		String l_summary = (String) p_obj.get("summary");
		return new GoogleCalendar(l_id, l_summary);
	}

	public List<GoogleEvent> parseJSONtoGoogleEvents(String p_jsonString) throws Exception
	{
		final List<GoogleEvent> l_events = new ArrayList<GoogleEvent>();
		JSONObject l_obj = (JSONObject) new JSONParser().parse(p_jsonString);

		Object l_items = l_obj.get("items");

		if (l_items instanceof JSONArray)
		{
			JSONArray l_jsonArray = (JSONArray) l_items;
			for (Object l_eventObj : l_jsonArray)
			{
				if (l_eventObj instanceof JSONObject)
				{
					JSONObject l_eventJSON = (JSONObject) l_eventObj;
					l_events.add(parseJSONtoGoogleEvent(l_eventJSON));
				}
			}
		}
		else if (l_items instanceof JSONObject)
		{
			JSONObject l_eventJSON = (JSONObject) l_items;
			l_events.add(parseJSONtoGoogleEvent(l_eventJSON));
		}

		return l_events;
	}

	public GoogleEvent parseJSONtoGoogleEvent(JSONObject p_obj) throws Exception
	{
		SimpleDateFormat l_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		final String l_id = (String) p_obj.get("id");
		final String l_summary = (String) p_obj.get("summary");
		final String l_location = (String) p_obj.get("location");
		final String l_description = (String) p_obj.get("description");
		final String l_htmlLink = (String) p_obj.get("htmlLink");
		final String l_created = (String) p_obj.get("created");
		final String l_updated = (String) p_obj.get("updated");

		final JSONObject l_startJSON = (JSONObject) p_obj.get("start");
		final JSONObject l_endJSON = (JSONObject) p_obj.get("end");
		Date l_start;
		Date l_end;
		try
		{
			l_start = l_sdf.parse((String) l_startJSON.get("dateTime"));
			l_end = l_sdf.parse((String) l_endJSON.get("dateTime"));
		}
		catch (ParseException l_e)
		{
			l_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
			l_start = l_sdf.parse((String) l_startJSON.get("dateTime"));
			l_end = l_sdf.parse((String) l_endJSON.get("dateTime"));
		}

		return new GoogleEvent(l_id,
		                       l_created,
		                       l_updated,
		                       l_summary,
		                       l_start,
		                       l_end,
		                       l_htmlLink,
		                       l_location,
		                       l_description);
	}

	@SuppressWarnings("unchecked")
	public String parseGoogleEventToJSON(GoogleEvent l_event)
	{
		final SimpleDateFormat l_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		final JSONObject l_jsonObject = new JSONObject();

		if (l_event.getStart() != null)
		{
			JSONObject l_dateTime = new JSONObject();
			l_dateTime.put("dateTime", l_sdf.format(l_event.getStart()));
			l_dateTime.put("timezone", "UTC");
			l_jsonObject.put("start", l_dateTime);
		}

		if (l_event.getEnd() != null)
		{
			JSONObject l_dateTime = new JSONObject();
			l_dateTime.put("dateTime", l_sdf.format(l_event.getEnd()));
			l_dateTime.put("timezone", "UTC");
			l_jsonObject.put("end", l_dateTime);
		}

		if (l_event.getSummary() != null)
		{
			l_jsonObject.put("summary", l_event.getSummary());
		}

		if (l_event.getDescription() != null)
		{
			l_jsonObject.put("description", l_event.getDescription());
		}

		if (l_event.getHtmlLink() != null)
		{
			l_jsonObject.put("htmlLink", l_event.getHtmlLink());
		}

		if (l_event.getCreated() != null)
		{
			l_jsonObject.put("created", l_event.getCreated());
		}

		if (l_event.getUpdated() != null)
		{
			l_jsonObject.put("updated", l_event.getUpdated());
		}

		if (l_event.getLocation() != null)
		{
			l_jsonObject.put("location", l_event.getLocation());
		}

		return l_jsonObject.toJSONString();
	}
}
