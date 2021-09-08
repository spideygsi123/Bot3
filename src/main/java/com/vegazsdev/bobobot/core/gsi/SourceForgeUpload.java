package com.vegazsdev.bobobot.core.gsi;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.vegazsdev.bobobot.commands.gsi.SourceForgeSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SourceForgeUpload {

    private static final Logger logger = LoggerFactory.getLogger(SourceForgeSetup.class);

    String user;
    String host;
    String pass;
    String proj;

    public SourceForgeUpload() {
        this.user = SourceForgeSetup.getSfConf("bot-sf-user");
        this.host = SourceForgeSetup.getSfConf("bot-sf-host");
        this.pass = SourceForgeSetup.getSfConf("bot-sf-pass");
        this.proj = SourceForgeSetup.getSfConf("bot-sf-proj");
    }

    public String uploadGsi(ArrayList<String> arrayList, String name) {

        if (name.contains(":")) {
            name = name.replace(":", " - ");
        }

        name = name + " - " + RandomStringUtils.randomAlphanumeric(10).toUpperCase();
        String path = "/home/frs/project/" + proj + "/GSI/" + name;

        try {
            JSch jsch = new JSch();

            Session session = jsch.getSession(user, host);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(pass);
            session.connect();

            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");

            sftpChannel.connect();
            sftpChannel.mkdir(path);

            for (String s : arrayList) {
                if (!s.endsWith(".img")) {
                    sftpChannel.put(s, path);
                }
            }
            return name;
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
        return null;
    }
public String uploadSgsi(ArrayList<String> arrayList, String name) {

        if (name.contains(":")) {
            name = name.replace(":", " - ");
        }

        name = name + " - " + RandomStringUtils.randomAlphanumeric(10).toUpperCase();
        String path = "/home/frs/project/" + proj + "/SGSI/" + name;

        try {
            JSch jsch = new JSch();

            Session session = jsch.getSession(user, host);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(pass);
            session.connect();

            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");

            sftpChannel.connect();
            sftpChannel.mkdir(path);

            for (String s : arrayList) {
                if (!s.endsWith(".img")) {
                    sftpChannel.put(s, path);
                }
            }
            return name;
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
        return null;
    }
}
