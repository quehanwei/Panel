package org.imdea.panel;

import android.bluetooth.BluetoothAdapter;
import android.os.Environment;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

public class FtpUpload implements Callable<String> {


    private String server;
    private int port;
    private String user;
    private String password;

    public FtpUpload() {
        // REMINDER : HIDE ACCESS CREDENTIALS
        this.server = "172.16.0.116";
        this.port = 21;
        this.user = "CommonUser";
        this.password = "123456";
    }

    public String call() {

        FTPClient ftpClient = new FTPClient();
        try {

            ftpClient.connect(server, port);
            ftpClient.login(user, password);
            ftpClient.enterLocalPassiveMode();

            ftpClient.setFileType(FTP.TELNET_TEXT_FORMAT);

            // APPROACH #1: uploads first file using an InputStream
            File firstLocalFile = new File(Environment.getExternalStorageDirectory().getPath() + "/FCNLog.txt");

            String firstRemoteFile = "FCNLog_" + BluetoothAdapter.getDefaultAdapter().getName() + "_("+ BluetoothAdapter.getDefaultAdapter().getAddress().replace(":", "-") + ")_" + System.currentTimeMillis() + ".txt";
            InputStream inputStream = new FileInputStream(firstLocalFile);

            System.out.println("Start uploading first file");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                System.out.println("The first file is uploaded successfully.");
            }

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            return "Error uploading data..";
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return "Done uploading files..";
    }
}