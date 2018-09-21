package de.uplanet.lucy.connectorapi.examples.google.drive;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class GoogleDriveJSONParser
{
	final static SimpleDateFormat ms_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");

	public List<GoogleDriveItem> parseJSONStringToGoogleDriveItems(String p_json) throws Exception
	{
		List<GoogleDriveItem> l_resultList = new ArrayList<GoogleDriveItem>();
		JSONObject l_jsonRootObj = (JSONObject) new JSONParser().parse(p_json);

		Object l_obj = l_jsonRootObj.get("files");

		if (l_obj instanceof JSONObject)
		{
			l_resultList.add(parseJsonToGoogleDriveItem((JSONObject) l_obj));
		}
		else if (l_obj instanceof JSONArray)
		{
			JSONArray l_array = (JSONArray) l_obj;

			for (Object l_jsonFileObj : l_array)
			{
				if (l_jsonFileObj instanceof JSONObject)
					l_resultList.add(parseJsonToGoogleDriveItem((JSONObject) l_jsonFileObj));
			}
		}

		return l_resultList;
	}

	public GoogleDriveItem parseJsonToGoogleDriveItem(JSONObject p_object)
	{
		String l_id = (String) p_object.get("id");
		String l_name = (String) p_object.get("name");
		String l_kind = (String) p_object.get("kind");
		String l_mimeType = (String) p_object.get("mimeType");
		String l_thumbnailLink = (String) p_object.get("thumbnailLink");
		String l_createdTime = (String) p_object.get("createdTime");
		String l_modifiedTime = (String) p_object.get("modifiedTime");
		String l_description = (String) p_object.get("description");
		String l_size = (String) p_object.get("size");
		String l_strWebViewLink = (String) p_object.get("webViewLink");
		String l_strDownloadLink = (String) p_object.get("webContentLink");
		
		URI l_thumbnailURI = null;
		URI l_webViewLinkURI = null;
		URI l_downloadLink = null;
		
		Long l_lSize = null;
		if(l_size != null)
			l_lSize = Long.parseLong(l_size);

		if(l_thumbnailLink != null)
			l_thumbnailURI = URI.create(l_thumbnailLink);
		
		if(l_strWebViewLink != null)
			l_webViewLinkURI = URI.create(l_strWebViewLink);
		
		if(l_strDownloadLink != null)
			l_downloadLink = URI.create(l_strDownloadLink);

		return new GoogleDriveItem(l_id,
		                           l_name,
		                           l_kind,
		                           l_mimeType,
		                           l_thumbnailURI,
		                           _parseDate(l_createdTime),
		                           _parseDate(l_modifiedTime),
		                           l_lSize,
		                           l_description,
		                           l_webViewLinkURI,
		                           l_downloadLink);
	}

	private Date _parseDate(String p_date)
	{
		if (p_date == null)
			return null;

		try
		{
			return ms_sdf.parse(p_date);
		}
		catch (ParseException l_e)
		{
			return null;
		}
	}
}
