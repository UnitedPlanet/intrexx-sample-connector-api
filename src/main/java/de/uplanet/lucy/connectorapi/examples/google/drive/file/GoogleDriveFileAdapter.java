/*
 * $Id: Office365OneDriveFileAdapter.java 180718 2018-03-29 14:33:47Z ManuelR $
 *
 * Copyright 2000-2017 United Planet GmbH, Freiburg Germany
 * All Rights Reserved.
 */

package de.uplanet.lucy.connectorapi.examples.google.drive.file;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.io.IOHelper;
import de.uplanet.lucy.connectorapi.examples.google.drive.GoogleDriveItem;
import de.uplanet.lucy.server.ContextSession;
import de.uplanet.lucy.server.ContextUser;
import de.uplanet.lucy.server.IProcessingContext;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderHelper;
import de.uplanet.lucy.server.file.VHFileAdapterDescriptor;
import de.uplanet.lucy.server.file.action.IOperationFile;
import de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorFileAdapter;
import de.uplanet.lucy.server.odata.connector.api.v1.Field;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.odata.consumer.office365.Office365ConnectorException;
import de.uplanet.lucy.server.portalserver.PortalServerPath;
import de.uplanet.lucy.server.property.IPropertyCollection;
import de.uplanet.lucy.server.rtcache.FieldInfo;
import de.uplanet.lucy.server.util.ContentTypeUtil;
import de.uplanet.lucy.server.util.FileMetaInfo;
import de.uplanet.lucy.server.util.IVHFileAdapterDescriptor;
import de.uplanet.util.Preconditions;


public final class GoogleDriveFileAdapter extends AbstractConnectorFileAdapter
{
	private static final Logger ms_log = LoggerFactory.getLogger(GoogleDriveFileAdapter.class);

	private final GoogleDriveFileService m_service;

	public GoogleDriveFileAdapter(IProcessingContext  p_ctx,
	                              FieldInfo           p_adapterInfo,
	                              IPropertyCollection p_properties,
	                              String              p_strImpersonationGuid)
	{
		super(p_ctx, p_adapterInfo, p_properties, p_strImpersonationGuid);

		m_service = new GoogleDriveFileService();
	}

	@Override
	public List<IVHFileAdapterDescriptor> createFileDescriptors(IConnectorRecord p_record,
	                                                            String           p_strDgGuid,
	                                                            String           p_strFieldGuid,
	                                                            TimeZone         p_tz)
	{
		if (p_record == null || p_record.getId() == null || p_record.getId().isEmpty() ||  p_record.getId().equals("-1"))
		{
			return Collections.emptyList();
		}
		else
		{
			return _getRecordFiles(p_record, p_tz);
		}
	}


	@Override
	public boolean itemExists(IConnectorRecord p_record)
	{
		if (p_record.getId().equalsIgnoreCase("-1"))
			return false;

		try
		{
			final GoogleDriveItem l_result = m_service.getDriveItem(createHttpClient(), p_record.getId());

			return l_result != null && l_result.getId() != null;
		}
		catch (Exception l_e)
		{
			throw new RuntimeException(l_e);
		}
	}


	@Override
	public IVHFileAdapterDescriptor getFileByName(IConnectorRecord p_record,
	                                              String p_strFileName)
		throws Exception
	{
		Preconditions.requireNonNull(p_record.getId(), "RecordId is required!");
		final GoogleDriveItem l_driveItem;

		l_driveItem = m_service.getDriveItem(createHttpClient(), p_record.getId());

		if (l_driveItem == null)
			return null;

		String l_downloadLink = null;
		String l_weblink = null;

		if (l_driveItem.getDownloadLink() != null)
			l_downloadLink = l_driveItem.getDownloadLink().toASCIIString();

		if (l_driveItem.getWebViewLink() != null)
			l_weblink = l_driveItem.getWebViewLink().toASCIIString();

		return _toFileDescriptor(p_record.getId(),
		                         l_driveItem.getId(),
		                         l_driveItem.getName(),
		                         l_driveItem.getName(),
		                         l_weblink,
		                         l_downloadLink,
		                         l_driveItem.getSize(),
		                         l_driveItem.getModifiedTime(),
		                         1);
	}


	@Override
	public IVHFileAdapterDescriptor getFileById(IConnectorRecord p_record,
	                                            String p_strFileId)
	{
		Preconditions.requireNonNull(p_record.getId(), "RecordId is required.");

		final GoogleDriveItem l_file;

		l_file = m_service.getDriveItem(createHttpClient(), p_record.getId());

		if(l_file != null)
		{
			String l_downloadLink = null;
			String l_weblink = null;

			if (l_file.getDownloadLink() != null)
				l_downloadLink = l_file.getDownloadLink().toASCIIString();

			if (l_file.getWebViewLink() != null)
				l_weblink = l_file.getWebViewLink().toASCIIString();

			return _toFileDescriptor(p_record.getId(),
			                         l_file.getId(),
			                         l_file.getName(),
			                         l_file.getName(),
			                         l_weblink,
			                         l_downloadLink,
			                         l_file.getSize(),
			                         l_file.getModifiedTime(),
			                         1);
		}

		return null;
	}


	@Override
	public int getFileCount(String p_strRecId)
	{
		if (p_strRecId == null || p_strRecId.isEmpty() || p_strRecId.equals("-1"))
		{
			return 0;
		}
		else
		{
			return m_service.getFileCount(createHttpClient(), p_strRecId);
		}
	}


	@Override
	public IVHFileAdapterDescriptor downloadToLocalFile(IConnectorRecord p_record,
	                                                    String p_strFileId)
			throws Exception
	{
		final List<IVHFileAdapterDescriptor> l_descr = createFileDescriptors(p_record, null, null,
																			ContextUser.get().getTimeZone());
		if (l_descr.isEmpty())
			throw new Office365ConnectorException("Cannot find file.");

		IVHFileAdapterDescriptor l_vhf =
				l_descr.stream().filter(p_descr -> p_descr.getFileId().equals(p_strFileId))
								.findFirst()
								.orElseThrow(() -> new RuntimeException("Cannot find file."));

		final String l_strUrl = l_vhf.getMetaInfo().getValue("downloadurl");
		Preconditions.requireNonNullNonEmpty(l_strUrl, "Cannot get file URL.");

		final File l_fileTmpDir;
		l_fileTmpDir = IOHelper.createTempDirectory(PortalServerPath.get(PortalServerPath.TMP_DIR),
													ContextSession.get().getId(), "_connector");

		final Path l_p = l_fileTmpDir.toPath().resolve(l_vhf.getFileName());
		final File l_fileTarget = l_p.toFile();
		m_service.downloadFile(createHttpClient(), l_vhf.getMetaInfo().getValue("downloadurl"), l_fileTarget);

		final String l_strFileName;
		final String l_strContentType;
		final String l_strMetadata;

		l_strFileName    = l_fileTarget.getName();
		l_strContentType = ContentTypeUtil.contentTypeFromFileName(l_strFileName);
		l_strMetadata    = "fileSize= " + l_fileTarget.length();

		return new VHFileAdapterDescriptor(m_fieldInfo.getGuid(),
											p_record.getId(),
		                                    p_strFileId,
		                                    l_p.toFile(),
		                                    l_strFileName,
		                                    l_strContentType,
		                                    l_vhf.getLastModified(),
		                                    FileMetaInfo.fromString(l_strMetadata),
		                                    1,
		                                    true);
	}


	@Override
	public IConnectorRecord createFiles(IConnectorRecord p_record,
                                        List<IOperationFile> p_operationFiles)
		throws Exception
	{
		if (p_operationFiles.size() != 1 && isSingleFileField(m_properties))
		{
			throw new Office365ConnectorException("Invalid number of file operations for single file field. " +
				"Please change the file upload control behavior to 'replace'.");
		}

		String l_strParentId = _getParentId();

		final List<IConnectorField> l_return = new ArrayList<>();

		if (isSingleFileField(m_properties))
		{
			final IOperationFile l_file = p_operationFiles.get(0);

			if (m_service.fileNameExists(createHttpClient(), l_strParentId, l_file.getFileName()))
			{
				throw new Office365ConnectorException("File name conflict: There is already a file item in the given"
					+ " OneDrive subfolder. Please rename your file before uploading (file name: " + l_file
						.getFileName() + ").");
			}

			final String l_parentId = m_properties.getString(GOOGLE_DRIVE_CONSTANT.FOLDER_ID);
			final String l_filename = ValueHolderHelper.getStringFromVH(
					p_record.getFieldByGuid(m_properties.getString(GOOGLE_DRIVE_CONSTANT.NAME_GUID)).getValue());

			final GoogleDriveItem l_item = m_service.uploadFile(createHttpClient(), l_file, l_filename, l_parentId);

			l_return.add(new Field(m_properties.getGuid(GOOGLE_DRIVE_CONSTANT.ITEM_ID), ValueHolderFactory.getValueHolder(l_item.getId())));
		}
		else
		{
			throw new UnsupportedOperationException();
		}

		return p_record.putFields(l_return);
	}


	@Override
	public IConnectorRecord updateFiles(IConnectorRecord p_record,
                                        List<IOperationFile>         p_newFiles,
                                        List<IOperationFile>         p_replacedFiles,
                                        List<IOperationFile>         p_deletedFiles)
		throws Exception
	{
		if (itemExists(p_record))
		{
			if (p_replacedFiles.size() == 1)
			{
				final String l_strParentId = _getParentId();
				final String l_filename = ValueHolderHelper.getStringFromVH(
						p_record.getFieldByGuid(m_properties.getString(GOOGLE_DRIVE_CONSTANT.NAME_GUID)).getValue());

				if (m_service.fileNameExists(createHttpClient(), l_strParentId, l_filename))
				{
					return replaceFiles(p_record, p_replacedFiles);
				}
			}
		}

		return p_record;
	}


	@Override
	public IConnectorRecord replaceFiles(IConnectorRecord p_record,
	                                     List<IOperationFile> p_replacedFiles)
			throws Exception
	{
		if (p_replacedFiles.size() > 1 && isSingleFileField(m_properties))
			throw new IllegalArgumentException("Invalid number of file operations for single file field.");

		final List<IConnectorField> l_return = new ArrayList<>();

		final String l_parentId = m_properties.getString(GOOGLE_DRIVE_CONSTANT.FOLDER_ID);
		final String l_filename = ValueHolderHelper.getStringFromVH(
				p_record.getFieldByGuid(m_properties.getString(GOOGLE_DRIVE_CONSTANT.NAME_GUID)).getValue());

		final GoogleDriveItem l_item = m_service.replaceFile(createHttpClient(),
		                                                     p_replacedFiles.get(0),
		                                                     p_record.getId(),
		                                                     l_filename,
		                                                     l_parentId);

		l_return.add(new Field(m_properties.getGuid(GOOGLE_DRIVE_CONSTANT.ITEM_ID), ValueHolderFactory.getValueHolder(l_item.getId())));

		return p_record.putFields(l_return);
	}


	@Override
	public boolean isSingleFileField(IPropertyCollection p_properties)
	{
		//SUPPORT ONLY SINGLE FILE UPLOAD
		return true;
	}


	@Override
	public IConnectorRecord deleteFiles(IConnectorRecord p_record)
		throws Exception
	{
		//IMPLEMENTED BY GoogleDriveDataGroupAdapter
		return p_record;
	}


	@Override
	public IConnectorRecord deleteFiles(IConnectorRecord p_record,
	                                                List<IOperationFile> p_operationFiles)
		throws Exception
	{
		//IMPLEMENTED BY GoogleDriveDataGroupAdapter
		return p_record;
	}


	@Override
	public boolean login(String p_strUserGuid)
	{
		final String l_usrGuid;

		if (m_strImpersonationGuid != null)
			l_usrGuid = m_strImpersonationGuid;
		else
			l_usrGuid = p_strUserGuid;

		return m_service.isAuthenticated(getConfigurationId(), l_usrGuid);
	}

	private IVHFileAdapterDescriptor _toFileDescriptor(String p_strRecId,
	                                                   String p_strFileId,
	                                                   String p_strFileName,
	                                                   String p_strPath,
	                                                   String p_strWebUrl,
	                                                   String p_strDownloadUrl,
	                                                   Long   p_lSize,
	                                                   Date   p_dtModified,
	                                                   int    p_iOrder)
	{
		final String l_strName;
		final String l_strUrl;
		final String l_strContent;
		final String l_strMetaData;
		final Date l_dtModified;

		if (p_strFileName == null)
			throw new IllegalArgumentException("Cannot create file descriptor. Add the file name field to the page.");

		if (p_strPath == null)
			throw new IllegalArgumentException("Cannot create file descriptor. Add the path field to the page.");

		l_strName     = p_strFileName;
		l_strUrl      = p_strFileName;
		l_strContent  = ContentTypeUtil.contentTypeFromFileName(p_strFileName);
		l_strMetaData = "fileSize= " + p_lSize;

		if (p_dtModified != null)
			l_dtModified = p_dtModified;
		else
			l_dtModified = new Date();

		final FileMetaInfo l_fileInfo = FileMetaInfo.fromString(l_strMetaData);
		l_fileInfo.setValue("weburl", p_strWebUrl);
		l_fileInfo.setValue("downloadurl", p_strDownloadUrl);

		final IVHFileAdapterDescriptor l_desc;

		l_desc = new VHFileAdapterDescriptor(m_fieldInfo.getGuid(),
											p_strRecId,
											p_strFileId,
											new File(l_strUrl),
											l_strName,
											l_strContent,
											l_dtModified,
											l_fileInfo,
											p_iOrder,
											true);
		return l_desc;
	}

	private List<IVHFileAdapterDescriptor> _getRecordFiles(IConnectorRecord p_record, TimeZone p_tz)
	{
		final List<IVHFileAdapterDescriptor> l_descriptors;

		l_descriptors = new LinkedList<>();

		try
		{
			final GoogleDriveItem l_file = m_service.getDriveItem(createHttpClient(), p_record.getId());
			if (l_file != null)
			{
				String l_downloadLink = null;
				String l_weblink = null;

				if (l_file.getDownloadLink() != null)
					l_downloadLink = l_file.getDownloadLink().toASCIIString();

				if (l_file.getWebViewLink() != null)
					l_weblink = l_file.getWebViewLink().toASCIIString();

				l_descriptors.add(_toFileDescriptor(p_record.getId(),
				                                    l_file.getId(),
				                                    l_file.getName(),
				                                    l_file.getName(),
				                                    l_weblink,
				                                    l_downloadLink,
				                                    l_file.getSize(),
				                                    l_file.getModifiedTime(),
				                                    1));
			}


			return l_descriptors;
		}
		catch (Exception l_e)
		{
			ms_log.error("Cannot get document with ID " + p_record.getId() + ". "
				+ "Please verify that the OneDrive ID field is available in the current record and that the "
				+ "referenced file exists in OneDrive.", l_e);

			return Collections.emptyList();
		}
	}

	private String _getParentId()
	{
		return m_properties.getString(GOOGLE_DRIVE_CONSTANT.FOLDER_ID, "root");
	}
}
