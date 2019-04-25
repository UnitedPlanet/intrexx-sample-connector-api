package de.uplanet.lucy.connectorapi.examples.rest.o365

import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import de.uplanet.lucy.server.ContextUser
import de.uplanet.lucy.server.IProcessingContext
import de.uplanet.lucy.server.auxiliaries.datetime.DateTimeUtil
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory
import de.uplanet.lucy.server.odata.connector.api.v1.*
import de.uplanet.lucy.server.property.IPropertyCollection
import de.uplanet.util.URIEncoder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import java.util.*

/**
 * The Office365 event data class.
 */
data class Event(val id: String,
                 val subject: String,
                 val bodyPreview: String,
                 val start: EventDate,
                 val end: EventDate,
                 val webLink: String)

/**
 * The Office365 event date data class.
 */
data class EventDate(val dateTime: String,
                     val timeZone: String)

/**
 * The Office365 event data group adapter.
 */
class Office365EventsDataGroupAdapter(p_ctx: IProcessingContext?,
                                      p_dataGroupGuid: String?,
                                      p_properties: IPropertyCollection?,
                                      p_impersonationGuid: String?)
    : AbstractConnectorDataGroupAdapter(p_ctx, p_dataGroupGuid, p_properties, p_impersonationGuid) {

    override fun queryDataRange(p_queryCriteria: IConnectorQueryCriteria?): IConnectorQueryResult {
        val httpClient = createHttpClient(connectorGuid, null)

        val request = RequestBuilder.get("https://graph.microsoft.com/v1.0/me/events?" +
                "\$select=subject,body,bodyPreview,organizer,attendees,start,end,location,webLink&\$top=20&" +
                "\$orderby=${URIEncoder.encodeURIComponent("start/dateTime DESC")}")
                .addHeader("accept", "application/json").build()

        try {
            val response = httpClient.execute(request)
            val body = EntityUtils.toString(response.entity)

            if (response.statusLine.statusCode == 200) {
                val result = parseEvents(body, p_queryCriteria!!.fields)
                return ConnectorQueryResult(result, result.size, 20)
            } else {
                throw RuntimeException("Request failed with status " + response.statusLine.toString())
            }
        } finally {
            HttpClientUtils.closeQuietly(httpClient)
        }
    }

    override fun queryDataRecord(p_id: String?, p_fields: List<IConnectorField>): IConnectorRecord {
        val httpClient = createHttpClient(connectorGuid, null)

        val request = RequestBuilder.get("https://graph.microsoft.com/v1.0/me/events('$p_id')?" +
                "\$select=subject,body,bodyPreview,organizer,attendees,start,end,webLink")
                .addHeader("accept", "application/json").build()

        try {
            val response = httpClient.execute(request)
            val body = EntityUtils.toString(response.entity)

            if (response.statusLine.statusCode == 200) {
                val result = parseEvents(body, p_fields)
                return result[0]
            } else {
                throw RuntimeException("Request failed with status " + response.statusLine.toString())
            }
        } finally {
            HttpClientUtils.closeQuietly(httpClient)
        }
    }

    override fun recordExists(p_id: String?): Boolean {
        return !p_id.isNullOrEmpty()
    }

    override fun insert(p_record: IConnectorRecord?): String {
        TODO("not implemented")
    }

    override fun update(p_record: IConnectorRecord?): Boolean {
        TODO("not implemented")
    }

    override fun delete(p_id: String?) {
        TODO("not implemented")
    }

    private fun parseEvents(body: String, toFields: List<IConnectorField>): List<IConnectorRecord> {
        val payload = Klaxon().parseJsonObject(body.reader())
        val events = if (payload.containsKey("value")) {
            Klaxon().parseFromJsonArray<Event>(payload["value"] as JsonArray<*>)
        } else {
            val event = Klaxon().parseFromJsonObject<Event>(payload)
            if (event != null)
                listOf(event)
            else
                emptyList()
        }

        val fieldMap = toFields.associateBy { field -> field.name }

        return events!!.map { event ->
            val id = Field(fieldMap["id"]?.guid, ValueHolderFactory.getValueHolder(event.id))
            val subject = Field(fieldMap["subject"]?.guid, ValueHolderFactory.getValueHolder(event.subject))
            val eventBody = Field(fieldMap["body"]?.guid, ValueHolderFactory.getValueHolder(event.bodyPreview))
            val start = Field(fieldMap["start"]?.guid,
                    ValueHolderFactory.getValueHolder(parseDate(event.start.dateTime, event.start.timeZone)))
            val end = Field(fieldMap["end"]?.guid,
                    ValueHolderFactory.getValueHolder(parseDate(event.end.dateTime, event.end.timeZone)))
            val webLink = Field(fieldMap["webLink"]?.guid, ValueHolderFactory.getValueHolder(event.webLink))

            val fields = listOf(id, subject, eventBody, start, end, webLink)

            Record(event.id, fields.toList())
         }
    }

    private fun parseDate(p_date: String, p_tz: String): Date {
        val date = DateTimeUtil().parseDate("yyyy-MM-dd'T'HH:mm:ss.ssssss", p_date, TimeZone.getTimeZone(p_tz))
        return date.withTimeZone(ContextUser.get().timeZone)
    }
}