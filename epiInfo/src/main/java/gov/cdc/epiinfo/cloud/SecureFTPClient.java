package gov.cdc.epiinfo.cloud;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.InMemoryDestFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SecureFTPClient implements ICloudClient {

	private String tableName;
	private String url;
	private String userName;
	private String password;

	public class StreamingOutputFile extends InMemoryDestFile
	{
		private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		@Override
		public OutputStream getOutputStream() throws IOException {
			// TODO Auto-generated method stub
			return outputStream;
		}

	}

	public class StreamingInputFile extends InMemorySourceFile
	{
		private ByteArrayInputStream inputStream;
		private long length;

		public StreamingInputFile(byte[] input)
		{
			this.inputStream = new ByteArrayInputStream(input);
			length = input.length;
		}

		@Override
		public InputStream getInputStream() throws IOException {

			return inputStream;
		}

		@Override
		public long getLength() {

			return length;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "test.txt";
		}

	}

	public SecureFTPClient(String tableName, Context context)
	{
		this.tableName = tableName;

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		url = sharedPref.getString("sftp_url", "");
		url = url.toLowerCase().replace("sftp://", "").replace("ftp://", "");
		userName = sharedPref.getString("cloud_user_name", "");
		password = sharedPref.getString("cloud_pwd", "");
	}

	@Override
	public JSONArray getData(boolean downloadImages, boolean downloadMedia) {

		try
		{
			StringBuilder builder = new StringBuilder();
			SSHClient ssh = TryToConnect(null);
			ssh.authPassword(userName, password);
			try {
				final SFTPClient sftp = ssh.newSFTPClient();

				if (downloadImages)
				{
					try
					{
						String photoPath = "/__EpiInfoPhotos/" + tableName;
						List<RemoteResourceInfo> files = sftp.ls(photoPath);
						for (int x=0; x<files.size(); x++)
						{
							String fileName = "/sdcard/Download/EpiInfo/Images/" + files.get(x).getName();
							new File(fileName).createNewFile();
							sftp.get(photoPath + "/" + files.get(x).getName(), fileName);
						}
					}
					catch (Exception ex)
					{
						int x = 5;
						x++;
					}
				}
				
				if (downloadMedia)
				{
					try
					{
						String mediaPath = "/__EpiInfoMedia/" + tableName;
						List<RemoteResourceInfo> files = sftp.ls(mediaPath);
						for (int x=0; x<files.size(); x++)
						{
							String fileName = "/sdcard/Download/EpiInfo/Media/" + files.get(x).getName();
							new File(fileName).createNewFile();
							sftp.get(mediaPath + "/" + files.get(x).getName(), fileName);
						}
					}
					catch (Exception ex)
					{
						int x = 5;
						x++;
					}
				}


				String jsonPath = "/__EpiInfo/" + tableName;
				List<RemoteResourceInfo> files = sftp.ls(jsonPath);
				try {
					for (int x = 0; x < files.size(); x++)
					{
						if (x==0)
						{
							builder.append("[");
						}

						StreamingOutputFile output = new StreamingOutputFile();
						String fileName = files.get(x).getName();
						sftp.get(jsonPath + "/" + fileName, output);
						String data = new String(((ByteArrayOutputStream)output.getOutputStream()).toByteArray());
						builder.append(data);

						if (x==files.size()-1)
						{
							builder.append("]");
						}
						else
						{
							builder.append(",");
						}


					}
				} finally {
					sftp.close();
				}
			} finally {
				ssh.disconnect();
			}
			return new JSONArray(builder.toString());
		}
		catch (Exception ex)
		{
			return null;
		}

	}

	@Override
	public boolean insertRecord(ContentValues values) {

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
			return false;
		}

		try
		{
			SSHClient ssh = TryToConnect(null);
			ssh.authPassword(userName, password);
			try {
				final SFTPClient sftp = ssh.newSFTPClient();
				String path = "__EpiInfo/" + tableName + "/";
				sftp.mkdirs(path);
				try {
					sftp.put(new StreamingInputFile(jsonObject.toString().getBytes()), path + jsonObject.getString("id") + ".txt");
				} finally {
					sftp.close();
				}
			} finally {
				ssh.disconnect();
			}
		}
		catch (Exception ex)
		{
			return false;
		}
		
		if (images.size() > 0)
		{
			for (int x=0; x<images.size(); x++)
			{
				try
				{
					SSHClient ssh = TryToConnect(null);
					ssh.authPassword(userName, password);
					try {
						final SFTPClient sftp = ssh.newSFTPClient();
						String photoPath = "__EpiInfoPhotos/" + tableName + "/";
						sftp.mkdirs(photoPath);
						try {
							FileSystemFile photoFile = new FileSystemFile(images.get(x));
							sftp.put(photoFile, photoPath + photoFile.getName());
						} finally {
							sftp.close();
						}
					} finally {
						ssh.disconnect();
					}
				}
				catch (Exception ex)
				{
					return false;
				}
			}
		}
		
		if (media.size() > 0)
		{
			for (int x=0; x<media.size(); x++)
			{
				try
				{
					SSHClient ssh = TryToConnect(null);
					ssh.authPassword(userName, password);
					try {
						final SFTPClient sftp = ssh.newSFTPClient();
						String mediaPath = "__EpiInfoMedia/" + tableName + "/";
						sftp.mkdirs(mediaPath);
						try {
							FileSystemFile mediaFile = new FileSystemFile(media.get(x));
							sftp.put(mediaFile, mediaPath + mediaFile.getName());
						} finally {
							sftp.close();
						}
					} finally {
						ssh.disconnect();
					}
				}
				catch (Exception ex)
				{
					return false;
				}
			}
		}
		
		return true;
	}

	private SSHClient TryToConnect(String keyVerifier)
	{
		SSHClient ssh = new SSHClient();
		try
		{
			if (keyVerifier != null)
			{
				ssh.addHostKeyVerifier(keyVerifier);
			}
			try {
				ssh.connect(url);
			} catch (TransportException e) {
				if (e.getDisconnectReason() == DisconnectReason.HOST_KEY_NOT_VERIFIABLE) {
					String msg = e.getMessage();
					String[] split = msg.split("`");
					String vc = split[3];
					ssh.disconnect();
					return TryToConnect(vc);
				} else {
					return null;
				}
			}
		}
		catch (Exception ex)
		{
			return null;
		}
		return ssh;
	}

	@Override
	public boolean deleteRecord(String recordId) {
		try
		{
			SSHClient ssh = TryToConnect(null);
			ssh.authPassword(userName, password);
			try {
				final SFTPClient sftp = ssh.newSFTPClient();
				String path = "__EpiInfo/" + tableName + "/";
				sftp.mkdirs(path);
				try {
					sftp.rm(path + recordId + ".txt");
				} finally {
					sftp.close();
				}
			} finally {
				ssh.disconnect();
			}
		}
		catch (Exception ex)
		{
			return false;
		}

		return true;
	}

	@Override
	public boolean updateRecord(String recordId, ContentValues values) {

		return insertRecord(values);
	}

}
