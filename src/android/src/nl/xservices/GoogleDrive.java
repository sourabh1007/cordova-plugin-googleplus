package nl.xservices.plugins;

import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Google Drive APIs
     */
    private static Drive createDriveService(GoogleCredential credential) {
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
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

    public static String createFile(GoogleCredential credential, JSONObject configJSON) {
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

    public static void deleteFileOrFolder(GoogleCredential credential, JSONObject configJSON) {
        try {
            String fileid = configJSON.getString("fileid");
            createDriveService(credential).files().delete(fileid).execute();

        } catch(Exception ex) {
            Log.e(TAG,ex.getMessage());
        }

        Log.i(TAG,"Deleted folder Successfully");
    }

    public static void uploadFile(GoogleCredential credential, JSONObject configJSON) {
        try {
            java.io.File filePath = (java.io.File) configJSON.get("file");
            String type = configJSON.getString("type");
            System.out.println("type: " + type);

            System.out.println("filePath.getName() ------- " + filePath.getName());
            System.out.println("filePath.toString() ------- " + filePath.toString());

            String fileName = filePath.getName();
            if (configJSON.has("name")) {
                fileName = configJSON.getString("name");
                System.out.println("fileName: " + fileName);
            }

            String mimeType = configJSON.getString("mimeType");
            System.out.println("mimeType: " + mimeType);
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setMimeType(mimeType);

            FileContent mediaContent = new FileContent(type, filePath);
            File file = createDriveService(credential).files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("File ID: " + file.getId());
        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void downloadFile(GoogleCredential credential, JSONObject configJSON)
            throws IOException, JSONException {
        String fileId = configJSON.getString("fileId");
        OutputStream outputStream = new ByteArrayOutputStream();
        createDriveService(credential).files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
    }
    /**
     * Google Sheet APIs
     */
    private static Sheets createSheetsService(GoogleCredential credential) {
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static String createSheet(GoogleCredential credential, JSONObject configJSON) {
        Log.i(TAG,"Triggering Create File.....");
        Log.i(TAG,configJSON.toString());
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

    public static void addTabs(GoogleCredential credential, JSONObject configJSON)
            throws IOException, JSONException {
        List<String> tabList = new ArrayList<>();
        String spreadsheetId = configJSON.getString("spreadsheetId");
        JSONArray tabListinJson = configJSON.getJSONArray("tabs");
        int tabLength = tabListinJson.length();
        for(int tabCount = 0; tabCount < tabLength; tabCount++) {
            tabList.add(tabListinJson.getString(tabCount));
        }

        Spreadsheet spreadsheet = createSheetsService(credential).spreadsheets().get(spreadsheetId).execute();
        addTabs(credential, spreadsheet, tabList);

    }

    private static void addTabs(GoogleCredential credential, Spreadsheet spreadsheet, List<String> tabs)
            throws IOException {
        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
        List<Request> reqList  = new ArrayList<>();
        for(String tabName : tabs) {
            AddSheetRequest addSheetReq = new AddSheetRequest();
            addSheetReq.setProperties(new SheetProperties().setTitle(tabName));

            Request request = new Request();
            request.setAddSheet(addSheetReq);

            reqList.add(request);
        }
        batchUpdateSpreadsheetRequest.setRequests(reqList);

        createSheetsService(credential).spreadsheets()
                .batchUpdate(spreadsheet.getSpreadsheetId(), batchUpdateSpreadsheetRequest).execute();
    }

    public static int updateSheet(GoogleCredential credential, JSONObject configJSON) throws JSONException, IOException {
        Log.i(TAG,"updateSheet.....");
        Log.i(TAG,configJSON.toString());
        if(!configJSON.has("data") || !configJSON.has("sheetId")) {
            Log.e(TAG, "sheetId, data are mandatory");
        }
        List<List<Object>> values = new ArrayList<>();
        try {
            JSONArray rows = configJSON.getJSONArray("data"); // Get all JSONArray data
            int rowCount = rows.length();
            for (int count = 0; count < rowCount; count++) {
                JSONArray jsonArr = rows.getJSONArray(count);
                List<Object> colValue = new ArrayList<>();
                int columnCount = jsonArr.length();
                for (int colCount = 0; colCount < columnCount; colCount++) {
                    colValue.add(jsonArr.getString(colCount));
                }
                values.add(colValue);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while reading JSON");
            e.printStackTrace();
        }
        Log.i(TAG,"data is ");
        Log.i(TAG, Arrays.toString(values.toArray()));

        String spreadsheetId = configJSON.getString("sheetId");
        List<ValueRange> data = new ArrayList<>();
        data.add(new ValueRange()
                .setRange("A1")
                .setValues(values));

        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(data);
        BatchUpdateValuesResponse result =
                createSheetsService(credential).spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
        int rows = result.getTotalUpdatedCells();
        Log.i(TAG, rows + "cells updated.");

        return rows;
    }
}