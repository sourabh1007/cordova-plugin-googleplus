package nl.xservices.plugins;

import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static nl.xservices.plugins.Constants.*;

public class GoogleDrive {

    public static final String TAG = "GoogleDrive";

    public static List<File> listFiles(GoogleCredential credential) {

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        List<File> files = new ArrayList<>();
        try {
            FileList result = service.files().list()
                    .setPageSize(100)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            files =  result.getFiles();

            Log.i(TAG, "Number of files found  : " + files.size());

        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
        }
        return files;

    }

    public static void createSheet(GoogleCredential credential) {
        Log.i(TAG,"Triggering Create File.....");

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                        .setTitle("my title"));
        try {
            spreadsheet = service.spreadsheets().create(spreadsheet)
                    .setFields("spreadsheetId")
                    .execute();
        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
        }

        Log.i(TAG,"Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
    }

    public static void createFile(GoogleCredential credential, JSONObject configJSON) {
        Log.i(TAG,"Triggering Create File...." + new Gson().toJson(configJSON));

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        try {
            String filename = configJSON.getString("name");
            Log.i(TAG, "filename : " + filename);

            File file = new File()
                    .setMimeType("application/vnd.google-apps.file")
                    .setName(filename);

            file = service.files().create(file).execute();
            Log.i(TAG,"File ID: " + file.getId());

        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
        }
    }

    public static void createFolder(GoogleCredential credential, JSONObject folderConfig) {
        Log.i(TAG,"Triggering Create Folder....." + new Gson().toJson(folderConfig));

        try {
            String folderName = folderConfig.getString("name");
            if(null == folderName || folderName.isEmpty()) {
                folderName = APPLICATION_NAME;
            }
            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            File folderMetadata = new File();
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            folderMetadata.setName(folderName);
            service.files().create(folderMetadata).execute();

            try {
                JSONObject fileConfig = folderConfig.getJSONObject("file");
                createFile(credential, fileConfig);
                Log.i(TAG,"Created folder successfully with file");

            } catch (JSONException ex) {
                Log.i(TAG,"Created folder successfully without file");

            }
        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
        }
    }

    public static void deleteFileOrFolder(GoogleCredential credential) {

        try {
            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            service.files().delete("").execute();
        } catch(Exception ex) {
            Log.e(TAG,ex.getMessage());

        }

        Log.i(TAG,"Deleted folder Successfully");
    }

}