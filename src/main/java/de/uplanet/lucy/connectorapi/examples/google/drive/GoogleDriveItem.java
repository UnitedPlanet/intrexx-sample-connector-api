/*
 * $Id$
 *
 * Copyright 2000-2018 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */


package de.uplanet.lucy.connectorapi.examples.google.drive;

import java.net.URI;
import java.util.Date;


public class GoogleDriveItem
{
	public static final String FIELDS = "id,"
	                                  + "name,"
	                                  + "thumbnailLink,"
	                                  + "kind,"
	                                  + "mimeType,"
	                                  + "size,"
	                                  + "description,"
	                                  + "createdTime,"
	                                  + "modifiedTime,"
	                                  + "webViewLink,"
	                                  + "webContentLink";

	private final String m_id;
	private final String m_name;
	private final String m_kind;
	private final String m_mimeType;
	private final URI m_webViewLink;
	private final URI m_thumbnailLink;
	private final URI m_downloadLink;
	private final Date m_createdTime;
	private final Date m_modifiedTime;
	private final Long m_size;
	private final String m_description;
	private final boolean m_isFolder;

	public GoogleDriveItem(String p_id,
	                       String p_name,
	                       String p_kind,
	                       String p_mimeType,
	                       URI    p_thumbnailLink,
	                       Date   p_createdTime,
	                       Date   p_modifiedTime,
	                       Long   p_size,
	                       String p_description,
		URI p_webViewLink, URI p_downloadLink)
	{
		m_id            = p_id;
		m_name          = p_name;
		m_kind          = p_kind;
		m_mimeType      = p_mimeType;
		m_thumbnailLink = p_thumbnailLink;
		m_createdTime   = p_createdTime;
		m_modifiedTime  = p_modifiedTime;
		m_size          = p_size;
		m_description   = p_description;
		m_webViewLink   = p_webViewLink;
		m_downloadLink = p_downloadLink;
		m_isFolder      = "application/vnd.google-apps.folder".equals(m_mimeType);
	}

	public String getId()
	{
		return m_id;
	}

	public String getName()
	{
		return m_name;
	}

	public String getKind()
	{
		return m_kind;
	}

	public URI getWebViewLink()
	{
		return m_webViewLink;
	}

	public String getMimeType()
	{
		return m_mimeType;
	}

	public URI getThumbnailLink()
	{
		return m_thumbnailLink;
	}

	public URI getDownloadLink()
	{
		return m_downloadLink;
	}

	public Date getCreatedTime()
	{
		return m_createdTime;
	}

	public Date getModifiedTime()
	{
		return m_modifiedTime;
	}

	public Long getSize()
	{
		return m_size;
	}

	public String getDescription()
	{
		return m_description;
	}

	public boolean isFolder()
	{
		return m_isFolder;
	}
}
