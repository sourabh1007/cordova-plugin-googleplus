package nl.xservices.plugins;

import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;

import org.apache.cordova.CordovaArgs;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import static nl.xservices.plugins.Constants.*;

public class GoogleDrive {

    public static final String TAG = "GoogleDrive";

    public static List<File> listFiles(GoogleCredential credential) throws IOException {
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        FileList result = service.files().list()
                .setPageSize(100)
                .setFields("nextPageToken, files(id, name)")
                .execute();

        List<File> files =  result.getFiles();
        Log.i(TAG, "Number of files found  : " + files.size());
        return files;
    }

    public static void createSheet(GoogleCredential credential) throws IOException {
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
       // Log.i(TAG,"Triggering Create File.....");

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                        .setTitle("my title"));
        spreadsheet = service.spreadsheets().create(spreadsheet)
                .setFields("spreadsheetId")
                .execute();
       // Log.i(TAG,"Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
    }

    public static void createFolder(GoogleCredential credential, CordovaArgs args) throws IOException, JSONException {
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        Log.i(TAG,"Triggering Create Folder.....");

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        File fileMetadata = new File();
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setName(args.getString(1));

        File file = service.files().create(fileMetadata)
                .setFields("id")
                .execute();
        System.out.println("Folder ID: " + file.getId());
    }


    public static void deleteFile(GoogleCredential credential) {
    }

    public static void deleteFolder(GoogleCredential credential) {
    }
}
