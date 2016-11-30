package gov.cdc.epiinfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedList;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class SettingsFragment extends PreferenceFragment {

	private Activity activity;

	public void SetActivity(Activity activity)
	{
		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		final CheckBoxPreference ei7 = (CheckBoxPreference) getPreferenceManager().findPreference("ei7");
		final CheckBoxPreference stacked = (CheckBoxPreference) getPreferenceManager().findPreference("stacked");
		final CheckBoxPreference interview = (CheckBoxPreference) getPreferenceManager().findPreference("interview");
		final CheckBoxPreference sync_up_only = (CheckBoxPreference) getPreferenceManager().findPreference("sync_up_only");
		final CheckBoxPreference sync_up_down = (CheckBoxPreference) getPreferenceManager().findPreference("sync_up_down");
		final CheckBoxPreference sync_down_only = (CheckBoxPreference) getPreferenceManager().findPreference("sync_down_only");
		final SwitchPreference sample_forms = (SwitchPreference) getPreferenceManager().findPreference("sample_forms");
		final ListPreference cloud_service = (ListPreference) getPreferenceManager().findPreference("cloud_service");
		final SwitchPreference azure_classic = (SwitchPreference) getPreferenceManager().findPreference("azure_classic");
		final Preference service_name = getPreferenceManager().findPreference("service_name");
		final Preference application_key = getPreferenceManager().findPreference("application_key");
		final Preference sftp_url = getPreferenceManager().findPreference("sftp_url");
		final Preference cloud_user_name = getPreferenceManager().findPreference("cloud_user_name");
		final Preference cloud_pwd = getPreferenceManager().findPreference("cloud_pwd");

		if (DeviceManager.IsPhone())
		{
			ei7.setEnabled(false);
		}

		ei7.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				ei7.setChecked(true);
				stacked.setChecked(false);
				interview.setChecked(false);
				return true;
			}
		});

		stacked.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				stacked.setChecked(true);
				ei7.setChecked(false);
				interview.setChecked(false);
				return true;
			}
		});
		
		interview.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				stacked.setChecked(false);
				ei7.setChecked(false);
				interview.setChecked(true);
				return true;
			}
		});

		sync_up_only.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				sync_up_only.setChecked(true);
				sync_down_only.setChecked(false);
				sync_up_down.setChecked(false);
				return true;
			}
		});

		sync_up_down.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				sync_up_only.setChecked(false);
				sync_down_only.setChecked(false);
				sync_up_down.setChecked(true);
				return true;
			}
		});
		
		sync_down_only.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				sync_up_only.setChecked(false);
				sync_down_only.setChecked(true);
				sync_up_down.setChecked(false);
				return true;
			}
		});

		sample_forms.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object val) {
				if ((Boolean)val)
				{
					GetSampleForm();
				}
				else
				{
					DeleteSampleForm();
				}
				return true;
			}
		});
		
		if (cloud_service.getValue().equals("Box"))
		{
			azure_classic.setEnabled(false);
			service_name.setEnabled(false);
			application_key.setEnabled(false);
			sftp_url.setEnabled(false);
			cloud_user_name.setEnabled(false);
			cloud_pwd.setEnabled(false);
		}
		else if (cloud_service.getValue().equals("SFTP"))
		{
			azure_classic.setEnabled(false);
			service_name.setEnabled(false);
			application_key.setEnabled(false);
			sftp_url.setEnabled(true);
			cloud_user_name.setEnabled(true);
			cloud_pwd.setEnabled(true);
		}
		else
		{
			azure_classic.setEnabled(true);
			service_name.setEnabled(true);
			application_key.setEnabled(true);
			sftp_url.setEnabled(false);
			cloud_user_name.setEnabled(false);
			cloud_pwd.setEnabled(false);
		}
		
		cloud_service.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object val) {
				if (val.toString().equals("Box"))
				{
					azure_classic.setEnabled(false);
					service_name.setEnabled(false);
					application_key.setEnabled(false);
					sftp_url.setEnabled(false);
					cloud_user_name.setEnabled(false);
					cloud_pwd.setEnabled(false);
				}
				else if (val.toString().equals("SFTP"))
				{
					azure_classic.setEnabled(false);
					service_name.setEnabled(false);
					application_key.setEnabled(false);
					sftp_url.setEnabled(true);
					cloud_user_name.setEnabled(true);
					cloud_pwd.setEnabled(true);
				}
				else
				{
					azure_classic.setEnabled(true);
					service_name.setEnabled(true);
					application_key.setEnabled(true);
					sftp_url.setEnabled(false);
					cloud_user_name.setEnabled(false);
					cloud_pwd.setEnabled(false);
				}
				return true;
			}
		});
	}

	private void DeleteSampleForm()
	{
		try
		{
			File file1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Preparedness.xml");
			file1.delete();
			File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Ebola_Lab.xml");
			file2.delete();
			File file3 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Ebola_Site_Monitoring.xml");
			file3.delete();
			File file4 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Ebola_Lab_ws.xml");
			file4.delete();
			File file5 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Contact_Investigation.xml");
			file5.delete();
			File file6 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_InterviewMode.xml");
			file6.delete();
		}
		catch (Exception ex)
		{

		}
	}

	private void GetSampleForm()
	{
		AssetManager am = activity.getAssets();
		try 
		{          
			LinkedList<String> fileNames = new LinkedList<String>();
			fileNames.add("Sample_Contact_Investigation.xml");
			fileNames.add("Sample_InterviewMode.xml");
			fileNames.add("_ContactFollowup.xml");

			for (int x=0;x<fileNames.size();x++)
			{
				String fileName = fileNames.get(x);
				File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/" + fileName);
				InputStream in = am.open(fileName);         
				FileOutputStream f = new FileOutputStream(destinationFile);          
				byte[] buffer = new byte[1024];         
				int len1 = 0;         
				while ((len1 = in.read(buffer)) > 0) 
				{             
					f.write(buffer, 0, len1);         
				}         
				f.close();     
			}
		} 
		catch (Exception e) 
		{         

		}
		
		try 
		{          
			LinkedList<String> fileNames = new LinkedList<String>();
			fileNames.add("Sample_Contact_Investigation.csv");

			for (int x=0;x<fileNames.size();x++)
			{
				String fileName = fileNames.get(x);
				File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Preload/" + fileName);
				InputStream in = am.open(fileName);         
				FileOutputStream f = new FileOutputStream(destinationFile);          
				byte[] buffer = new byte[1024];         
				int len1 = 0;         
				while ((len1 = in.read(buffer)) > 0) 
				{             
					f.write(buffer, 0, len1);         
				}         
				f.close();     
			}
		} 
		catch (Exception e) 
		{         

		}
		
		try
		{
			File oldfile1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Preparedness.xml");
			oldfile1.delete();
			File oldfile2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Ebola_Lab.xml");
			oldfile2.delete();
			File oldfile3 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Ebola_Site_Monitoring.xml");
			oldfile3.delete();
			File oldfile4 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/EpiInfo/Questionnaires/Sample_Ebola_Lab_ws.xml");
			oldfile4.delete();
		}
		catch (Exception ex)
		{

		}
	}
}