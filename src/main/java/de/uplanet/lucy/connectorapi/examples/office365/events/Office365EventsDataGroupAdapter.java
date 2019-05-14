package de.uplanet.lucy.connectorapi.examples.office365.events;


import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.uplanet.lucy.server.rtcache.FieldInfo;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityCreateRequest;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.apache.olingo.client.api.communication.request.cud.UpdateType;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.response.ODataDeleteResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityCreateResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.format.ContentType;
import org.odata4j.expression.PrintExpressionVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uplanet.lucy.connectorapi.examples.calendar.CalendarFilterVisitor;
import de.uplanet.lucy.server.IProcessingContext;
import de.uplanet.lucy.server.dataobjects.IValueHolder;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory;
import de.uplanet.lucy.server.odata.connector.ConfigurationRegistry;
import de.uplanet.lucy.server.odata.connector.IConfigurationRegistry;
import de.uplanet.lucy.server.odata.connector.api.cfg.ConnectorConfiguration;
import de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter;
import de.uplanet.lucy.server.odata.connector.api.v1.ConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.Field;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryCriteria;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.odata.connector.api.v1.Record;
import de.uplanet.lucy.server.odata.consumer.office365.IOffice365OneDriveService;
import de.uplanet.lucy.server.odata.consumer.office365.IOffice365OneDriveSupport;
import de.uplanet.lucy.server.odata.consumer.office365.Office365OneDriveService;
import de.uplanet.lucy.server.property.IPropertyCollection;
import de.uplanet.lucy.server.rtcache.DataGroupInfo;
import de.uplanet.lucy.server.rtcache.RtCache;


/**
 * This class provides access to Office365 calendar events to an Intrexx data group.
 */
public final class Office365EventsDataGroupAdapter extends AbstractConnectorDataGroupAdapter
{
	private static final Logger ms_log = LoggerFactory.getLogger(Office365EventsDataGroupAdapter.class);

	private static final String ms_rootUri = "https://graph.microsoft.com/v1.0/";

	public Office365EventsDataGroupAdapter(IProcessingContext  p_ctx,
	                                       String              p_strDataGroupGuid,
	                                       IPropertyCollection p_properties,
	                                       String              p_strImpersonationGuid)
	{
		this(p_ctx, p_strDataGroupGuid, p_properties, p_strImpersonationGuid, null);
	}

	public Office365EventsDataGroupAdapter(IProcessingContext        p_ctx,
	                                       String                    p_strDataGroupGuid,
	                                       IPropertyCollection       p_properties,
	                                       String                    p_strImpersonationGuid,
	                                       IOffice365OneDriveSupport p_support)
	{
		super(p_ctx, p_strDataGroupGuid, p_properties, p_strImpersonationGuid);
	}

	@Override
	public IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)
	{
		final PrintExpressionVisitor l_visit = new PrintExpressionVisitor();
		l_visit.visitNode(p_criteria.getFilterExpression());
		ms_log.info("Filter expression: " + l_visit.toString());

		final CalendarFilterVisitor l_cvisit = new CalendarFilterVisitor();
		l_cvisit.visitNode(p_criteria.getFilterExpression());

		final String l_startDate = l_cvisit.getStart().toString("yyyy-MM-dd'T'HH:mm:ss.sss");
		final String l_endDate = l_cvisit.getEnd().toString("yyyy-MM-dd'T'HH:mm:ss.sss");

		ODataClient l_odataClient = createODataV4Client(_getConnectorGuid(), null);

		URI l_uri = l_odataClient.newURIBuilder(ms_rootUri).appendEntitySetSegment("me/calendarView?startDateTime="
			+ l_startDate + "&endDateTime=" + l_endDate).build();

		ODataEntitySetRequest<ClientEntitySet> l_request = l_odataClient.getRetrieveRequestFactory()
			.getEntitySetRequest(l_uri);

		ODataRetrieveResponse<ClientEntitySet> l_response = l_request.execute();

		ClientEntitySet l_entitySet = l_response.getBody();

		FieldInfo l_fieldPK = RtCache.getPrimaryKeyFields(fieldInfo ->
                getDataGroupGuid().equals(fieldInfo.getDataGroupGuid())).get(0);
		IConnectorField l_fieldID = new Field(l_fieldPK.getGuid());
		List<IConnectorField> allFields = new ArrayList<IConnectorField>();
		allFields.addAll(p_criteria.getFields());
		allFields.add(l_fieldID);

		List<IConnectorRecord> l_records = _getDataRange(allFields, l_entitySet);
		return new ConnectorQueryResult(l_records, l_records.size(), l_records.size());
	}

	@Override
	public IConnectorRecord queryDataRecord(String p_strRecordId, List<IConnectorField> p_fields)
	{
		IOffice365OneDriveService l_service = Office365OneDriveService.newInstance();

		ODataClient l_client = l_service.createODataClient(_getConnectorGuid(), null);

		URI l_uri = URI.create(ms_rootUri + "me/events/" + p_strRecordId);

		final ODataEntityRequest<ClientEntity> l_request = l_client.getRetrieveRequestFactory().getEntityRequest(l_uri);
		l_request.setPrefer("outlook.body-content-type=\"text\"");

		final ODataRetrieveResponse<ClientEntity> l_response = l_request.execute();

		final ClientEntity l_responseEntity = l_response.getBody();

		return _getValueMapForEntity(p_fields, l_responseEntity);
	}

	@Override
	public String insert(IConnectorRecord p_record)
	{
		String l_subject = null;
		Date l_end = null;
		Date l_start = null;
		String l_body = null;

		for (IConnectorField l_field : p_record.getFields())
		{
			if ("subject".equalsIgnoreCase(l_field.getName()))
			{
				l_subject = (String) l_field.getValue().getValue();
			}
			else if ("endDateTime".equalsIgnoreCase(l_field.getName()))
			{
				l_end = (Date) l_field.getValue().getValue();
			}
			else if ("startDateTime".equalsIgnoreCase(l_field.getName()))
			{
				l_start = (Date) l_field.getValue().getValue();
			}
			else if ("body".equalsIgnoreCase(l_field.getName()))
			{
				l_body = (String) l_field.getValue().getValue();
			}
		}

		try
		{
			return _executeInsertUpdateRequest(null, l_subject, l_end, l_start, l_body);
		}
		catch (EdmPrimitiveTypeException l_e)
		{
			throw new RuntimeException("", l_e);
		}
	}

	@Override
	public boolean update(IConnectorRecord p_record)
	{
		String l_subject = null;
		Date l_end = null;
		Date l_start = null;
		String l_body = null;

		for (IConnectorField l_field : p_record.getFields())
		{
			if ("subject".equalsIgnoreCase(l_field.getName()))
			{
				l_subject = (String) l_field.getValue().getValue();
			}
			else if ("endDateTime".equalsIgnoreCase(l_field.getName()))
			{
				l_end = (Date) l_field.getValue().getValue();
			}
			else if ("startDateTime".equalsIgnoreCase(l_field.getName()))
			{
				l_start = (Date) l_field.getValue().getValue();
			}
			else if ("body".equalsIgnoreCase(l_field.getName()))
			{
				l_body = (String) l_field.getValue().getValue();
			}
		}

		try
		{
			String l_id = _executeInsertUpdateRequest(p_record.getId(),
			                                          l_subject,
			                                          l_end,
			                                          l_start,
			                                          l_body);

			return !l_id.isEmpty();
		}
		catch (EdmPrimitiveTypeException l_e)
		{
			throw new RuntimeException("", l_e);
		}
	}

	@Override
	public void delete(String p_strRecordId)
	{
		final IOffice365OneDriveService l_service = Office365OneDriveService.newInstance();
		final ODataClient l_client = l_service.createODataClient(_getConnectorGuid(), null);
		final URI l_uri = URI.create(ms_rootUri + "me/events/" + p_strRecordId);

		ODataDeleteResponse l_response = l_client.getCUDRequestFactory().getDeleteRequest(l_uri).execute();

		if (l_response.getStatusCode() != 204)
			throw new RuntimeException("Deleting failed: " + l_response.getStatusMessage());
	}

	private String _getConnectorGuid()
	{
		IConfigurationRegistry l_cfgRegistry = ConfigurationRegistry.getInstance();
		DataGroupInfo l_dgInfo = RtCache.getDataGroup(getDataGroupGuid());
		ConnectorConfiguration l_cfg = l_cfgRegistry.getConnectorConfiguration(l_dgInfo.getConnectString());
		return l_cfg.getGuid();
	}

	private List<IConnectorRecord> _getDataRange(List<IConnectorField> p_fields, ClientEntitySet l_entitySet)
	{
		List<IConnectorRecord> l_result = new ArrayList<>();

		l_entitySet.getEntities().forEach(p_entity -> l_result.add(_getValueMapForEntity(p_fields, p_entity)));

		return l_result;
	}

	private IConnectorRecord _getValueMapForEntity(List<IConnectorField> p_fields, ClientEntity p_entity)
	{
		List<IConnectorField> l_result = new ArrayList<>();
		String l_id;
		try
		{
			l_id = p_entity.getProperty("id").getPrimitiveValue().toCastValue(String.class);

			for (IConnectorField l_field : p_fields)
			{
				if ("id".equalsIgnoreCase(l_field.getName()))
				{
					IValueHolder<String> l_vh = ValueHolderFactory.getValueHolder(p_entity.getProperty("id")
						.getPrimitiveValue().toCastValue(String.class));
					l_result.add(new Field(l_field.getGuid(), l_vh));
				}
				else
				{
					final SimpleDateFormat l_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ssssss");

					if ("startDateTime".equalsIgnoreCase(l_field.getName()))
					{
						IValueHolder<Date> l_vh = ValueHolderFactory.getValueHolder(l_format.parse(
								p_entity.getProperty("start").getComplexValue().get(
								"dateTime").getPrimitiveValue().toCastValue(String.class)));
						l_result.add(new Field(l_field.getGuid(), l_vh));
					}
					else if ("endDateTime".equalsIgnoreCase(l_field.getName()))
					{
						IValueHolder<Date> l_vh = ValueHolderFactory.getValueHolder(l_format.parse(
								p_entity.getProperty("end").getComplexValue().get(
								"dateTime").getPrimitiveValue().toCastValue(String.class)));

						l_result.add(new Field(l_field.getGuid(), l_vh));
					}
					else if ("body".equalsIgnoreCase(l_field.getName()))
					{
						IValueHolder<String> l_vh = ValueHolderFactory.getValueHolder(p_entity.getProperty("body")
							.getComplexValue().get("content").getPrimitiveValue().toCastValue(String.class));
						l_result.add(new Field(l_field.getGuid(), l_vh));
					}
					else if ("subject".equalsIgnoreCase(l_field.getName()))
					{
						IValueHolder<String> l_vh = ValueHolderFactory.getValueHolder(p_entity.getProperty("subject")
							.getPrimitiveValue().toCastValue(String.class));
						l_result.add(new Field(l_field.getGuid(), l_vh));
					}
				}
			}
		}
		catch (ParseException | EdmPrimitiveTypeException l_e)
		{
			ms_log.error("Fail to get ValueMap.", l_e);
			throw new RuntimeException(l_e);
		}

		return new Record(l_id, l_result);
	}

	private String _executeInsertUpdateRequest(String p_id, String p_subject, Date p_end, Date p_start,
		String p_body) throws EdmPrimitiveTypeException
	{
		final SimpleDateFormat l_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		final String l_result;

		IOffice365OneDriveService l_service = Office365OneDriveService.newInstance();

		ODataClient l_client = l_service.createODataClient(_getConnectorGuid(), null);

		ClientObjectFactory l_factory = l_client.getObjectFactory();

		ClientEntity l_entity = l_factory.newEntity(new FullQualifiedName("microsoft.graph", "event"));
		if (p_subject != null && !p_subject.isEmpty())
		{
			ClientProperty l_subjectProp = l_factory.newPrimitiveProperty("subject", l_factory
				.newPrimitiveValueBuilder().buildString(p_subject));
			l_entity.getProperties().add(l_subjectProp);
		}

		if (p_start != null && p_start.getTime() > 0)
		{
			ClientComplexValue l_startDate = l_factory.newComplexValue("microsoft.graph.dateTimeTimeZone");

			l_startDate.add(l_factory.newPrimitiveProperty("dateTime", l_factory.newPrimitiveValueBuilder().buildString(
				l_sdf.format(p_start))));

			l_startDate.add(l_factory.newPrimitiveProperty("timeZone", l_factory.newPrimitiveValueBuilder().buildString(
				"UTC")));

			l_entity.getProperties().add(l_factory.newComplexProperty("start", l_startDate));
		}

		if (p_start != null && p_start.getTime() > 0)
		{
			ClientComplexValue l_endDate = l_factory.newComplexValue("microsoft.graph.dateTimeTimeZone");
			l_endDate.add(l_factory.newPrimitiveProperty("dateTime", l_factory.newPrimitiveValueBuilder().buildString(
				l_sdf.format(p_end))));

			l_endDate.add(l_factory.newPrimitiveProperty("timeZone", l_factory.newPrimitiveValueBuilder().buildString(
				"UTC")));
			l_entity.getProperties().add(l_factory.newComplexProperty("end", l_endDate));
		}

		if (p_body != null && !p_body.isEmpty())
		{
			ClientComplexValue l_bodyCV = l_factory.newComplexValue("microsoft.graph.itemBody");
			l_bodyCV.add(l_factory.newPrimitiveProperty("contentType", l_factory.newPrimitiveValueBuilder().buildString(
				"text")));

			l_bodyCV.add(l_factory.newPrimitiveProperty("content", l_factory.newPrimitiveValueBuilder().buildString(
				p_body)));

			l_entity.getProperties().add(l_factory.newComplexProperty("body", l_bodyCV));
		}

		if (p_id != null && !p_id.isEmpty())
		{
			URI l_uri = URI.create(ms_rootUri + "me/events/" + p_id);

			ODataEntityUpdateRequest<ClientEntity> l_req;
			l_req = l_client.getCUDRequestFactory().getEntityUpdateRequest(l_uri, UpdateType.PATCH, l_entity);

			l_req.setFormat(ContentType.JSON_NO_METADATA);

			ODataEntityUpdateResponse<ClientEntity> l_response = l_req.execute();

			l_result = l_response.getBody().getProperty("id").getPrimitiveValue().toCastValue(String.class);
		}
		else
		{
			URI l_uri = URI.create(ms_rootUri + "me/events");

			ODataEntityCreateRequest<ClientEntity> l_req;
			l_req = l_client.getCUDRequestFactory().getEntityCreateRequest(l_uri, l_entity);

			l_req.setFormat(ContentType.JSON_NO_METADATA);

			ODataEntityCreateResponse<ClientEntity> l_response = l_req.execute();

			l_result = l_response.getBody().getProperty("id").getPrimitiveValue().toCastValue(String.class);
		}

		return l_result;
	}
}
