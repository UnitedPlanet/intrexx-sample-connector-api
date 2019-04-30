
package de.uplanet.lucy.connectorapi.examples.simple;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import de.uplanet.lucy.server.IProcessingContext;
import de.uplanet.lucy.server.businesslogic.util.SortByInfo;
import de.uplanet.lucy.server.dataobjects.IValueHolder;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory;
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderHelper;
import de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter;
import de.uplanet.lucy.server.odata.connector.api.v1.ConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.Field;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorField;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorPagination;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryCriteria;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorQueryResult;
import de.uplanet.lucy.server.odata.connector.api.v1.IConnectorRecord;
import de.uplanet.lucy.server.odata.connector.api.v1.Record;
import de.uplanet.lucy.server.property.IPropertyCollection;
import de.uplanet.lucy.server.rtcache.DataGroupInfo;
import de.uplanet.lucy.server.rtcache.FieldInfo;
import de.uplanet.lucy.server.rtcache.RtCache;


/**
 * A simple connector datagroup implementation for manipulating records stored in an in-memory data store.
 */
public final class InMemoryDataGroupConnector extends AbstractConnectorDataGroupAdapter
{
	private final static AtomicLong m_counter = new AtomicLong(0);
	private final static ConcurrentMap<Long, Map<String, IValueHolder<?>>> m_dataStore = new ConcurrentHashMap<>();

	static
	{
		for (int i = 0; i < 100; i++)
			_addData();
	}

	private static void _addData()
	{
		final Map<String, IValueHolder<?>> l_record = new HashMap<>();

		l_record.put("ID", ValueHolderFactory.getValueHolder(Long.valueOf(m_counter.incrementAndGet())));
		l_record.put("Title", ValueHolderFactory.getValueHolder("Title " + m_counter.get()));
		l_record.put("Integer", ValueHolderFactory.getValueHolder(Integer.valueOf(m_counter.intValue())));
		l_record.put("Long", ValueHolderFactory.getValueHolder(m_counter.get()));
		l_record.put("Double", ValueHolderFactory.getValueHolder(Double.valueOf(Long.toString(m_counter.get()) + "."
		+ Long.toString(m_counter.get()))));
		l_record.put("Text", ValueHolderFactory.getValueHolder("Text Text Text Text Text Text Text"));
		l_record.put("Boolean", ValueHolderFactory.getValueHolder(Boolean.TRUE));
		l_record.put("DateTime", ValueHolderFactory.getValueHolder(new Date()));

		m_dataStore.put(m_counter.get(), l_record);
	}

	public InMemoryDataGroupConnector(IProcessingContext  p_ctx,
	                                  String              p_dataGroupGuid,
	                                  IPropertyCollection p_properties,
	                                  String              p_impersonationGuid)
	{
		super(p_ctx, p_dataGroupGuid, p_properties, p_impersonationGuid);
	}


	@Override
	public IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_queryCriteria)
	{
		List<IConnectorRecord> l_records =
				m_dataStore.keySet().stream().map(p_key ->
					new Record(p_key.toString(),
							   p_queryCriteria.getFields().stream()
							   				.map(p_field -> new Field(p_field.getGuid(),
						   											  m_dataStore.get(p_key).get(p_field.getName())))
							   				.collect(Collectors.toList()))
		).collect(Collectors.toList());

		if (p_queryCriteria.getFilterExpression() != null)
		{
			InMemoryFilterExpressionVisitor l_filterVisitor = new InMemoryFilterExpressionVisitor();
			l_filterVisitor.visitNode(p_queryCriteria.getFilterExpression());

			l_records = _filterRecords(l_records, l_filterVisitor);
		}

		_sortResults(p_queryCriteria, l_records);

		int l_totalSize = l_records.size();

		l_records = _paginate(p_queryCriteria, l_records);

		return new ConnectorQueryResult(l_records, l_records.size(), l_totalSize);
	}


	@Override
	public IConnectorRecord queryDataRecord(String p_recId, List<IConnectorField> p_fields)
	{
		if (!m_dataStore.containsKey(Long.valueOf(p_recId)))
			throw new IllegalStateException("Record not found.");

		final Map<String,IValueHolder<?>> l_record = m_dataStore.get(Long.valueOf(p_recId));

		final List<IConnectorField> l_fields =
				p_fields.stream().map(p_field -> new Field(p_field.getGuid(), l_record.get(p_field.getName())))
										  .collect(Collectors.toList());

		return new Record(p_recId, l_fields);
	}


	@Override
	public String insert(IConnectorRecord p_record)
	{
		if (p_record.getId() != null && m_dataStore.containsKey(Long.valueOf(p_record.getId())))
			throw new IllegalStateException("PK already exists.");

		String l_id = p_record.getId();

		if (l_id == null || l_id.equals("") || l_id.equals("-1"))
			l_id = String.valueOf(m_counter.incrementAndGet());

		final Map<String, IValueHolder<?>> l_record = new HashMap<>();
		l_record.put("Title", _getRecordValue(p_record, "Title"));
		l_record.put("Long", _getRecordValue(p_record, "Long"));
		l_record.put("Double", _getRecordValue(p_record, "Double"));
		l_record.put("Text", _getRecordValue(p_record, "Text"));
		l_record.put("Boolean", _getRecordValue(p_record, "Boolean"));
		l_record.put("DateTime", _getRecordValue(p_record, "DateTime"));

		l_record.put("ID", ValueHolderFactory.getValueHolder(Long.valueOf(l_id)));

		m_dataStore.putIfAbsent(Long.valueOf(l_id), l_record);

		return l_id;
	}


	@Override
	public boolean update(IConnectorRecord p_record)
	{
		if (p_record.getId() == null || !m_dataStore.containsKey(Long.valueOf(p_record.getId())))
			throw new IllegalStateException("Record not found.");

		final Map<String, IValueHolder<?>> l_record = new HashMap<>();

		if (_recordContainsField(p_record, "Title"))
			l_record.put("Title", _getRecordValue(p_record, "Title"));
		if (_recordContainsField(p_record, "Long"))
			l_record.put("Long", _getRecordValue(p_record, "Long"));
		if (_recordContainsField(p_record, "Double"))
			l_record.put("Double", _getRecordValue(p_record, "Double"));
		if (_recordContainsField(p_record, "Text"))
			l_record.put("Text", _getRecordValue(p_record, "Text"));
		if (_recordContainsField(p_record, "Boolean"))
			l_record.put("Boolean", _getRecordValue(p_record, "Boolean"));
		if (_recordContainsField(p_record, "DateTime"))
			l_record.put("DateTime", _getRecordValue(p_record, "DateTime"));

		 String l_id = p_record.getId();

		if (l_id == null)
			l_id = String.valueOf(m_counter.incrementAndGet());

		l_record.put("ID", ValueHolderFactory.getValueHolder(Long.valueOf(l_id)));

		m_dataStore.put(Long.valueOf(l_id), l_record);

		return true;
	}


	@Override
	public void delete(String p_recId)
	{
		m_dataStore.remove(Long.valueOf(p_recId));
	}


	private String _getFieldGuid(String p_fieldName)
	{
		final DataGroupInfo l_dgi = RtCache.getDataGroup(getDataGroupGuid());
		final FieldInfo l_fi = RtCache.getFields(p_fi ->	p_fi.getDataGroupGuid().equals(l_dgi.getGuid()) &&
													p_fi.getColumnName().equals(p_fieldName))
				.stream().findFirst().orElseThrow(RuntimeException::new);

		return l_fi.getGuid();
	}


	private boolean _recordContainsField(IConnectorRecord p_record, String p_fieldName)
	{
		final FieldInfo l_fi = _getFieldInfo(p_fieldName);

		return p_record.containsFieldGuid(l_fi.getGuid());
	}

	private FieldInfo _getFieldInfo(String p_fieldName)
	{
		final DataGroupInfo l_dgi = RtCache.getDataGroup(getDataGroupGuid());
		final FieldInfo l_fi = RtCache.getFields(p_fi -> p_fi.getDataGroupGuid().equals(l_dgi.getGuid()) &&
														 p_fi.getName().equals(p_fieldName))
										.stream().findFirst().orElseThrow(RuntimeException::new);
		return l_fi;
	}


	private IValueHolder<?> _getRecordValue(IConnectorRecord p_record, String p_fieldName)
	{
		final FieldInfo l_fi = _getFieldInfo(p_fieldName);

		if (p_record.containsFieldGuid(l_fi.getGuid()))
			return p_record.getFieldByGuid(l_fi.getGuid()).getValue();
		else
			return ValueHolderFactory.getNullValueHolder();
	}


	private List<IConnectorRecord> _filterRecords(List<IConnectorRecord> p_records,
												  InMemoryFilterExpressionVisitor p_filterVisitor)
	{
		return p_records.stream().filter(p_rec ->
		{
			if (p_filterVisitor.getTitle() != null)
			{
				String l_value = ValueHolderHelper.getStringFromVH(p_rec.getFieldByName("Title").getValue());
				return l_value.contains(p_filterVisitor.getTitle());
			}
			else if (p_filterVisitor.getBoolean() != null)
			{
				Boolean l_value = ValueHolderHelper.getBooleanFromVH(p_rec.getFieldByName("Boolean").getValue());
				return l_value.equals(p_filterVisitor.getBoolean());
			}
			else
			{
				return true;
			}
		}).collect(Collectors.toList());
	}


	private void _sortResults(IConnectorQueryCriteria p_queryCriteria, List<IConnectorRecord> l_records)
	{
		if (!p_queryCriteria.getSortbyFields().isEmpty())
		{
			final List<SortByInfo> l_sortBy = p_queryCriteria.getSortbyFields();

			final Optional<SortByInfo> l_byId = l_sortBy.stream()
					.filter(p_si -> p_si.getFieldGuid().equals(_getFieldGuid("ID"))).findFirst();

			if (l_byId.isPresent())
			{
				l_records.sort((p_e1, p_e2) -> {
					try
					{
						Long l_value1 = (Long) p_e1.getFieldByGuid(l_byId.get().getFieldGuid()).getValue().getValue();
						Long l_value2 = (Long) p_e2.getFieldByGuid(l_byId.get().getFieldGuid()).getValue().getValue();
						if (l_byId.get().isDescending())
						{
							if (l_value2 != null)
								return l_value2.compareTo(l_value1);
							else return 0;
						}
						else
						{
							if (l_value1 != null)
								return l_value1.compareTo(l_value2);
							else return 0;
						}
					}
					catch (Exception l_e)
					{
						return 0;
					}
				});
			}

			final String l_titleGuid = _getFieldGuid("Title");

			final Optional<SortByInfo> l_byTitle = l_sortBy.stream()
															.filter(p_si -> p_si.getFieldGuid().equals(l_titleGuid)).findFirst();

			if (l_byTitle.isPresent())
			{
				l_records.sort((p_e1, p_e2) -> {
					try
					{
						String l_value1 = (String) p_e1.getFieldByGuid(l_byTitle.get().getFieldGuid()).getValue().getValue();
						String l_value2 = (String) p_e2.getFieldByGuid(l_byTitle.get().getFieldGuid()).getValue().getValue();
						if (l_byTitle.get().isDescending())
						{
							if (l_value2 != null)
								return l_value2.compareTo(l_value1);
							else
								return 0;
						}
						else
						{
							if (l_value1 != null)
								return l_value1.compareTo(l_value2);
							else
								return 0;
						}
					}
					catch (Exception l_e)
					{
						return 0;
					}
				});
			}
		}
	}


	private List<IConnectorRecord> _paginate(IConnectorQueryCriteria p_queryCriteria, List<IConnectorRecord> l_records)
	{
		final IConnectorPagination l_pagination  = p_queryCriteria.getPagination();

		int l_skip = (l_pagination.getPageNumber() - 1) * l_pagination.getPageSize();
		int l_limit = l_pagination.getPageSize();

		return l_records.stream()
						.skip(l_skip)
						.limit(l_limit)
						.collect(Collectors.toList());
	}
}
