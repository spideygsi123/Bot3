package com.vegazsdev.bobobot.core.gsi;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.vegazsdev.bobobot.commands.gsi.SourceForgeSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vegazsdev.bobobot.utils.FileTools;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    /**
     * Get model/codename of the device.
     */
    public String getModelOfOutput(String folder) {
        /*
         * Initialize core variables
         */
        File outputFolder = new File(folder);
        File file = null;
        String modelName = null;
        String buildType = null;
        String brand = null;

        /*
         * List the files
         */
        for (final File fileEntry : Objects.requireNonNull(outputFolder.listFiles())) {
            if (fileEntry.getName().endsWith(".txt") && !fileEntry.getName().contains("System-Tree")) {
                file = new File(String.valueOf(fileEntry));
            }
        }

        /*
         * Try to get codename
         */
        try (Source fileSource = Okio.source(Objects.requireNonNull(file));
             BufferedSource bufferedSource = Okio.buffer(fileSource)) {

            /*
             * Get codename
             */
            while (true) {
                String line = bufferedSource.readUtf8Line();
                if (line == null) break;
                if (line.startsWith("Brand")) brand = line.substring(7);
                if (line.startsWith("Model")) modelName = line.substring(7);
                if (line.startsWith("Build Type")) buildType = line.substring(12);
            }

            /*
             * Check if the model have special codename
             */
            if (Objects.requireNonNull(modelName).length() < 1)
                modelName = "Generic";
            else if (modelName.toLowerCase().contains("x00qd"))
                modelName = "Asus Zenfone 5";
            else if (modelName.toLowerCase().contains("qssi"))
                modelName = "Qualcomm Single System Image";
            else if (modelName.toLowerCase().contains("miatoll"))
                modelName = "Redmi Note 9*";
            else if (modelName.toLowerCase().contains("surya"))
                modelName = "Poco X3";
            else if (modelName.toLowerCase().contains("lavender"))
                modelName = "Redmi Note 7";
            else if (modelName.toLowerCase().contains("ginkgo"))
                modelName = "Redmi Note 8";
            else if (modelName.toLowerCase().contains("raphael"))
                modelName = "Mi 9T Pro";
            else if (modelName.toLowerCase().contains("mainline"))
                modelName = "AOSP/Pixel (Mainline) Device";
            else if (modelName.toLowerCase().contains("sm6250"))
                modelName = "Atoll device";
            else if (modelName.toLowerCase().contains("msi"))
                modelName = "Motorola System Image";
            else if (modelName.toLowerCase().contains("mssi"))
                modelName = "MIUI Single System Image";
            else if (modelName.toLowerCase().contains("apollo"))
                modelName = "Mi 10T/Mi 10T Pro/Redmi K30S";
            else if (modelName.toLowerCase().contains("gauguin"))
                modelName = "Mi 10T Lite/Mi 10i 5G/Redmi Note 9 5G";
            else if (modelName.equals(" "))
                modelName = "Generic";

            if (Objects.requireNonNull(brand).equals("google") || Objects.requireNonNull(brand).equals("Android")) {
                if (modelName.equals("AOSP/Pixel (Mainline) Device")) {
                    switch (Objects.requireNonNull(buildType)) {
                        // only Pixel which have QP, RP & SP (Q, R & S)
                        case "raven-user", "aosp_raven-user", "aosp_raven-userdebug", "aosp_raven-eng"
                                -> modelName = "Google Pixel 6 Pro";
                        case "oriel-user", "aosp_oriel-user", "aosp_oriel-userdebug", "aosp_oriel-eng"
                                -> modelName = "Google Pixel 6";
                        case "barbet-user", "aosp_barbet-user", "aosp_barbet-userdebug", "aosp_barbet-eng"
                                -> modelName = "Google Pixel 5a";
                        case "redfin-user", "aosp_redfin-user", "aosp_redfin-userdebug", "aosp_redfin-eng"
                                -> modelName = "Google Pixel 5";
                        case "bramble-user", "aosp_bramble-user", "aosp_bramble-userdebug", "aosp_bramble-eng"
                                -> modelName = "Google Pixel 4a 5G";
                        case "sunfish-user", "aosp_sunfish-user", "aosp_sunfish-userdebug", "aosp_sunfish-eng"
                                -> modelName = "Google Pixel 4a";
                        case "coral-user", "aosp_coral-user", "aosp_coral-userdebug", "aosp_coral-eng"
                                -> modelName = "Google Pixel 4 XL";
                        case "flame-user", "aosp_flame-user", "aosp_flame-userdebug", "aosp_flame-eng"
                                -> modelName = "Google Pixel 4";
                        case "bonito-user", "aosp_bonito-user", "aosp_bonito-userdebug", "aosp_bonito-eng"
                                -> modelName = "Google Pixel 3a XL";
                        case "sargo-user", "aosp_sargo-user", "aosp_sargo-userdebug", "aosp_sargo-eng"
                                -> modelName = "Google Pixel 3a";
                        case "crosshatch-user", "aosp_crosshatch-user", "aosp_crosshatch-userdebug", "aosp_crosshatch-eng"
                                -> modelName = "Google Pixel 3 XL";
                        case "blueline-user", "aosp_blueline-user", "aosp_blueline-userdebug", "aosp_blueline-eng"
                                -> modelName = "Google Pixel 3";
                        case "taimen-user", "aosp_taimen-user", "aosp_taimen-userdebug", "aosp_taimen-eng"
                                -> modelName = "Google Pixel 2 XL";
                        case "walleye-user", "aosp_walleye-user", "aosp_walleye-userdebug", "aosp_walleye-eng"
                                -> modelName = "Google Pixel 2";
                        case "marlin-user", "aosp_marlin-user", "aosp_marlin-userdebug", "aosp_marlin-eng"
                                -> modelName = "Google Pixel XL";
                        case "sailfish-user", "aosp_sailfish-user", "aosp_sailfish-userdebug", "aosp_sailfish-eng"
                                -> modelName = "Google Pixel";
                        default
                                -> modelName = "Google Pixel *";
                    }
                }
            } else if (Objects.requireNonNull(brand).equals("Redmi")) {
                if (modelName.equals("Atoll device")) {
                    modelName = "Redmi Note 9*";
                }
            }

            /*
             * First check
             */
            String stringToCheck = modelName.toLowerCase();
            boolean testPass = false;

            char[] characterSearch = {
                    'q', 'w', 'e', 'r', 't', 'y', 'u',
                    'i', 'o', 'p', 'a', 's', 'd', 'f',
                    'g', 'h', 'j', 'k', 'l', 'z', 'x',
                    'c', 'v', 'b', 'n', 'm'
            };

            for (int i = 0; i < stringToCheck.length(); i++) {
                char character = stringToCheck.charAt(i);
                for (char search : characterSearch) {
                    if (search == character) {
                        testPass = true;
                        break;
                    }
                }
            }

            if (!testPass) return "Generic";
            return modelName;
        } catch (IOException e) {
            return "Generic";
        }
    }

    public String uploadGsi(ArrayList<String> arrayList, String name) {

        if (name.contains(":")) {
            name = name.replace(":", " - ");
        }

        String toolPath = "ErfanGSIs/";
        String model = getModelOfOutput(toolPath + "output");
        name = name + " - " + model + " - " + RandomStringUtils.randomAlphanumeric(10).toUpperCase();
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

        String toolPath = "XiaoxindadaSGSIs/";
        String model = getModelOfOutput(toolPath + "output");
        name = name + " - " + model + " - " + RandomStringUtils.randomAlphanumeric(10).toUpperCase();
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
