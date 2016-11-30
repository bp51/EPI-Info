package gov.cdc.epiinfo.cloud;

import org.json.JSONArray;

import android.content.ContentValues;

public interface ICloudClient {

	public JSONArray getData(boolean downloadImages, boolean downloadMedia);

	public boolean insertRecord(ContentValues values);

	public boolean deleteRecord(String recordId);

	public boolean updateRecord(String recordId, ContentValues values);
	
}
