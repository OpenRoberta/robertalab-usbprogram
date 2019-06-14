package de.fhg.iais.roberta.connection.nao;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class NaoCommunicator {
    private static Logger LOG = LoggerFactory.getLogger("NaoCommunicator");

    private static final int SSH_PORT = 22;
    private static final int FTP_PORT = 21;

    private final String name;
    private final String ip;
    private String username;
    private String password;

    private final JSch jsch = new JSch();
    private final FTPClient ftpClient = new FTPClient();
    private String halZipPath;
    private String workingDirectory;

    public NaoCommunicator(String name, String ip, String username, String password) {
        this.name = name;
        this.ip = ip;
        this.username = username;
        this.password = password;
        //        this.ftpClient.setConnectTimeout(20000);
        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.halZipPath = System.getenv("APPDATA") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getenv("APPDATA") + "/OpenRoberta/";
        } else {
            this.halZipPath = System.getProperty("user.home") + "/OpenRoberta/roberta.zip";
            this.workingDirectory = System.getProperty("user.home") + "/OpenRoberta/";
        }
    }

    public void uploadFile(byte[] binaryfile, String fileName) throws Exception {
        // LOG.info("Robot IP: " + this.ip + "  Username: " + this.username + "  Password: " + this.password);
        //connect to ftp server(NAO)
        this.ftpClient.connect(this.ip, FTP_PORT);
        boolean success = this.ftpClient.login(this.username, this.password);
        showServerReply();
        int replyCode = this.ftpClient.getReplyCode();
        //print error message if connection is not established
        if ( !FTPReply.isPositiveCompletion(replyCode) ) {
            LOG.info("Operation failed. Server reply code: " + replyCode);
        }
        showServerReply();

        if ( !success ) {
            LOG.info("Could not login to the FTP server!");
            throw new Exception("Could not login to the FTP server!");
        }
        List<String> fileNames = new ArrayList<>();
        fileNames.add(this.workingDirectory + "/roberta/__init__.py");
        fileNames.add(this.workingDirectory + "/roberta/blockly_methods.py");
        fileNames.add(this.workingDirectory + "/roberta/original_hal.py");
        fileNames.add(this.workingDirectory + "/roberta/speech_recognition_module.py");
        fileNames.add(this.workingDirectory + "/roberta/face_recognition_module.py");
        byte[] fileContents;
        try {
            for ( String fname : fileNames ) {
                fileContents = getFileFromFileSystem(fname);
                String[] remoteNames = fname.split("/");
                String remoteName = remoteNames[remoteNames.length - 1];
                ftpTransfer("roberta/" + remoteName, fileContents);
            }
            ftpTransfer(fileName, binaryfile);
            sshCommand("python " + fileName);
            sshCommand("rm " + fileName);
            sshCommand("rm -r roberta");
        } catch ( Exception e ) {
            //            key = Key.NAO_PROGRAM_UPLOAD_ERROR_CONNECTION_NOT_ESTABLISHED;
        }
        //        return key;
        //logout
        this.ftpClient.logout();
        LOG.info("file transferred");

    }

    private byte[] getFileFromResources(String fileName) {
        try {
            String input = "";
            Scanner scanner = new Scanner(NaoCommunicator.class.getClassLoader().getResourceAsStream(fileName));
            while ( scanner.hasNextLine() ) {
                input += scanner.nextLine() + '\n';
            }
            scanner.close();
            //replace the line
            //System.out.println(this.ip);
            //            input = input.replace("self.NAO_IP = \"\"", "self.NAO_IP = \"" + this.ip + "\"");
            return input.getBytes();

        } catch ( Exception e ) {
            System.out.println(e.getMessage());
            System.out.println("Problem reading or writing " + fileName + " file.");
        }
        return null;
    }

    private byte[] getFileFromFileSystem(String fileName) {
        Path filePath = Paths.get(fileName);
        try {
            return Files.readAllBytes(filePath);
        } catch ( Exception e ) {
            System.out.println(e);
        }
        return null;
    }

    private void sshCommand(String command) {
        //implement the abstract class MyUserInfo
        UserInfo ui = new MyUserInfo();
        try {
            Session session = this.jsch.getSession(this.username, this.ip, SSH_PORT);
            session.setPassword(this.password);
            session.setUserInfo(ui);
            session.setTimeout(50000);
            //fingerprint is not accepted
            //uncomment this part if key fingerprint is not accepted
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            //establish session connection
            session.connect();
            //open channel and send command
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            //set streams
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            //establish channel connection
            channel.connect();
            //show messages on the ssh channel
            byte[] tmp = new byte[1024];
            while ( true ) {
                while ( in.available() > 0 ) {
                    int i = in.read(tmp, 0, 1024);
                    if ( i < 0 ) {
                        break;
                    }
                    //System.out.print(new String(tmp, 0, i));
                }
                if ( channel.isClosed() ) {
                    if ( in.available() > 0 ) {
                        continue;
                    }
                    // LOG.info("exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch ( Exception ee ) {
                }
            }
            //disconnect from channel and session
            channel.disconnect();
            session.disconnect();
        } catch ( Exception e ) {
            System.out.println(e);

        }
    }

    private void showServerReply() {
        String[] replies = this.ftpClient.getReplyStrings();
        if ( replies != null && replies.length > 0 ) {
            for ( String aReply : replies ) {
                LOG.info("NAO-SERVER: " + aReply);
            }
        }
    }

    private void ftpTransfer(String fileName, byte[] binaryFile) throws Exception {
        LOG.info(String.format("transferring file: %s to NAO...", fileName));
        //Use local passive mode to avoid problems with firewalls
        this.ftpClient.enterLocalPassiveMode();
        FTPFile[] directories = this.ftpClient.listDirectories();
        boolean isRobertaDir = false;
        for ( FTPFile dir : directories ) {
            if ( dir.getName().equals("roberta") ) {
                isRobertaDir = true;
            }
        }
        if ( !isRobertaDir ) {
            this.ftpClient.makeDirectory("roberta");
        }
        //store file on the robot
        InputStream input = new ByteArrayInputStream(binaryFile);
        this.ftpClient.storeFile(fileName, input);
        System.out.println(this.ftpClient.getReplyCode());
    }

    //abstract MyUserInfo needed for ssh connection.
    public class MyUserInfo implements UserInfo, UIKeyboardInteractive {
        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptYesNo(String str) {
            return false;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return false;
        }

        @Override
        public boolean promptPassword(String message) {
            return false;
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
            return null;
        }
    }

    public void updateRobotLogin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * @return true if a program is currently running, false otherwise
     * @throws IOException
     */
    public NaoState getNAOstate() {
        return NaoState.WAITING_FOR_PROGRAM;
    }

    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("firmwarename", "Nao");
        deviceInfo.put("robot", "nao");
        deviceInfo.put("firmwareversion", "2.1");
        deviceInfo.put("macaddr", "usb");
        deviceInfo.put("brickname", name);
        deviceInfo.put("battery", "1.0");
        return deviceInfo;
    }
}
