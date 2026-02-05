package com.inspien.eai.sftp;

import com.inspien.eai.common.Constants;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayInputStream;
import java.util.Properties;

public class SftpSender {

    private static final String HOST = "211.106.171.36";
    private static final int PORT = 20423;
    private static final String USER = "inspien";
    private static final String PASSWORD = "inspien";
    private static final String REMOTE_PATH = "/recruit/2026";

    public void send(String fileName, String content) {

        Session session = null;
        ChannelSftp channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(USER, HOST, PORT);
            session.setPassword(PASSWORD);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            channel.cd(REMOTE_PATH);

            byte[] bytes = content.getBytes(Constants.FILE_CHARSET);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            channel.put(inputStream, fileName);

            System.out.println("[SFTP 전송 성공] " + REMOTE_PATH + "/" + fileName);

        } catch (Exception e) {
            throw new RuntimeException("SFTP 전송 실패", e);

        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
