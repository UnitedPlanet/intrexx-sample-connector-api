package de.uplanet.lucy.connectorapi.examples.mfiles

import de.uplanet.lucy.server.IProcessingContext
import de.uplanet.lucy.server.dataobjects.IValueHolder
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderHelper
import de.uplanet.lucy.server.mfiles.api.*
import de.uplanet.lucy.server.mfiles.connector.*
import de.uplanet.lucy.server.odata.connector.api.v1.*
import de.uplanet.lucy.server.property.IPropertyCollection
import de.uplanet.lucy.server.rtcache.RtCache
import org.slf4j.LoggerFactory
import java.io.IOException


/**
 * Example for creating M-Files "customer" objects.
 */
class MFilesDataGroupAdapter(p_ctx: IProcessingContext,
                             p_dataGroupGuid:     String,
                             p_properties: IPropertyCollection,
                             p_impersonationGuid: String?)
    : AbstractConnectorDataGroupAdapter(p_ctx,
                                        p_dataGroupGuid,
                                        p_properties,
                                        p_impersonationGuid) {

    private val ms_log = LoggerFactory.getLogger(this.javaClass)

    override fun insert(p_record: IConnectorRecord): String {
        val l_session = MFilesSession.get(properties, businessLogicProcessingContext)
        val l_objType = properties.getInteger("connector.mfiles.customer.object.type")
        val l_props   = mutableListOf<PropertyValue>()

        p_record.fields.forEach { p_field ->
            when (p_field.name) {
                "name" -> l_props.add(getPropertyValue("name", p_field.value))
                "no" -> l_props.add(getPropertyValue("no", p_field.value))
                "address" -> l_props.add(getPropertyValue("address", p_field.value))
                "city" -> l_props.add(getPropertyValue("city", p_field.value))
                "zip" -> l_props.add(getPropertyValue("zip", p_field.value))
                "email" -> l_props.add(getPropertyValue("email", p_field.value))
                "classId" -> l_props.add(getPropertyValue("classId", p_field.value))
            }
        }

        val l_object = createObject(l_session, l_objType, l_props)

        return l_object.objVer.id.toString()
    }

    override fun update(p_record: IConnectorRecord): Boolean {
        val l_session = MFilesSession.get(properties, businessLogicProcessingContext)
        val l_objType = properties.getInteger("connector.mfiles.customer.object.type")
        val l_props   = mutableListOf<PropertyValue>()
        var objVer = 0

        p_record.fields.forEach { p_field ->
            when (p_field.name) {
                "name" -> l_props.add(getPropertyValue("name", p_field.value))
                "no" -> l_props.add(getPropertyValue("no", p_field.value))
                "address" -> l_props.add(getPropertyValue("address", p_field.value))
                "city" -> l_props.add(getPropertyValue("city", p_field.value))
                "zip" -> l_props.add(getPropertyValue("zip", p_field.value))
                "email" -> l_props.add(getPropertyValue("email", p_field.value))
                "objver"  -> objVer = ValueHolderHelper.getIntFromVH(p_field.value)
            }
        }

        updateProperties(l_session, l_objType, p_record.id.toInt(), objVer, l_props)

        return true
    }

    override fun queryDataRange(p_queryCriteria: IConnectorQueryCriteria): IConnectorQueryResult {
        val l_session   = MFilesSession.get(properties, businessLogicProcessingContext)
        val l_service   = MFilesService.getInstance()
        val l_mfObjects = l_service.viewSearch(l_session, "V2/L2")

        var l_records   = l_mfObjects.map { p_objVersion ->
            val l_fields = mapPropsToFields(l_session, p_objVersion)
            Record(p_objVersion.objVer.id.toString(), l_fields)
        }.toList()

        //sort by name if selected
        val fieldName = p_queryCriteria.fields.firstOrNull{ p_field -> p_field.name == "name" }
        val sortByInfo = p_queryCriteria.sortbyFields.firstOrNull { p_sort -> p_sort.fieldGuid == fieldName?.guid }
        if (sortByInfo != null)
        {
            l_records = if (sortByInfo.isAscending) {
                l_records.sortedBy { p_rec ->
                    ValueHolderHelper.getStringFromVH(p_rec.getFieldByGuid(fieldName?.guid).value)
                }
            } else {
                l_records.sortedByDescending { p_rec ->
                    ValueHolderHelper.getStringFromVH(p_rec.getFieldByGuid(fieldName?.guid).value)
                }
            }
        }

        return ConnectorQueryResult(l_records, l_records.size, l_records.size)
    }

    override fun queryDataRecord(p_recId: String, p_fields: List<IConnectorField>): IConnectorRecord {
        val l_session = MFilesSession.get(properties, businessLogicProcessingContext)
        val l_object  = getObjectById(l_session, p_recId)
        val l_fields  = mapPropsToFields(l_session, l_object as ObjectVersion)

        return Record(p_recId, l_fields)
    }

    override fun delete(p_recId: String) {
        val l_session = MFilesSession.get(properties, businessLogicProcessingContext)
        val l_config = l_session.configuration
        val l_objType = properties.getInteger("connector.mfiles.customer.object.type")

        try {
            val l_com = MFilesHttpRequest(l_config.endpointUri)

            val l_strPayload = PrimitiveType.toJSON(true)

            val l_strResult = l_com.putAsString(l_session,
                    String.format("objects/%s/%s/deleted", l_objType, p_recId),
                    l_strPayload,
                    null)

            if (l_com.lastResponseCode >= 400) {
                throw MFilesError.logAndThrow(ms_log, "Cannot delete object from M-Files.", l_strResult)
            }
        } catch (l_e: IOException) {
            throw MFilesException(l_e)
        }
    }

    private fun getObjectProperties(p_session: IMFilesSession, p_objType: Int, p_iObjId: Int, p_iObjVersion: Int):
            Array<PropertyValue> {
        val l_config = p_session.configuration
        val l_request = MFilesHttpRequest(l_config.endpointUri)

        try {
            val l_result = l_request.getAsString(p_session,
                    String.format("objects/%s/%s/%s/properties", p_objType, p_iObjId, p_iObjVersion), null)

            if (l_request.lastResponseCode >= 400) {
                throw MFilesError.logAndThrow(ms_log, "Cannot get object properties.", l_result)
            }

            return  MFilesJSONHelper.createArrayFromJSON(PropertyValue::class.java, l_result)
        } catch (l_e: IOException) {
            throw MFilesException("Cannot get object properties.", l_e)
        }
    }

    private fun getObjectById(p_session: IMFilesSession, p_strId: String): ExtendedObjectVersion? {
        val l_objType = properties.getInteger("connector.mfiles.customer.object.type")
        val l_config  = p_session.configuration
        val l_request = MFilesHttpRequest(l_config.endpointUri)

        try {
            val l_strResult = l_request.getAsString(p_session,
                    String.format("objects/%s/%s/latest", l_objType, p_strId),
                    null)

            return when {
                l_request.lastResponseCode == 200 ->
                    MFilesJSONHelper.createFromJSON(ExtendedObjectVersion::class.java, l_strResult)
                l_request.lastResponseCode == 404 -> null
                else -> throw MFilesError.logAndThrow(ms_log, "Cannot load object.", l_strResult)
            }
        } catch (l_e: IOException) {
            throw MFilesException(l_e)
        }

    }

    private fun mapPropsToFields(l_session: IMFilesSession, p_objVersion: ObjectVersion):
            MutableList<IConnectorField> {
        val l_fields = mutableListOf<IConnectorField>()

        val l_objProps = getObjectProperties(l_session,
                                             p_objVersion.objVer.type,
                                             p_objVersion.objVer.id,
                                             p_objVersion.objVer.version)
        l_objProps.map { p_objProp ->
            try {
                when (p_objProp.propertyDef) {
                    //name
                    0 -> l_fields.add(Field(getFieldGuidByPropName("name"),
                            ValueHolderFactory.getValueHolder(p_objProp.typedValue.displayValue)))
                    //no
                    1021 -> l_fields.add(Field(getFieldGuidByPropName("no"),
                            ValueHolderFactory.getValueHolder(p_objProp.typedValue.displayValue)))
                    //address
                    1022 -> l_fields.add(Field(getFieldGuidByPropName("address"),
                            ValueHolderFactory.getValueHolder(p_objProp.typedValue.displayValue)))
                    //city
                    1023 -> l_fields.add(Field(getFieldGuidByPropName("city"),
                            ValueHolderFactory.getValueHolder(p_objProp.typedValue.displayValue)))
                    //zip
                    1020 -> l_fields.add(Field(getFieldGuidByPropName("zip"),
                            ValueHolderFactory.getValueHolder(p_objProp.typedValue.displayValue)))
                    //email
                    1036 -> l_fields.add(Field(getFieldGuidByPropName("email"),
                            ValueHolderFactory.getValueHolder(p_objProp.typedValue.displayValue)))
                    else -> null
                }
            } catch (l_e: Exception) {
                //ignore field
                ms_log.warn("Cannot find field for property", l_e)
            }
        }

        l_fields.add(Field(getFieldGuidByPropName("id"),
                            ValueHolderFactory.getValueHolder(p_objVersion.objVer.id.toLong())))
        l_fields.add(Field(getFieldGuidByPropName("objid"),
                            ValueHolderFactory.getValueHolder(p_objVersion.objVer.id.toLong())))
        l_fields.add(Field(getFieldGuidByPropName("objver"),
                            ValueHolderFactory.getValueHolder(p_objVersion.objVer.version.toLong())))
        l_fields.add(Field(getFieldGuidByPropName("classId"),
                            ValueHolderFactory.getValueHolder(p_objVersion.classId)))
        l_fields.add(Field(getFieldGuidByPropName("objType"),
                            ValueHolderFactory.getValueHolder(p_objVersion.objVer.type)))


        return l_fields
    }

    private fun createObject(p_session: IMFilesSession,
                             p_iObjType: Int,
                             p_properties: List<PropertyValue>)
            : ObjectVersion {
        val l_config = p_session.configuration
        val l_com = MFilesHttpRequest(l_config.endpointUri)

        val objectInfo = ObjectCreationInfo()
        objectInfo.propertyValues = p_properties.toTypedArray()

        try {
            if (p_properties.isEmpty()) {
                throw IllegalArgumentException("Skipped creating M-Files object since no properties were provided.")
            }

            val l_json = objectInfo.toJSON()
            val l_strResult = l_com.postAsString(p_session,
                    String.format("/objects/%s", p_iObjType), null, l_json)

            if (l_com.lastResponseCode >= 400) {
                throw MFilesError.logAndThrow(ms_log, "Cannot create object.", l_strResult)
            }

            return MFilesJSONHelper.createFromJSON(ObjectVersion::class.java, l_strResult)
        } catch (l_e: IOException) {
            throw MFilesException("Cannot create object.", l_e)
        }
    }

    private fun updateProperties(p_session: IMFilesSession,
                                 p_objType: Int,
                                 p_objId: Int,
                                 p_objVer: Int,
                                 p_properties: List<PropertyValue>)
            : Any {
        val l_config = p_session.configuration
        val l_com = MFilesHttpRequest(l_config.endpointUri)

        val objectInfo = ObjectCreationInfo()
        objectInfo.propertyValues = p_properties.toTypedArray()

        try {
            if (p_properties.isEmpty()) {
                throw IllegalArgumentException("Skipped updating M-Files object since no properties were provided.")
            }

            val l_json = StringBuilder()

            l_json.append("[")

            for (i in p_properties.indices) {
                l_json.append(p_properties[i].toJSON())

                if (i + 1 < p_properties.size)
                    l_json.append(",")
            }

            l_json.append("]")

            val l_strResult = l_com.postAsString(p_session,
                                                 String.format("/objects/%s/%s/%s/properties", p_objType, p_objId, p_objVer),
                                                 null,
                                                 l_json.toString())

            if (l_com.lastResponseCode >= 400) {
                throw MFilesError.logAndThrow(ms_log, "Cannot update object.", l_strResult)
            }

            return MFilesJSONHelper.createFromJSON(ObjectVersion::class.java, l_strResult)
        } catch (l_e: IOException) {
            throw MFilesException("Cannot update object.", l_e)
        }
    }

    private fun getFieldGuidByPropName(p_propName: String): String? {
        return RtCache.getFields { p_fi -> dataGroupGuid == p_fi.dataGroupGuid }
                .first { p_field -> p_field.name == p_propName }
                .guid
    }

    private fun getPropertyValue(p_name: String, p_value: IValueHolder<*>): PropertyValue {
        val prop = PropertyValue()
        val typedValue = TypedValue()

        when (p_name) {
           "name" -> {typedValue.value = ValueHolderHelper.getStringFromVH(p_value); typedValue.dataType = 1; prop.propertyDef = 0}
           "no" -> {typedValue.value = ValueHolderHelper.getIntFromVH(p_value); typedValue.dataType = 1; prop.propertyDef = 1021}
           "address" -> {typedValue.value = ValueHolderHelper.getStringFromVH(p_value); typedValue.dataType = 1; prop.propertyDef = 1022}
           "city" -> {typedValue.value = ValueHolderHelper.getStringFromVH(p_value); typedValue.dataType = 1; prop.propertyDef = 1023}
           "zip" -> {typedValue.value = ValueHolderHelper.getStringFromVH(p_value); typedValue.dataType = 1; prop.propertyDef = 1020}
           "email" -> {typedValue.value = ValueHolderHelper.getStringFromVH(p_value); typedValue.dataType = 1; prop.propertyDef = 1036}
           "classId" -> {val lookup = Lookup(); lookup.item = ValueHolderHelper.getIntFromVH(p_value); typedValue.lookup = lookup
                        typedValue.dataType = 9; prop.propertyDef = 100}
        }

        prop.typedValue = typedValue
        return prop
    }
}