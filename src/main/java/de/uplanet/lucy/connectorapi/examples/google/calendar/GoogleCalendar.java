
package de.uplanet.lucy.connectorapi.examples.google.calendar;


public class GoogleCalendar
{
	private final String m_id;
	private final String m_summary;

	public GoogleCalendar(String p_summary)
	{
		this(null, p_summary);
	}

	public GoogleCalendar(String p_id, String p_summary)
	{
		m_id = p_id;
		m_summary = p_summary;
	}

	public String getId()
	{
		return m_id;
	}

	public String getSummary()
	{
		return m_summary;
	}
}
