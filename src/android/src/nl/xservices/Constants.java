package nl.xservices.plugins;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

public interface Constants {

    String APPLICATION_NAME     = "MyApp";

    String ACTION_IS_AVAILABLE  = "isAvailable";
    String ACTION_LOGIN         = "login";
    String ACTION_LOGOUT        = "logout";
    String ACTION_DISCONNECT    = "disconnect";
    String ACTION_CREATE_FILE   = "createFile";
    String ACTION_CREATE_FOLDER = "createFolder";
    String ACTION_CREATE_SHEET  = "createSheet";
    String ACTION_LIST_FILES    = "listFiles";
    String ACTION_DELETE_FILES  = "deleteFile";
    String ACTION_DELETE_FOLDER = "deleteFolder";
    String ACTION_TRY_SILENT_LOGIN = "trySilentLogin";
    String ACTION_GET_SIGNING_CERTIFICATE_FINGERPRINT = "getSigningCertificateFingerprint";

    String FIELD_ACCESS_TOKEN      = "accessToken";
    String FIELD_TOKEN_EXPIRES     = "expires";
    String FIELD_TOKEN_EXPIRES_IN  = "expires_in";

    String MIME_TYPE_TEXT_PLAIN     = "text/plain";
    String MIME_TYPE_GOOGLE_SHEET   = "application/vnd.google-apps.spreadsheet";
    String MIME_TYPE_FOLDER         = "application/vnd.google-apps.folder";

    String VERIFY_TOKEN_URL        = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=";

    //String options/config object names passed in to login and trySilentLogin
    String ARGUMENT_WEB_CLIENT_ID   = "webClientId";
    String ARGUMENT_SCOPES          = "scopes";
    String ARGUMENT_OFFLINE_KEY     = "offline";
    String ARGUMENT_HOSTED_DOMAIN   = "hostedDomain";

    int RC_GOOGLEPLUS           = 1552; // Request Code to identify our plugin's activities
    int KAssumeStaleTokenSec    = 60;

    JsonFactory JSON_FACTORY    = JacksonFactory.getDefaultInstance();
    NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
}
