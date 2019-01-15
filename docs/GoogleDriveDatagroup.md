# GoogleDriveDataGroupAdapter implementation

This document explains how you can use the Connector API to develop your own connectors for Intrexx.
This example shows how to consume a REST API (Google Drive) using the Connector API.

## Gradle project

Clone the project and add it to your preferred IDE as a Gradle project.

## Adapter class

1. Create your own package.
2. Create your own `GoogleDriveDataGroupAdapter` class.

### GoogleDriveDataGroupAdapter class

This class must extend the `AbstractConnectorDataGroupAdapter` class.

#### AbstractConnectorDataGroupAdapter class

The `AbstractConnectorDataGroupAdapter` class implements useful methods such as:

* createHTTPClient: This method generates an HTTP client where the OAuth2 and other authentication protocols can be configured in the connector configuration XML.
* getConnectorGuid: Returns the GUID of your connector.
* createODataV2Client: This method creates an ODataV2 client where the OAuth2 and other authentication protocols can be configured with the connector configuration XML.
* createODataV4Client: This method creates an ODataV4 client where the OAuth2 and other authentication protocols can be configured with the connector configuration XML.
* createHttpClient: This method creates an Apache HTTP client where required authentication protocols can be configured with the connector configuration XML.
* getProperties: Returns a collection of properties from the expert settings of your data group.
* getImpersonationGuid: Returns the GUID of an impersonation user.
* getDataGroupGuid: Returns the GUID of your data group.

#### Implement the methods of the IConnectorDataGroupAdapter interface

The Connector API defines the following CRUD methods you can implement:

1. queryDataRange
2. queryDataRecord
3. insert
4. update
5. delete

#### queryDataRange method

This method returns a collection of records. The return object is of type `IConnectorQueryResult`. It contains a list of records, the count of records in the list and the total count of records the external API can return.

The count and total count is needed for the pagination control. With the abstract class method, we create an HTTP client. This client performs the OAuth2 authentication to the Google service, if the connector is configured correctly. See the quick start guide for more details.

The request method is extracted into a separate service class. For the request we need the Google Drive parent ID (the item ID of the parent folder). This ID comes from an expert flag in the data group. With getProperties() we get access to all expert flags of the data group.

With getFields we get all the data fields we have to provide values for.

```Java
	@Override
	public IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)
	{
		final HttpClient l_httpClient = createHttpClient(getConnectorGuid(), null);
		final List<IConnectorRecord> l_records;

		l_records = m_service.getItemsInFolder(l_httpClient,
		                                       getProperties().getString(GOOGLE_DRIVE_CONSTANT.FOLDER_ID),
		                                       p_criteria.getFields(),
		                                       true);

		return new ConnectorQueryResult(l_records, l_records.size(), l_records.size());
	}
```

### queryDataRecord() method

This method will be called if Intrexx requests a single record. The implementation will call the Google Drive API and return an `IConnectorRecord` object.

```Java
	@Override
	public IConnectorRecord queryDataRecord(String p_strRecordId, List<IConnectorField> p_fields)
	{
		HttpClient l_client = createHttpClient(getConnectorGuid(), null);

		return m_service.getRecord(l_client, p_strRecordId, p_fields);
	}
```

### insert() method

This method is called once the insert method of the file adapter has created a drive item. This method will subsequently add the metadata to the drive item.

```Java
	@Override
	public String insert(IConnectorRecord p_record)
	{
		return m_service.updateMetaDataItem(createHttpClient(getConnectorGuid(), null), p_record);
	}
```

### update() method

This method updates the metadata of the drive item.

```Java
	@Override
	public boolean update(IConnectorRecord p_record)
	{
		String l_id = m_service.updateMetaDataItem(createHttpClient(getConnectorGuid(), null), p_record);

		if (l_id == null || l_id.isEmpty() || "-1".equals(l_id))
			return false;

		return true;
	}
```

### delete() method

This method deletes a drive item in your Google Drive.

```Java
	@Override
	public void delete(String p_strRecordId)
	{
		HttpClient l_client = createHttpClient(getConnectorGuid(), null);
		m_service.deleteRecord(l_client, p_strRecordId);
	}
```