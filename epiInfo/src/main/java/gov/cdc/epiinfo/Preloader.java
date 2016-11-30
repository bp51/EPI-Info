package gov.cdc.epiinfo;

import gov.cdc.epiinfo.etc.ExtFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Preloader {

	private Activity context;
	
	public void Load(Activity ctx)
	{
		context = ctx;
		new AsyncLoader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class AsyncLoader extends AsyncTask<Void,Double, Boolean>
	{

		@Override
		protected Boolean doInBackground(Void... x) {

			LoadData();
			return true;
		}
	}
	
	private void LoadData()
	{
		File basePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		File quesPath = new File(basePath + "/EpiInfo/Preload");

		String[] files = quesPath.list(new ExtFilter("csv",null));
		if (files != null)
		{			
			for (int x=0;x<files.length;x++)
			{
				String csvFile = files[x];
				BufferedReader br = null;
				String line = "";
				String delimeter = ",";
				
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
				String decimalSeparator = sharedPref.getString("decimal_symbol", ".");
				if (decimalSeparator.equals(","))
				{
					delimeter = ";";
				}
				
				EpiDbHelper mDbHelper = null;
				
				try {
					
					int idx = files[x].indexOf(".");
					String viewName = files[x].substring(0, idx);
					FormMetadata formMetadata = new FormMetadata("EpiInfo/Questionnaires/"+ viewName +".xml", context);
					
					if (viewName.startsWith("_"))
					{
						viewName = viewName.toLowerCase();
					}
					
					mDbHelper = new EpiDbHelper(context, formMetadata, viewName);
					mDbHelper.open();

					br = new BufferedReader(new FileReader(quesPath + "/" + csvFile));
					boolean firstRow = true;
					String[] header = null;
					while ((line = br.readLine()) != null) {
						if (firstRow)
						{
							header = line.split(delimeter);
							firstRow = false;
						}
						else
						{
							String[] dataRow = line.split(delimeter,-1);
							
							ContentValues initialValues = new ContentValues();
							String GlobalRecordId = "";
							for (int i=0;i<header.length;i++)
							{
								String strHeader = header[i].replace("\"", "");
								if (!strHeader.equals("RECSTATUS") && !strHeader.equals("UniqueKey") && !strHeader.equals("FirstSaveLogonName") && !strHeader.equals("FirstSaveTime") && !strHeader.equals("LastSaveLogonName") && !strHeader.equals("LastSaveTime") && !strHeader.equals("FKEY"))
								{
									if (strHeader.equals("GlobalRecordId"))
									{
										GlobalRecordId = dataRow[i];
									}
									else
									{
										if (!dataRow[i].equals(""))
										{
											if (formMetadata.GetFieldType(strHeader) == 10)
											{
												if (dataRow[i].toLowerCase().equals("true"))
												{
													initialValues.put(strHeader, 1);
												}
											}
											else if (formMetadata.GetFieldType(strHeader) == 11)
											{
												if (dataRow[i].equals("0"))
												{
													initialValues.put(strHeader, 2);
												}
												else
												{
													initialValues.put(strHeader, dataRow[i]);
												}
											}
											else if (formMetadata.GetFieldType(strHeader) == 17)
											{
												int listIndex = formMetadata.GetFieldByName(strHeader).getListValues().indexOf(dataRow[i]);
												if (listIndex > -1)
												{
													initialValues.put(strHeader, listIndex);
												}
											}
											else
											{
												initialValues.put(strHeader, dataRow[i]);
											}
										}
									}
								}
							}
							if (!GlobalRecordId.equals(""))
							{
								if (mDbHelper.fetchWhere(EpiDbHelper.GUID, EpiDbHelper.GUID + " = '" + GlobalRecordId + "'").getCount() == 0)
								{
									long w = mDbHelper.createRecord(initialValues, false, GlobalRecordId, null);
									w++;
								}
							}
						}

					}

				} 
				catch (Exception ex) 
				{
					int w = 5;
					w++;
				} 
				finally {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (mDbHelper != null)
					{
						mDbHelper.close();
					}
				}
			  
			}
		}
	}
}
