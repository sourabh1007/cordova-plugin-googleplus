package nl.xservices.plugins;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
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
    String ACTION_LIST_FILES    = "listFiles";
    String ACTION_DELETE_FILES  = "deleteFile";
    String ACTION_DELETE_FOLDER = "deleteFolder";
    String ACTION_TRY_SILENT_LOGIN = "trySilentLogin";
    String ACTION_GET_SIGNING_CERTIFICATE_FINGERPRINT = "getSigningCertificateFingerprint";

    String FIELD_ACCESS_TOKEN      = "accessToken";
    String FIELD_TOKEN_EXPIRES     = "expires";
    String FIELD_TOKEN_EXPIRES_IN  = "expires_in";

    String VERIFY_TOKEN_URL        = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=";

    //String options/config object names passed in to login and trySilentLogin
    String ARGUMENT_WEB_CLIENT_ID   = "webClientId";
    String ARGUMENT_SCOPES          = "scopes";
    String ARGUMENT_OFFLINE_KEY     = "offline";
    String ARGUMENT_HOSTED_DOMAIN   = "hostedDomain";

    int RC_GOOGLEPLUS           = 1552; // Request Code to identify our plugin's activities
    int KAssumeStaleTokenSec    = 60;

    JsonFactory JSON_FACTORY    = JacksonFactory.getDefaultInstance();
}
