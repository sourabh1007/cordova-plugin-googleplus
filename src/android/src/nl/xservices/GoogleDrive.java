package nl.xservices.plugins;

import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static nl.xservices.plugins.Constants.APPLICATION_NAME;
import static nl.xservices.plugins.Constants.HTTP_TRANSPORT;
import static nl.xservices.plugins.Constants.JSON_FACTORY;
import static nl.xservices.plugins.Constants.MIME_TYPE_FOLDER;
import static nl.xservices.plugins.Constants.MIME_TYPE_GOOGLE_SHEET;
import static nl.xservices.plugins.Constants.MIME_TYPE_TEXT_PLAIN;

public class GoogleDrive {

    public static final String TAG = "GoogleDrive";

    private static Drive createDriveService(GoogleCredential credential) {
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static Sheets createSheetsService(GoogleCredential credential) {
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static boolean isFolderExists(GoogleCredential credential, String folderName)
            throws IOException {
        Log.i(TAG,"Checking if folder exists : folderName : " + folderName);

        FileList folderList = createDriveService(credential).files().list().setQ(
                "mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + folderName+"'").execute();

        Log.i(TAG, "folderList.size() " + folderList.getFiles().size());
        return !folderList.getFiles().isEmpty();
    }

    private static void moveFile(GoogleCredential credential, String fileId, String newParentFolder)
            throws IOException {
        File file = createDriveService(credential).files().get(fileId)
                .setFields("parents")
                .execute();
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
        createDriveService(credential).files().update(fileId,
                null).setAddParents(newParentFolder).setRemoveParents(previousParents.toString())
                .setFields("id, parents").execute();
    }

    /**
     * Create Functions
     * @param credential
     */
    public static String createFile  (GoogleCredential credential, JSONObject configJSON) {
        Log.i(TAG,"Triggering Create File...." + new Gson().toJson(configJSON));

        try {
            String filename = configJSON.getString("name");
            Log.i(TAG, "filename : " + filename);
            String fileType = MIME_TYPE_TEXT_PLAIN;
            if(configJSON.has("type")) {
                fileType = configJSON.getString("type");
                if(MIME_TYPE_GOOGLE_SHEET.equals(fileType)) {
                    return createSheet(credential, configJSON);
                }
            }
            Log.i(TAG, "fileType : " + fileType);
            String parentFolderId = configJSON.getString("parentFolderId");
            Log.i(TAG, "parentFolderId : " + parentFolderId);

            File fileMetadata = new File();
            fileMetadata.setName(filename);
            if(null != parentFolderId && !parentFolderId.isEmpty())
                fileMetadata.setParents(Collections.singletonList(parentFolderId));

            FileContent mediaContent = new FileContent(fileType,
                    java.io.File.createTempFile(filename + "-tmp", "config"));
            File file = createDriveService(credential).files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();

            Log.i(TAG,"File ID: " + file.getId());
            return file.getId();

        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
            return null;
        }
    }

    public static String createFolder(GoogleCredential credential, JSONObject folderConfig) {
        Log.i(TAG,"Triggering Create Folder....." + new Gson().toJson(folderConfig));
        String folderId;
        try {
            String folderName = folderConfig.getString("name");
            if(null == folderName || folderName.isEmpty()) {
                folderName = APPLICATION_NAME;
            }
            String parentFolderId = null;
            if(folderConfig.has("parentFolderId")) {
                parentFolderId = folderConfig.getString("parentFolderId");
                Log.i(TAG, "parentFolderId : " + parentFolderId);
            }

            if(!isFolderExists(credential, folderName)) {
                Log.i(TAG,"folderName....." + folderName + " NOT found so creating it");
                File folderMetadata = new File();
                folderMetadata.setMimeType(MIME_TYPE_FOLDER);
                folderMetadata.setName(folderName);
                if(null != parentFolderId && !parentFolderId.isEmpty())
                    folderMetadata.setParents(Collections.singletonList(parentFolderId));

                File folder = createDriveService(credential).files().create(folderMetadata).execute();
                folderId = folder.getId();
                if(folderConfig.has("files")) {
                    JSONArray fileConfigArray = folderConfig.getJSONArray("files");
                    for(int fileCount = 0; fileCount < fileConfigArray.length(); fileCount++) {
                        JSONObject fileConfig = fileConfigArray.getJSONObject(fileCount);
                        fileConfig.put("parentFolderId", folderId);
                        createFile(credential, fileConfig);
                    }
                    Log.i(TAG,"Created folder successfully with files");

                } else {
                    Log.i(TAG,"Created folder successfully.");
                }
                return folderId;
            }
            else {
                Log.i(TAG,"folderName....." + folderName + " found so skip creating it");
                return null;
            }
        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
            return null;
        }
    }

    public static String createSheet (GoogleCredential credential, JSONObject configJSON) {
        Log.i(TAG,"Triggering Create File.....");
        Spreadsheet spreadsheet;
        try {
            String filename = configJSON.getString("name");
            String parentFolderId = configJSON.getString("parentFolderId");

            List<String> tabList = new ArrayList<>();
            if(configJSON.has("tabs")) {
                JSONArray tabListinJson = configJSON.getJSONArray("tabs");
                int tabLength = tabListinJson.length();
                for(int tabCount = 0; tabCount < tabLength; tabCount++) {
                    tabList.add(tabListinJson.getString(tabCount));
                }
            }

            spreadsheet = new Spreadsheet()
                    .setProperties(new SpreadsheetProperties().setTitle(filename));

            spreadsheet = createSheetsService(credential).spreadsheets().create(spreadsheet)
                    .setFields("spreadsheetId")
                    .execute();

            if(!tabList.isEmpty()) {
                addTabs(credential, spreadsheet, tabList);
            }

            moveFile(credential, spreadsheet.getSpreadsheetId(), parentFolderId);

        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
            return null;
        }

        Log.i(TAG,"Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
        return spreadsheet.getSpreadsheetId();
    }

    /**
     * List Functions
     * @param credential
     * @return
     */
    public static List<File> listFiles(GoogleCredential credential) {

        List<File> files = new ArrayList<>();
        try {
            FileList result = createDriveService(credential).files().list()
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

    /**
     * Delete File/Folder
     * @param credential
     */
    public static void deleteFileOrFolder(GoogleCredential credential) {

        try {
            createDriveService(credential).files().delete("").execute();

        } catch(Exception ex) {
            Log.e(TAG,ex.getMessage());
        }

        Log.i(TAG,"Deleted folder Successfully");
    }

    public static void addTabs(GoogleCredential credential, Spreadsheet spreadsheet, List<String> tabs)
            throws IOException {
        for(String tabName : tabs) {
            BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();

            AddSheetRequest addSheetReq = new AddSheetRequest();
            addSheetReq.setProperties(new SheetProperties().setTitle(tabName));

            Request request = new Request();
            request.setAddSheet(addSheetReq);

            List<Request> reqList  = new ArrayList<>();
            reqList.add(request);

            batchUpdateSpreadsheetRequest.setRequests(reqList);

            createSheetsService(credential).spreadsheets()
                    .batchUpdate(spreadsheet.getSpreadsheetId(), batchUpdateSpreadsheetRequest).execute();
        }
    }

}