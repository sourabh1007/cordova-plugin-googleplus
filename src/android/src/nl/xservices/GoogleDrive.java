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

    private static FileList getFolderInformation(GoogleCredential credential, String folderName)
            throws IOException {
        Log.i(TAG,"Checking if folder exists : folderName : " + folderName);

        FileList folderList = createDriveService(credential).files().list().setQ(
                "mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + folderName+"'").execute();

        Log.i(TAG, "folderList.getFiles().size() " + folderList.getFiles().size());
        return folderList;
    }

    private static void moveFile(GoogleCredential credential, String fileId, String newParentFolder)
            throws IOException {
        Log.i(TAG,"Moving file to parent folder : " + newParentFolder);
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
        Log.i(TAG,"Moving file to parent folder Successful");
    }

    public static String createFile(GoogleCredential credential, JSONObject configJSON) {
        Log.i(TAG,"Triggering Create File...." + new Gson().toJson(configJSON));

        String fileType = MIME_TYPE_TEXT_PLAIN;
        String fileId = null;
        try {
            String filename = configJSON.getString("name");
            Log.i(TAG, "filename : " + filename);
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

            FileList files = createDriveService(credential)
                    .files().list().setQ("trashed=false and name='" + filename+"'").execute();
            if(files.getFiles().size() == 0) {
                File file = createDriveService(credential)
                        .files()
                        .create(fileMetadata, new FileContent(fileType,
                                java.io.File.createTempFile(filename + "-tmp", "config")))
                        .setFields("id, parents")
                        .execute();
                fileId = file.getId();
                Log.i(TAG, "Creating File " + filename);
            } else {
                fileId = files.getFiles().get(0).getId();
            }
            Log.i(TAG,"File ID: " + fileId);

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }
        return fileId;
    }

    public static JSONObject createFolder(GoogleCredential credential, JSONObject folderConfig) {
        Log.i(TAG,"Triggering Create Folder....." + new Gson().toJson(folderConfig));
        String folderId = null;
        JSONObject response = new JSONObject();
        try {
            String folderName = folderConfig.getString("name");
            if(null == folderName || folderName.isEmpty()) {
                folderName = APPLICATION_NAME;
            }
            response.put("parentFolderName", folderName);

            if(folderConfig.has("parentFolderId")) {
                folderId = folderConfig.getString("parentFolderId");
                Log.i(TAG, "parentFolderId : " + folderId);
            }
            FileList folderList = getFolderInformation(credential, folderName);
            if(null == folderList || folderList.getFiles().size() == 0) {
                Log.i(TAG,"folderName....." + folderName + " NOT found so creating it");
                File folderMetadata = new File();
                folderMetadata.setMimeType(MIME_TYPE_FOLDER);
                folderMetadata.setName(folderName);
                if(null != folderId && !folderId.isEmpty())
                    folderMetadata.setParents(Collections.singletonList(folderId));

                File folder = createDriveService(credential).files().create(folderMetadata).execute();
                folderId = folder.getId();
                Log.i(TAG, "folder created with id " + folderId + ", name " + folderName);
            }
            else {
                folderId = folderList.getFiles().get(0).getId();
                Log.i(TAG, "folder exists with id " + folderId + ", name " + folderName);
                Log.i(TAG,"folderName....." + folderName + " found so skip creating it");
            }
            response.put("parentFolderId", folderId);

            if(folderConfig.has("files")) {
                Log.i(TAG,"Creating files.....");
                JSONArray fileConfigArray = folderConfig.getJSONArray("files");
                for(int fileCount = 0; fileCount < fileConfigArray.length(); fileCount++) {
                    JSONObject fileConfig = fileConfigArray.getJSONObject(fileCount);
                    fileConfig.put("parentFolderId", folderId);
                    response.put(fileConfig.getString("name"), createFile(credential, fileConfig));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }
        return response;
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
        Log.i(TAG,"Triggering Create Sheet.....");
        Log.i(TAG,configJSON.toString());
        String spreadsheetId = null;
        try {
            String filename = configJSON.getString("name");
            List<String> tabList = new ArrayList<>();
            if(configJSON.has("tabs")) {
                JSONArray tabListinJson = configJSON.getJSONArray("tabs");
                int tabLength = tabListinJson.length();
                for(int tabCount = 0; tabCount < tabLength; tabCount++) {
                    tabList.add(tabListinJson.getString(tabCount));
                }
            }

            Log.i(TAG,"Got tag Information....." + tabList);
            FileList files = createDriveService(credential)
                    .files()
                    .list()
                    .setQ("trashed=false and name='" + filename+"'").execute();

            if(files.getFiles().size() == 0) {
                Log.i(TAG,"Sheet does not exist. So creating one ");
                Spreadsheet spreadsheet = new Spreadsheet()
                        .setProperties(new SpreadsheetProperties().setTitle(filename));
                if(!tabList.isEmpty()) {
                    Log.i(TAG,"Tabs are defined so call addTabs");
                    spreadsheet = addTabs(credential, spreadsheet, tabList);
                } else {
                    Log.i(TAG,"Tabs are NOT defined, creating sheet with default tab");
                    spreadsheet = createSheetsService(credential).spreadsheets().create(spreadsheet)
                            .setFields("spreadsheetId")
                            .execute();
                }
                Log.i(TAG,"Created Spreadsheet Successfully. SpreadsheetId : " + spreadsheetId);
                spreadsheetId = spreadsheet.getSpreadsheetId();
                moveFile(credential, spreadsheetId, configJSON.getString("parentFolderId"));
            } else {
                Log.i(TAG,"Sheet exists.");
                spreadsheetId = files.getFiles().get(0).getId();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }
        return spreadsheetId;
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

    private static Spreadsheet addTabs(GoogleCredential credential, Spreadsheet spreadsheet, List<String> tabs)
            throws IOException {
        List<Sheet> sheets = new ArrayList<>();
        for(String tabName : tabs) {
            Sheet sheet = new Sheet();
            sheet.setProperties(new SheetProperties().setTitle(tabName));
            sheets.add(sheet);
        }
        spreadsheet.setSheets(sheets);
        Spreadsheet finalSheet = createSheetsService(credential).spreadsheets().create(spreadsheet).execute();
        return finalSheet;
    }

    public static int updateSheet(GoogleCredential credential, JSONObject configJSON) throws JSONException, IOException {
        Log.i(TAG,"Triggering updateSheet.....");
        if(!configJSON.has("data") || !configJSON.has("sheetId")) {
            Log.e(TAG, "sheetId, data are mandatory");
        }
        List<List<Object>> values = new ArrayList<>();
        List<ValueRange> data = new ArrayList<>();
        try {
            JSONArray tabs = configJSON.getJSONArray("data"); // Get all JSONArray data
            int tabCount = tabs.length();

            for (int count = 0; count < tabCount; count++) {
                JSONObject tabInfo = tabs.getJSONObject(count);
                String tabName = tabInfo.getString("tabName");
                JSONArray tabContent = tabInfo.getJSONArray("content");// Get all JSONArray data
                int rowCount = tabContent.length();
                for (int rCount = 0; rCount < rowCount; rCount++) {
                    JSONArray jsonArr = tabContent.getJSONArray(rCount);
                    List<Object> colValue = new ArrayList<>();
                    int columnCount = jsonArr.length();
                    for (int colCount = 0; colCount < columnCount; colCount++) {
                        colValue.add(jsonArr.getString(colCount));
                    }
                    values.add(colValue);
                }
                data.add(new ValueRange()
                        .setRange("'" + tabName + "'!A1:Z")
                        .setValues(values)
                );
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while reading JSON");
            e.printStackTrace();
        }
        Log.i(TAG,"data is ");
        Log.i(TAG, Arrays.toString(values.toArray()));

        String spreadsheetId = configJSON.getString("sheetId");
        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(data);

        BatchUpdateValuesResponse result =
                createSheetsService(credential).spreadsheets()
                        .values()
                        .batchUpdate(spreadsheetId, body).execute();
        int rows = result.getTotalUpdatedCells();
        Log.i(TAG, rows + "cells updated.");

        return rows;
    }
}