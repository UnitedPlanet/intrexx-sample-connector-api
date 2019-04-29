
package de.uplanet.lucy.connectorapi.examples.google.calendar;

import java.util.Date;


public final class GoogleEvent
{
	private final String m_id;
	private final String m_created;
	private final String m_updated;
	private final String m_summary;
	private final Date m_start;
	private final Date m_end;
	private final String m_htmlLink;
	private final String m_location;
	private final String m_description;


	public GoogleEvent(String p_summary, Date p_start, Date p_end, String p_description)
	{
		this(null, p_summary, p_start, p_end, p_description);
	}

	public GoogleEvent(String p_id, String p_summary, Date p_start, Date p_end, String p_description)
	{
		this(p_id, p_summary, p_start, p_end, null, p_description);
	}

	public GoogleEvent(String p_id, String p_summary, Date p_start, Date p_end, String p_location, String p_description)
	{
		this(p_id, null, null, p_summary, p_start, p_end, null, p_location, p_description);
	}

	public GoogleEvent(String p_id,
	                   String p_created,
	                   String p_updated,
	                   String p_summary,
	                   Date   p_start,
	                   Date   p_end,
	                   String p_htmlLink,
	                   String p_location,
	                   String p_description)
	{
		m_id = p_id;
		m_created = p_created;
		m_updated = p_updated;
		m_summary = p_summary;
		m_start = p_start;
		m_end = p_end;
		m_htmlLink = p_htmlLink;
		m_location = p_location;
		m_description = p_description;
	}

	/**
	 * @return the id
	 */
	public String getId()
	{
		return m_id;
	}

	/**
	 * @return the created
	 */
	public String getCreated()
	{
		return m_created;
	}

	/**
	 * @return the updated
	 */
	public String getUpdated()
	{
		return m_updated;
	}

	/**
	 * @return the summary
	 */
	public String getSummary()
	{
		return m_summary;
	}

	/**
	 * @return the start
	 */
	public Date getStart()
	{
		return m_start;
	}

	/**
	 * @return the end
	 */
	public Date getEnd()
	{
		return m_end;
	}

	/**
	 * @return the htmlLink
	 */
	public String getHtmlLink()
	{
		return m_htmlLink;
	}

	/**
	 * @return the location
	 */
	public String getLocation()
	{
		return m_location;
	}

	/**
	 * @return the description
	 */
	public String getDescription()
	{
		return m_description;
	}
}
