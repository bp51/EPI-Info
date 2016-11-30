package gov.cdc.epiinfo.cloud;

import gov.cdc.epiinfo.RecordList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.auth.BoxAuthentication.BoxAuthenticationInfo;
import com.box.androidsdk.content.models.BoxEntity;
import com.box.androidsdk.content.models.BoxError;
import com.box.androidsdk.content.models.BoxError.ErrorContext;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;

public class BoxClient implements BoxAuthentication.AuthListener, ICloudClient {

	private String tableName;
	private Context context;
	private BoxSession session;
	private String tableFolderId;
	private String photoFolderId;
	private String mediaFolderId;

	public static boolean isAuthenticated(Context context)
	{
		try
		{
			BoxConfig.IS_LOG_ENABLED = true;
			BoxConfig.CLIENT_ID = "";
			BoxConfig.CLIENT_SECRET = "";
			BoxSession session = new BoxSession(context);
			BoxAuthenticationInfo info = session.getAuthInfo();
			String token = info.accessToken();
			if (token == null || token.equals(""))
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public static void SignOut(Context context)
	{
		try
		{
			BoxConfig.IS_LOG_ENABLED = true;
			BoxConfig.CLIENT_ID = "x87efslud4bo81cl9oeocjwzqmcr9kc4";
			BoxConfig.CLIENT_SECRET = "tGA384Zkct7BdCbsdLEd4sGhtUL7wZMa";
			BoxSession session = new BoxSession(context);
			session.logout();
		}
		catch (Exception ex)
		{

		}
	}

	public BoxClient(String tableName, Context context)
	{
		try
		{
			this.context = context;
			this.tableName = tableName;

			BoxConfig.IS_LOG_ENABLED = true;
			BoxConfig.CLIENT_ID = "x87efslud4bo81cl9oeocjwzqmcr9kc4";
			BoxConfig.CLIENT_SECRET = "tGA384Zkct7BdCbsdLEd4sGhtUL7wZMa";

			if (tableName.startsWith("_"))
			{
				this.tableName = tableName.replaceFirst("_", "");
			}

			initialize();
		}
		catch (Exception ex)
		{

		}
	}

	private void initialize() {
		try
		{
			session = new BoxSession(context);
			session.setSessionAuthListener(this);
			session.authenticate();
		}
		catch (Exception ex)
		{

		}
	}

	private void getTableFolderStructure()
	{
		String rootFolder = createFolder("0", "__EpiInfo");
		tableFolderId = createFolder(rootFolder, tableName);
	}

	private void getPhotoFolderStructure()
	{
		String rootFolder = createFolder("0", "__EpiInfoPhotos");
		photoFolderId = createFolder(rootFolder, tableName);
	}
	
	private void getMediaFolderStructure()
	{
		String rootFolder = createFolder("0", "__EpiInfoMedia");
		mediaFolderId = createFolder(rootFolder, tableName);
	}


	private String createFolder(String parent, String name)
	{
		try
		{
			BoxApiFolder folderApi = new BoxApiFolder(session);
			return folderApi.getCreateRequest(parent, name).send().getId();
		}
		catch (BoxException ex)
		{
			try
			{
				BoxError test = ex.getAsBoxError();
				ErrorContext test2 = test.getContextInfo();
				ArrayList<BoxEntity> test3 = test2.getConflicts();
				if (test3.size() > 0)
				{
					return test3.get(0).getId();
				}

			}
			catch (Exception e)
			{

			}
		}
		return "";
	}

	public JSONArray getData(boolean downloadImages, boolean downloadMedia) 
	{
		try
		{
			BoxApiFile fileApi = new BoxApiFile(session);
			BoxApiFolder folderApi = new BoxApiFolder(session);

			if (downloadImages)
			{
				getPhotoFolderStructure();
				BoxListItems photoFolderItems = folderApi.getItemsRequest(photoFolderId).send();
				for (int x=0; x<photoFolderItems.size(); x++)
				{
					File f = new File("/sdcard/Download/EpiInfo/Images/" + photoFolderItems.get(x).getName());
					f.createNewFile();
					fileApi.getDownloadRequest(f, photoFolderItems.get(x).getId()).send();
				}
			}
			
			if (downloadMedia)
			{
				getMediaFolderStructure();
				BoxListItems mediaFolderItems = folderApi.getItemsRequest(mediaFolderId).send();
				for (int x=0; x<mediaFolderItems.size(); x++)
				{
					File f = new File("/sdcard/Download/EpiInfo/Media/" + mediaFolderItems.get(x).getName());
					f.createNewFile();
					fileApi.getDownloadRequest(f, mediaFolderItems.get(x).getId()).send();
				}
			}

			getTableFolderStructure();
			StringBuilder builder = new StringBuilder();
			BoxListItems folderItems = folderApi.getItemsRequest(tableFolderId).send();
			for (int x=0; x<folderItems.size(); x++)
			{
				if (x==0)
				{
					builder.append("[");
				}

				PipedOutputStream po = new PipedOutputStream();
				PipedInputStream pi = new PipedInputStream(po);

				fileApi.getDownloadRequest(po, folderItems.get(x).getId()).send();
				int i;
				po.close();
				GZIPInputStream s = new GZIPInputStream(pi);
				while ((i = s.read()) != -1)
				{
					builder.append((char)i);
				}
				pi.close();
				s.close();

				if (x==folderItems.size()-1)
				{
					builder.append("]");
				}
				else
				{
					builder.append(",");
				}
			}
			return new JSONArray(builder.toString());
		} 
		catch (Exception e) 
		{
			return null;
		} 
	}

	public boolean insertRecord(ContentValues values) 
	{

		getTableFolderStructure();
		JSONObject jsonObject = new JSONObject();
		LinkedList<String> images = new LinkedList<String>();
		LinkedList<String> media = new LinkedList<String>();
		try {

			for (String key : values.keySet())
			{
				Object value = values.get(key);
				if (value != null)
				{
					if (value instanceof Integer)
					{
						jsonObject.put(key, (Integer)value);
					}
					else if (value instanceof Double)
					{
						if (((Double)value) < Double.POSITIVE_INFINITY)
						{
							jsonObject.put(key, (Double)value);
						}
					}
					else if (value instanceof Long)
					{
						jsonObject.put(key, (Long)value);
					}
					else if (value instanceof Boolean)
					{
						jsonObject.put(key, (Boolean)value);
					}
					else
					{
						jsonObject.put(key, value.toString());
					}
					if (value.toString().contains("/EpiInfo/Images/"))
					{
						images.add(value.toString());
					}
					if (value.toString().contains("/EpiInfo/Media/"))
					{
						media.add(value.toString());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		boolean retval = false;

		try 
		{
			new BoxApiFile(session).getUploadRequest(new ByteArrayInputStream(jsonObject.toString().getBytes()), jsonObject.getString("id") + ".txt", tableFolderId).send();
			retval = true;
		}
		catch (BoxException bx)
		{
			try
			{
				BoxError test = bx.getAsBoxError();
				ErrorContext test2 = test.getContextInfo();
				ArrayList<BoxEntity> test3 = test2.getConflicts();
				if (test3.size() > 0)
				{
					new BoxApiFile(session).getUploadNewVersionRequest(new ByteArrayInputStream(jsonObject.toString().getBytes()), test3.get(0).getId()).send();
					retval = true;
				}
			}
			catch (Exception e)
			{
				int x=5;
				x++;
			}
		}
		catch (Exception ex)
		{
			int x=5;
			x++;
		}

		if (images.size() > 0)
		{
			getPhotoFolderStructure();
			for (int x=0; x<images.size(); x++)
			{
				retval = false;

				try
				{
					new BoxApiFile(session).getUploadRequest(new File(images.get(x)), photoFolderId).send();
					retval = true;
				}
				catch (BoxException ex)
				{
					try
					{
						BoxError test = ex.getAsBoxError();
						ErrorContext test2 = test.getContextInfo();
						ArrayList<BoxEntity> test3 = test2.getConflicts();
						if (test3.size() > 0)
						{
							new BoxApiFile(session).getUploadNewVersionRequest(new File(images.get(x)), test3.get(0).getId()).send();
							retval = true;
						}
					}
					catch (Exception e)
					{
						int z=5;
						z++;
					}
				}
				catch (Exception ex)
				{
					int z=5;
					z++;
				}
			}
		}
		
		if (media.size() > 0)
		{
			getMediaFolderStructure();
			for (int x=0; x<media.size(); x++)
			{
				retval = false;

				try
				{
					new BoxApiFile(session).getUploadRequest(new File(media.get(x)), mediaFolderId).send();
					retval = true;
				}
				catch (BoxException ex)
				{
					try
					{
						BoxError test = ex.getAsBoxError();
						ErrorContext test2 = test.getContextInfo();
						ArrayList<BoxEntity> test3 = test2.getConflicts();
						if (test3.size() > 0)
						{
							new BoxApiFile(session).getUploadNewVersionRequest(new File(media.get(x)), test3.get(0).getId()).send();
							retval = true;
						}
					}
					catch (Exception e)
					{
						int z=5;
						z++;
					}
				}
				catch (Exception ex)
				{
					int z=5;
					z++;
				}
			}
		}

		return retval;
	}

	public boolean deleteRecord(String recordId) {

		try 
		{
			BoxFile file = new BoxApiFile(session).getUploadRequest(new ByteArrayInputStream("_".getBytes()), recordId + ".txt", tableFolderId).send();
			if (file != null)
			{
				new BoxApiFile(session).getDeleteRequest(file.getId()).send();
				return true;
			}
		}
		catch (BoxException bx)
		{
			try
			{
				BoxError test = bx.getAsBoxError();
				ErrorContext test2 = test.getContextInfo();
				ArrayList<BoxEntity> test3 = test2.getConflicts();
				if (test3.size() > 0)
				{
					new BoxApiFile(session).getDeleteRequest(test3.get(0).getId()).send();
					return true;
				}
			}
			catch (Exception e)
			{

			}
		}
		catch (Exception ex)
		{

		}
		return false;
	}

	public boolean updateRecord(String recordId, ContentValues values) {

		return insertRecord(values);
	}


	@Override
	public void onRefreshed(BoxAuthenticationInfo info) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAuthCreated(BoxAuthenticationInfo info) {
		// TODO Auto-generated method stub
		//createFolders();
		if (context != null && context.getClass() == RecordList.class)
		{
			((RecordList)context).OnBoxLoggedIn();
		}
	}

	@Override
	public void onAuthFailure(BoxAuthenticationInfo info, Exception ex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoggedOut(BoxAuthenticationInfo info, Exception ex) {
		// TODO Auto-generated method stub
		initialize();
	}


}
