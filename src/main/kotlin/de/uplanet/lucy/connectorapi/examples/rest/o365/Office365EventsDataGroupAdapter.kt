package de.uplanet.lucy.connectorapi.examples.rest.o365

import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import de.uplanet.lucy.server.ContextUser
import de.uplanet.lucy.server.IProcessingContext
import de.uplanet.lucy.server.auxiliaries.datetime.DateTimeUtil
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderFactory
import de.uplanet.lucy.server.dataobjects.impl.ValueHolderHelper
import de.uplanet.lucy.server.odata.connector.api.v1.*
import de.uplanet.lucy.server.property.IPropertyCollection
import de.uplanet.util.ISODateTimeUtil
import de.uplanet.util.URIEncoder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import java.time.LocalDateTime
import java.time.ZoneOffset
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
 * The Office365 events data group adapter.
 */
class Office365EventsDataGroupAdapter(p_ctx:               IProcessingContext,
                                      p_dataGroupGuid:     String,
                                      p_properties:        IPropertyCollection,
                                      p_impersonationGuid: String?)
    : AbstractConnectorDataGroupAdapter(p_ctx,
                                        p_dataGroupGuid,
                                        p_properties,
                                        p_impersonationGuid) {

    override fun queryDataRange(p_queryCriteria: IConnectorQueryCriteria): IConnectorQueryResult {
        val httpClient = createHttpClient(connectorGuid, null)

        // prepare date range filter
        val now = LocalDateTime.now()
        val periodStart =
                ISODateTimeUtil.formatISODateTimeMillis(Date.from(now.minusDays(1)
                .toInstant(ZoneOffset.UTC)))
        val periodEnd =
                ISODateTimeUtil.formatISODateTimeMillis(Date.from(now.plusDays(30)
                .toInstant(ZoneOffset.UTC)))
        val filter = "start/dateTime lt '$periodEnd' and end/dateTime gt '$periodStart'"

        // prepare request URI
        val request = RequestBuilder.get("https://graph.microsoft.com/v1.0/me/events?" +
                "\$select=id,subject,body,bodyPreview,organizer,attendees,start,end," +
                "location,webLink&" +
                "\$top=20&" +
                "\$filter=" + URIEncoder.encodeURIComponent(filter) + "&" +
                "\$orderby=${URIEncoder.encodeURIComponent("start/dateTime ASC")}")
                .addHeader("accept", "application/json").build()

        try {
            // execute request
            val response = httpClient.execute(request)

            // get response payload
            val body = EntityUtils.toString(response.entity)

            if (response.statusLine.statusCode == 200) { // OK?
                // create event objects from response JSON
                val result = parseEvents(body, p_queryCriteria.fields)
                // create query result
                return ConnectorQueryResult(result, result.size, 20)
            } else { // NOT OK
                throw RuntimeException("Request failed with status " + response.statusLine.toString())
            }
        } finally {
            HttpClientUtils.closeQuietly(httpClient) //clean up resources
        }
    }

    override fun queryDataRecord(p_id: String, p_fields: List<IConnectorField>): IConnectorRecord {
        val httpClient = createHttpClient(connectorGuid, null)

        val request = RequestBuilder.get("https://graph.microsoft.com/v1.0/me/events('$p_id')?" +
                "\$select=id,subject,body,bodyPreview,organizer,attendees,start,end,webLink")
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

    override fun insert(p_record: IConnectorRecord): String {
        val httpClient = createHttpClient(connectorGuid, null)
        try {
            val payload = toJsonPayload(p_record)

            val request = RequestBuilder.post("https://graph.microsoft.com/v1.0/me/events")
                    .addHeader("content-type", "application/json")
                    .setEntity(StringEntity(payload))
                    .build()

            val response = httpClient.execute(request)
            val body = EntityUtils.toString(response.entity)

            if (response.statusLine.statusCode == 201) {
                val result = Klaxon().parseJsonObject(body.reader())
                if (result.containsKey("id"))
                    return result["id"] as String
                else
                    throw java.lang.RuntimeException("Cannot get ID from response.")
            } else {
                throw RuntimeException("Request failed with status " + response.statusLine.toString())
            }
        } finally {
            HttpClientUtils.closeQuietly(httpClient)
        }
    }

    private fun toJsonPayload(p_record: IConnectorRecord): String {
        val fieldNameMap = p_record.fields.associateBy { field -> field.name }
        val subject = ValueHolderHelper.getStringFromVH(fieldNameMap["subject"]?.value)
        val description = ValueHolderHelper.getStringFromVH(fieldNameMap["body"]?.value)
        val start = ISODateTimeUtil.formatISODateTimeMillis(
                ValueHolderHelper.getDateFromVH(fieldNameMap["start"]?.value))
        val end = ISODateTimeUtil.formatISODateTimeMillis(
                ValueHolderHelper.getDateFromVH(fieldNameMap["end"]?.value))

        return """
            {
              "subject": "$subject",
              "body": {
                "content": "$description",
                "contentType": "text"
              },
              "start": {
                "dateTime": "$start",
                "timeZone": "Etc/GMT"
              },
              "end": {
                "dateTime": "$end",
                "timeZone": "Etc/GMT"
              }
            }
        """.trimIndent()
    }

    override fun update(p_record: IConnectorRecord): Boolean {
        val httpClient = createHttpClient(connectorGuid, null)
        try {
            val payload = toJsonPayload(p_record)

            val request = RequestBuilder.patch("https://graph.microsoft.com/v1.0/me/events/${p_record.id}")
                    .addHeader("content-type", "application/json")
                    .setEntity(StringEntity(payload))
                    .build()

            val response = httpClient.execute(request)

            if (response.statusLine.statusCode == 200) {
                return true
            } else {
                throw RuntimeException("Request failed with status " + response.statusLine.toString())
            }
        } finally {
            HttpClientUtils.closeQuietly(httpClient)
        }
    }

    override fun delete(p_id: String) {
        val httpClient = createHttpClient(connectorGuid, null)
        try {
            val request = RequestBuilder.delete("https://graph.microsoft.com/v1.0/me/events/$p_id")
                    .build()

            val response = httpClient.execute(request)

            if (response.statusLine.statusCode != 204)
                throw RuntimeException("Request failed with status " + response.statusLine.toString())
        } finally {
            HttpClientUtils.closeQuietly(httpClient)
        }
    }

    private fun parseEvents(body: String, toFields: List<IConnectorField>): List<IConnectorRecord> {
        //parse body
        val payload = Klaxon().parseJsonObject(body.reader())

        // parse events
        val events = if (payload.containsKey("value")) {
            Klaxon().parseFromJsonArray<Event>(payload["value"] as JsonArray<*>)
        } else { // parse single events
            val event = Klaxon().parseFromJsonObject<Event>(payload)
            if (event != null)
                listOf(event)
            else
                emptyList()
        }

        return events!!.map { event ->
            val fields = toFields.map { field ->
                when (field.name) {
                    "id" -> Field(field.guid, ValueHolderFactory.getValueHolder(event.id))
                    "subject" -> Field(field.guid, ValueHolderFactory.getValueHolder(event.subject))
                    "body" -> Field(field.guid, ValueHolderFactory.getValueHolder(event.bodyPreview))
                    "start" -> Field(field.guid,ValueHolderFactory.getValueHolder(parseDate(event.start.dateTime,
                                                                                            event.start.timeZone)))
                    "end" -> Field(field.guid, ValueHolderFactory.getValueHolder(parseDate(event.end.dateTime,
                                                                                            event.end.timeZone)))
                    "webLink" -> Field(field.guid, ValueHolderFactory.getValueHolder(event.webLink))
                    else -> throw IllegalArgumentException("Unknown field name " + field.name)
                }
            }

            Record(event.id, fields.toList())
         }
    }

    private fun parseDate(p_date: String, p_tz: String): Date {
        val date = DateTimeUtil().parseDate("yyyy-MM-dd'T'HH:mm:ss.ssssss", p_date, TimeZone.getTimeZone(p_tz))
        return date.withTimeZone(ContextUser.get().timeZone)
    }
}