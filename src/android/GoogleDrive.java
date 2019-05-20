package nl.xservices.plugins;

import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;

import java.io.IOException;
import java.util.List;

public class GoogleDrive {
    private static final String APPLICATION_NAME = "Investo";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static final String TAG = "GoogleDrive";
    public static List<File> listFiles(GoogleCredential credential) throws IOException {
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(100)
                .setFields("nextPageToken, files(id, name)")
                .execute();

        List<File> files =  result.getFiles();
        Log.i(TAG, "Number of files found  : " + files.size());
        return files;
    }

    public static void createFile(GoogleCredential credential) throws IOException {
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                        .setTitle("my title"));
        spreadsheet = service.spreadsheets().create(spreadsheet)
                .setFields("spreadsheetId")
                .execute();
        System.out.println("Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
    }
}
