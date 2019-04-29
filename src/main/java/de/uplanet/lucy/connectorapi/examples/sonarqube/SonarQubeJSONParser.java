
package de.uplanet.lucy.connectorapi.examples.sonarqube;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class SonarQubeJSONParser {

	private final JSONParser m_jsonParser;
	private final String m_sonarIssuesUri;

	public SonarQubeJSONParser(String p_sonarIssuesUri)
	{
		m_jsonParser = new JSONParser();
		m_sonarIssuesUri = p_sonarIssuesUri;
	}

	public List<AssigneeWithIssues> parseJSONToAssigneesWithIssues(String p_json) throws Exception
	{
		final List<AssigneeWithIssues> l_result = new ArrayList<>();

		final JSONArray l_array = _getUsers(p_json);

		JSONObject l_obj;

		for (Object l_user : l_array)
		{
			l_obj = (JSONObject) l_user;
			l_result.add(_parseJSONToAssigneeWithIssues(l_obj));
		}

		return l_result;
	}

	public AssigneeWithIssues getAssigneeWithIssues(String p_json, String p_uid) throws Exception
	{
		final JSONArray l_array = _getUsers(p_json);
		JSONObject l_obj;
		for (Object l_user : l_array)
		{
			l_obj = (JSONObject) l_user;
			if (l_obj.get("login").equals(p_uid))
			{
				return _parseJSONToAssigneeWithIssues(l_obj);
			}
		}
		return null;
	}

	public Map<String, Integer> getSonarPaging(String p_json) throws Exception
	{
		final Map<String, Integer> l_result = new HashMap<>();

		final JSONObject l_root = (JSONObject) m_jsonParser.parse(p_json);
		final JSONObject l_paging = (JSONObject)l_root.get("paging");

		l_result.put("pageSize", ((Long)l_paging.get("pageSize")).intValue());
		l_result.put("total", ((Long)l_paging.get("total")).intValue());

		return Collections.unmodifiableMap(l_result);
	}

	@SuppressWarnings("unchecked")
	public void parseJSONAndCompleteAssigneeWithIssues(List<String> p_json,
	                                                                 AssigneeWithIssues p_assigneeWithIssues) throws Exception
	{
		JSONObject l_root;
		final JSONArray l_issues = new JSONArray();

		for (String l_page : p_json)
		{
			l_root = (JSONObject) m_jsonParser.parse(l_page);
			l_issues.addAll((JSONArray) l_root.get("issues"));
		}

		final String l_openIssuesUrl = l_issues.size() > 0
			? m_sonarIssuesUri + "?statuses=OPEN&assignees=" + p_assigneeWithIssues.getLogin()
			: null;

		final DateFormat l_dfCreatedAfter = new SimpleDateFormat("yyyy-MM-dd");

		final DateFormat l_dfCreationDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		final int l_newIssues = (int) l_issues.stream()
				.filter(issue -> _getCalendarFromString((String) ((JSONObject) issue).get("creationDate"), l_dfCreationDate)
						.compareTo(_getCalendarAtStartOfDay(-1)) >= 0)
				.count();

		final String l_newIssuesUrl = l_newIssues > 0
				? l_openIssuesUrl + "&createdAfter=" + l_dfCreatedAfter.format(_getCalendarAtStartOfDay(-1).getTime())
				: null;

		p_assigneeWithIssues.setNewIssues(l_newIssues);
		p_assigneeWithIssues.setNewIssuesUrl(l_newIssuesUrl);
		p_assigneeWithIssues.setOpenIssues(l_issues.size());
		p_assigneeWithIssues.setOpenIssuesUrl(l_openIssuesUrl);
		p_assigneeWithIssues.setChangeDate(_getCalendarAtStartOfDay(new Date()).getTime());
	}

	private JSONArray _getUsers(String p_json) throws Exception
	{
		final JSONObject l_root = (JSONObject) m_jsonParser.parse(p_json);
		final Object l_users = l_root.get("users");
		final JSONArray l_array = (JSONArray) l_users;
		return l_array;
	}

	private AssigneeWithIssues _parseJSONToAssigneeWithIssues(JSONObject p_obj)
	{
		final String l_uid = (String)p_obj.get("login");
		final String l_name = (String)p_obj.get("name");
		final String l_email = (String)p_obj.get("email");

		return new AssigneeWithIssues(l_uid, l_name, l_email);
	}

	private Calendar _getCalendarAtStartOfDay(int p_NowPlusMinusDays)
	{
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, p_NowPlusMinusDays);
		_setCalendarToStartOfDay(cal);
		return cal;
	}

	private Calendar _getCalendarAtStartOfDay(Date p_atDate)
	{
		final Calendar cal = Calendar.getInstance();
		cal.setTime(p_atDate);
		_setCalendarToStartOfDay(cal);
		return cal;
	}

	private void _setCalendarToStartOfDay(Calendar p_cal)
	{
		p_cal.set(Calendar.HOUR_OF_DAY, 0);
		p_cal.set(Calendar.MINUTE, 0);
		p_cal.set(Calendar.SECOND, 0);
		p_cal.set(Calendar.MILLISECOND, 0);
	}

	private Calendar _getCalendarFromString(String p_date, DateFormat p_sdf)
	{
		final Calendar l_cal = Calendar.getInstance();

		try
		{
			l_cal.setTime(p_sdf.parse(p_date));
		}
		catch (ParseException l_e) {
			throw new RuntimeException(l_e);
		}

		return l_cal;
	}
}