package de.fhg.iais.roberta.connection;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The server communicator runs the server protocol on behalf of the actual robot hardware.
 * This class provides access to push requests, downloads the user program and download system libraries for
 * the upload function.
 *
 * @author dpyka
 */
public class ServerCommunicator {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ServerCommunicator.class);

    private static final String PUSH_ADDRESS = "/rest/pushcmd";
    private static final String DOWNLOAD_ADDRESS = "/rest/download";
    private static final String UPDATE_ADDRESS = "/rest/update";
    private static final int CONNECT_TIMEOUT = 5000;

    private String serverAddress;
    private String filename = "";

    private String serverUpdateAddress;
    private String serverUpdateChecksumAddress;
    private final CloseableHttpClient httpclient;
    private HttpPost post = null;
    private String halZipPath;
    private String workingDirectory;

    /**
     * @param serverAddress either the default address taken from the properties file or the custom address entered in the gui.
     */
    public ServerCommunicator(String serverAddress) {
        this.serverAddress = serverAddress;
        updateCustomServerAddressNAO(serverAddress);
        this.httpclient = HttpClients.createDefault();
    }

    public String getServerAddress() {
        return this.serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * @return the file name of the last binary file downloaded of the server communicator object.
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Sends a push request to the open roberta server for registration or keeping the connection alive. This will be hold by the server for approximately 10
     * seconds and then answered.
     *
     * @param requestContent data from the EV3 plus the token and the command send to the server (CMD_REGISTER or CMD_PUSH)
     * @return response from the server
     * @throws IOException if the server is unreachable for whatever reason.
     */
    public JSONObject pushRequest(JSONObject requestContent) throws IOException {
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Accept", "application/json");

        URLConnection conn = openURLConnection(this.serverAddress + PUSH_ADDRESS, "POST", requestProperties);
        sendServerRequest(requestContent, conn);
        String responseText = getServerResponse(conn);

        return new JSONObject(responseText);
    }

    private static URLConnection openURLConnection(String url, String requestMethod, Map<String, String> requestProperties) throws IOException {
        URLConnection conn;
        try {
            if ( url.contains("localhost") ) { // workaround for HttpParser warning server side when connecting via localhost
                conn = getHttpConnection(url, requestMethod, requestProperties);
            } else {
                conn = getHttpsConnection(url, requestMethod, requestProperties);
            }
            conn.connect();
        } catch ( IOException ioException ) {
            conn = getHttpConnection(url, requestMethod, requestProperties);
            conn.connect();
        }
        return conn;
    }

    private static HttpURLConnection getHttpConnection(String urlAddress, String requestMethod, Map<String, String> requestProperties) throws IOException {
        URL url = new URL("http://" + urlAddress);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setURLConnectionProperties(conn, requestMethod, requestProperties);
        return conn;
    }

    private static HttpsURLConnection getHttpsConnection(String urlAddress, String requestMethod, Map<String, String> requestProperties) throws IOException {
        URL url = new URL("https://" + urlAddress);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        setURLConnectionProperties(conn, requestMethod, requestProperties);
        return conn;
    }

    private static void setURLConnectionProperties(HttpURLConnection conn, String requestMethod, Map<String, String> requestProperties)
        throws ProtocolException {
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod(requestMethod);

        for ( Entry<String, String> property : requestProperties.entrySet() ) {
            conn.setRequestProperty(property.getKey(), property.getValue());
        }
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        conn.setRequestProperty("Content-Type", "application/json");
    }

    private static String getServerResponse(URLConnection conn) throws IOException {
        InputStream responseEntity = new BufferedInputStream(conn.getInputStream());
        String responseText = IOUtils.toString(responseEntity, "UTF-8");
        responseEntity.close();
        return responseText;
    }

    private static void sendServerRequest(JSONObject requestContent, URLConnection conn) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestContent.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    /**
     * Downloads a user program from the server as binary. The http POST is used here.
     *
     * @param requestContent all the content of a standard push request.
     * @return the binary file of the response
     * @throws IOException if the server is unreachable or something is wrong with the binary content.
     */
    public byte[] downloadProgram(JSONObject requestContent) throws IOException {
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Accept", "application/octet-stream");

        URLConnection conn = openURLConnection(this.serverAddress + DOWNLOAD_ADDRESS, "POST", requestProperties);
        sendServerRequest(requestContent, conn);

        return getBinaryFileFromResponse(conn);
    }

    /**
     * Basically the same as downloading a user program but without any information about the EV3. It uses http GET(!).
     *
     * @param fwFile name of the file in the url as suffix ( .../rest/update/ev3menu)
     * @return the binary file of the response
     * @throws IOException if the server is unreachable or something is wrong with the binary content.
     */
    public byte[] downloadFirmwareFile(String fwFile) throws IOException {
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Accept", "application/octet-stream");

        URLConnection conn = openURLConnection(this.serverAddress + UPDATE_ADDRESS + '/' + fwFile, "GET", requestProperties);

        return getBinaryFileFromResponse(conn);
    }

    private byte[] getBinaryFileFromResponse(URLConnection conn) throws IOException {
        try (InputStream responseEntity = new BufferedInputStream(conn.getInputStream())) {
            this.filename = conn.getHeaderField("Filename");
            return IOUtils.toByteArray(responseEntity);
        }
    }

    /**
     * Update the server address if the user wants to use an own installation of open roberta with a different IP address.
     *
     * @param customServerAddress for example localhost:1999 or 192.168.178.10:1337
     */
    public void updateCustomServerAddressNAO(String customServerAddress) {
        String prefix;
        if ( customServerAddress.contains("443") ) {
            prefix = "https://";
        } else {
            prefix = "http://";
        }
        this.serverUpdateAddress = prefix + customServerAddress + "/update/nao/v2-1-4-3/hal";
        this.serverUpdateChecksumAddress = prefix + customServerAddress + "/update/nao/v2-1-4-3/hal/checksum";
        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.halZipPath = System.getenv("APPDATA") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getenv("APPDATA") + "/OpenRoberta/";
        } else {
            this.halZipPath = System.getProperty("user.home") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getProperty("user.home") + "/OpenRoberta/";
        }
    }

    public boolean verifyHalChecksumNAO() throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        Path path = Paths.get(this.halZipPath);
        try {
            digest.update(Files.readAllBytes(path));
        } catch ( IOException e ) {
            return false;
        }
        byte[] result = digest.digest();
        LOG.info("Current hals checksum: {} ", Base64.getEncoder().encodeToString(result));

        HttpGet get = new HttpGet(this.serverUpdateChecksumAddress);
        get.setHeader("User-Agent", "Java/1.7.0_60");
        CloseableHttpResponse response = this.httpclient.execute(get);
        HttpEntity responseEntity = response.getEntity();

        BufferedReader rd = new BufferedReader(new InputStreamReader(responseEntity.getContent()));
        String line;
        if ( (line = rd.readLine()) != null ) {
            LOG.info("Received checksum from server: {} ", line);
            return Base64.getEncoder().encodeToString(result).equals(line);
        } else {
            return false;
        }
    }

    public void updateHalNAO() throws IOException, ZipException {
        FileUtils.copyURLToFile(new URL(this.serverUpdateAddress), new File(this.halZipPath));
        File dataFile = new File(this.halZipPath);
        ZipFile zipFile = new ZipFile(dataFile);
        zipFile.extractAll(this.workingDirectory);
        LOG.info("New HAL downloaded and unzipped");
    }

    /**
     * Cancel a pending push request (which is blocking in another thread), if the user wants to disconnect.
     */
    public void abortNAO() {
        if ( this.post != null ) {
            this.post.abort();
        }
    }

    /**
     * Shut down the http client.
     */
    public void shutdownNAO() {
        try {
            this.httpclient.close();
        } catch ( IOException e ) {
            // ok
        }
    }
}
