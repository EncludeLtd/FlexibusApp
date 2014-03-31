/**
 * Copyright (C) 2008 Dai Odahara.
 */
package ie.enclude.salesforce.database;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import ie.enclude.salesforce.util.StaticInformation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
/**
 * This class is responsible for managing SQLite. At present, it does simply insert/update/delte.
 * 
 * TODO handle salesforce object relations
 * 
 * @author Dai Odahara
 * 
 */
public class SObjectDataFactory extends Activity {
	private static final String TAG = "SOjbectSQLite";
	private static final String DB_NAME = "SObject";
	private static final int DB_VERSION = 1;
	
	private static SQLiteDatabase db = null;
	public SObjectDataFactory(){};
	
	/** create table */
	public void create (Context context, String table, String sobject) {
		try {

			//db = context.openOrCreateDatabase(DB_NAME, DB_VERSION, null);
			showErrorAsDialog(table);
			db.execSQL("drop table if exists " + sobject);
			db.execSQL(table);
		} catch (Exception ex) {
			//db = null;
			Log.v(TAG, ex.toString());
		}
	}
	
	/** create table */
	public void createKeyprefixSObject (Context context) {
		try {
			String tableName = "KeyPrefix_SObject";
			String table = "create table " + tableName + " (" 
				+ "keyPrefix text(" + StaticInformation.SOBJECT_PREFIX_SIZE + ") not null primary key, "
				+ "SObject text not null"
				+ ");";
			
			//db = context.openOrCreateDatabase(DB_NAME, DB_VERSION, null);
			db.execSQL("drop table if exists " + tableName);
			db.execSQL(table);
		} catch (Exception ex) {
			//db = null;
			Log.v(TAG, ex.toString());
		}
	}
	
	/** create table */
	public long insert(ContentValues insertData, String table) {
		Log.v(TAG, "Inserted:" + insertData.toString());
		return db.insert(table, null, insertData);
	}
	
	/** update table */
	public int update(ContentValues updateData, String table, String rowId){
		return db.update(table, updateData, "rowid = " + rowId, null);
	}
	
	/** read colmun - test */
	/**
	public List<Schedule> selectAll(String table) {
		List<Schedule> result = new ArrayList<Schedule>();
		Cursor cursor = db.query(table, null, null, null, null, null, "Name");
		while(cursor.moveToNext()) {
			Schedule s = new Schedule();
			s.Id = cursor.getString(0);
			s.Name = cursor.getString(1);
			s.Importance = cursor.getInt(2);
			result.add(s);
		}
		return result;
	}
	*/
	
	/** open db */
	public void open(Context context) {
		if(null == db || !db.isOpen())db = context.openOrCreateDatabase(DB_NAME, DB_VERSION, null);
	}
	
	/** close the db */
	public void close() {
		if(null != db)db.close();
	}
	
	/** show detail error message */
	private void showErrorAsDialog(String msg) {
		new AlertDialog.Builder(SObjectDataFactory.this)
        .setMessage(msg)
        .show();
	}
	
	/** write data into local file */
	public void write(Context context, String fname, String data){
		/*
		FileOutputStream fos = null;
		byte[] outdata = Base64.decode(data);
		
		try {
			fos = context.openFileOutput(fname, MODE_WORLD_WRITEABLE);
			fos.write(outdata);
			fos.close();
		} catch (Exception ex) {
			Log.v(TAG, ex.toString());
		} finally {
			try {
				fos.close();				
			} catch(IOException ex) {
				ex.printStackTrace();
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		*/
	}
	

	/**
	 * unzip file of zipfile and copy the fils in other directory
	 * @param fileName
	 */
	public void unZip(String fileName) {
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		ZipInputStream zis = null;
		ZipEntry zent = null;
		BufferedOutputStream out = null;
		
		try {
			fis = new FileInputStream(fileName);		
			bis = new BufferedInputStream(fis);
			zis = new ZipInputStream(bis);
			String sep = System.getProperty("file.separator");
			
            int data = 0;    
            
			// read file info in zip file
			while ((zent = zis.getNextEntry()) != null) {
				Log.v(TAG, zent.toString());
				
				if(zent.isDirectory())continue;
                
                String tfn = zent.getName().replaceAll(sep, "_");
                
                new File("data/data/com.android/files/" + tfn);                
                out = new BufferedOutputStream(new FileOutputStream("data/data/com.android/files/" + tfn));
				while( (data = zis.read()) != -1 )
                {
                      out.write(data);
                }
    			out.close();
    			zis.closeEntry();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				zis.close();
				bis.close();
				fis.close();
			} catch (IOException ex) {
				Log.v(TAG, ex.toString());
			} catch(Exception ex) {
				Log.v(TAG, ex.toString());
			}
		}
	}

	/**
	 * parse xml value of report file 
	 * @param xml
	 */
	public void parseReportXML(String xml) {
 
	}

	/**
	 * parse xml value of report file 
	 * @param xml
	 */
	public void parseDashboardXML(String xml) {
  

	}

	// save id and token when finishing login with success
	public void saveIdAndToken(String id, String token, Context context) {
		FileOutputStream fos = null;
		BufferedWriter bw = null;
		
		try {
			fos = context.openFileOutput("idandtoken.txt", 0);
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(id);
			bw.write(System.getProperty("line.separator"));
			bw.write(token);
			//bw.write(System.getProperty("line.separator"));
			bw.flush();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				bw.close();
				fos.close();
			} catch (IOException ex) {
				Log.v(TAG, ex.toString());
			} catch(Exception ex) {
				Log.v(TAG, ex.toString());
			}
		}
	}
	
	// save id and token when finishing login with success
	public String readIdAndToken(Context context) {
		FileInputStream fis = null;
		BufferedReader br = null;
		StringBuffer ret = new StringBuffer();
		try {
			fis = context.openFileInput("idandtoken.txt");
			br = new BufferedReader(new InputStreamReader(fis));
			String t = br.readLine();
			t = t == null ? "" : t;
			ret.append(t).append(":");
			t = br.readLine();
			t = t == null ? "" : t;			
			ret.append(t);
			
		} catch (FileNotFoundException ex) {
			return "";
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				br.close();
				fis.close();
			} catch (IOException ex) {
				Log.v(TAG, ex.toString());
			} catch(Exception ex) {
				Log.v(TAG, ex.toString());
			}
		}
		return ret.toString();
	}
	
}