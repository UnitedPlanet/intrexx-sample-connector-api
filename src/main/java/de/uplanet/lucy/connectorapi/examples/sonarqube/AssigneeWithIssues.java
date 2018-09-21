/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */

package de.uplanet.lucy.connectorapi.examples.sonarqube;

import java.util.Date;

public final class AssigneeWithIssues
{
	private final String m_login;

	private String m_name;
	private String m_email;
	private int m_newIssues;
	private String m_newIssuesUrl;
	private int m_openIssues;
	private String m_openIssuesUrl;
	private Date m_changeDate;


	public AssigneeWithIssues(String p_login)
	{
		m_login = p_login;
	}

	public AssigneeWithIssues(String p_login, String p_name, String p_email)
	{
		this(p_login);
		m_name = p_name;
		m_email = p_email;
	}

	public int getNewIssues()
	{
		return m_newIssues;
	}

	public void setNewIssues(int p_newIssues)
	{
		m_newIssues = p_newIssues;
	}

	public String getNewIssuesUrl()
	{
		return m_newIssuesUrl;
	}

	public void setNewIssuesUrl(String p_newIssuesUrl)
	{
		m_newIssuesUrl = p_newIssuesUrl;
	}

	public int getOpenIssues()
	{
		return m_openIssues;
	}

	public void setOpenIssues(int p_openIssues)
	{
		m_openIssues = p_openIssues;
	}

	public String getOpenIssuesUrl()
	{
		return m_openIssuesUrl;
	}

	public void setOpenIssuesUrl(String p_openIssuesUrl)
	{
		m_openIssuesUrl = p_openIssuesUrl;
	}

	public Date getChangeDate()
	{
		return m_changeDate;
	}

	public void setChangeDate(Date p_changeDate)
	{
		m_changeDate = p_changeDate;
	}

	public String getLogin()
	{
		return m_login;
	}

	public String getName()
	{
		return m_name;
	}

	public String getEmail()
	{
		return m_email;
	}
}