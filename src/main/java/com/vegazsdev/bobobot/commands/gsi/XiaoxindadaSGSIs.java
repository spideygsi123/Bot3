package com.vegazsdev.bobobot.commands.gsi;

import com.vegazsdev.bobobot.TelegramBot;
import com.vegazsdev.bobobot.core.command.Command;
import com.vegazsdev.bobobot.core.gsi.GSICmdObj;
import com.vegazsdev.bobobot.core.gsi.SourceForgeUpload;
import com.vegazsdev.bobobot.db.PrefObj;
import com.vegazsdev.bobobot.utils.Config;
import com.vegazsdev.bobobot.utils.FileTools;
import com.vegazsdev.bobobot.utils.JSONs;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Main command of the bot specialized in making SGSI (Xiaoxindada tool).
 * <p>
 * This class consists of doing SGSI using the Xiaoxindada tool, named XiaoxindadaSGSIs.
 * <p>
 * Some methods:
 * <ul>
 *     <li>{@link #isCommandValid(Update)}</li>
 *     <li>{@link #try2AvoidCodeInjection(String)}</li>
 *     <li>{@link #isSGSIValid(String)}</li>
 *     <li>{@link #createSGSI(GSICmdObj, TelegramBot)}</li>
 *     <li>{@link #userHasPortPermissions(String)}</li>
 *     <li>{@link #getModelOfOutput(String)}</li>
 *     <li>{@link #addPortPerm(String)}</li>
 *
 * </ul>
 */
@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class XiaoxindadaSGSIs extends Command {

    /**
     * Logger: To send warning, info & errors to terminal.
     */
    private static final Logger logger = LoggerFactory.getLogger(XiaoxindadaSGSIs.class);

    /**
     * Main variables to SGSI process.
     */
    private static final ArrayList<GSICmdObj> queue = new ArrayList<>();
    private static boolean isPorting = false;
    private final String toolPath = "XiaoxindadaSGSIs/";

    /**
     * Get supported versions from XiaoxindadaSGSIs tool.
     */
    private final File[] supportedSGSIs12 = new File(toolPath + "other/roms/").listFiles(File::isDirectory);

    /**
     * Some workarounds.
     */
    private String messageError = "";
    private String infoSGSI = "";
    private String noticeSGSI = "";
    private String developerNoticeSGSI = "";

    public XiaoxindadaSGSIs() {
        super("jurl2sgsi");
    }

    private static String[] listFilesForFolder(final File folder) {
        StringBuilder paths = new StringBuilder();
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                if (fileEntry.getName().contains(".img")) {
                    paths.append(fileEntry.getAbsolutePath()).append("\n");
                }
            }
        }
        return paths.toString().split("\n");
    }

    @Override
    public void botReply(Update update, TelegramBot bot, PrefObj prefs) {
        String[] msgComparableRaw = update.getMessage().getText().split(" ");
        if (update.getMessage().getText().contains(" ")) {
            switch (msgComparableRaw[1]) {
                case "allowuser" -> {
                    if (update.getMessage().getReplyToMessage() != null) {
                        String userid = update.getMessage().getReplyToMessage().getFrom().getId().toString();
                        if (addPortPerm(userid)) {
                            bot.sendReply(prefs.getString("xgsi_allowed").replace("%1", userid), update);
                        }
                    } else {
                        bot.sendReply(prefs.getString("xgsi_allow_by_reply").replace("%1", prefs.getHotkey())
                                .replace("%2", this.getAlias()), update);
                    }
                }
                case "queue" -> {
                    if (!queue.isEmpty()) {
                        StringBuilder v = new StringBuilder();
                        for (int i = 0; i < queue.size(); i++) {
                            v.append("#").append(i + 1).append(": ").append(queue.get(i).getGsi()).append("\n");
                        }
                        bot.sendReply(prefs.getString("xgsi_current_queue")
                                .replace("%2", v.toString())
                                .replace("%1", String.valueOf(queue.size())), update);
                    } else {
                        bot.sendReply(prefs.getString("xgsi_no_ports_queue"), update);
                    }
                }
                case "cancel" -> {
                    if (isPorting) {
                        ProcessBuilder pb;
                        pb = new ProcessBuilder("/bin/bash", "-c", "kill -TERM -- -$(ps ax | grep url2SGSI.sh | grep -v grep | awk '{print $1;}')");
                        try {
                            pb.start();
                        } catch (IOException ignored) {}

                        if (FileTools.checkIfFolderExists(toolPath + "output")) {
                            if (FileTools.deleteFolder(toolPath + "output")) {
                                logger.info("Output folder deleted");
                            }
                        }
                    } else {
                        bot.sendReply(prefs.getString("xgsi_no_ports_queue"), update);
                    }
                }
                case "list", "roms", "sgsis" -> sendSupportedROMs(update, bot, prefs);
                default -> {
                    messageError = prefs.getString("xgsi_fail_to_build_gsi");
                    if (userHasPortPermissions(update.getMessage().getFrom().getId().toString())) {
                        if (!FileTools.checkIfFolderExists("XiaoxindadaSGSIs")) {
                            bot.sendReply(prefs.getString("xgsi_dont_exists_tool_folder"), update);
                        } else {
                            GSICmdObj gsiCommand = isCommandValid(update);
                            if (gsiCommand != null) {
                                boolean isSGSITypeValid = isSGSIValid(gsiCommand.getGsi());
                                if (isSGSITypeValid) {
                                    if (!isPorting) {
                                        isPorting = true;
                                        createSGSI(gsiCommand, bot);
                                        while (queue.size() != 0) {
                                            GSICmdObj portNow = queue.get(0);
                                            queue.remove(0);
                                            createSGSI(portNow, bot);
                                        }
                                        isPorting = false;
                                    } else {
                                        queue.add(gsiCommand);
                                        bot.sendReply(prefs.getString("xgsi_added_to_queue"), update);
                                    }
                                } else {
                                    sendSupportedROMs(update, bot, prefs);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            bot.sendReply(prefs.getString("bad_usage"), update);
        }
    }

    /**
     * Avoid shell usage on jurl2sgsi command.
     */
    private String try2AvoidCodeInjection(String parameters) {
        try {
            parameters = parameters.replace("&", "")
                    .replace("\\", "").replace(";", "").replace("<", "")
                    .replace(">", "").replace("|", "");
        } catch (Exception e) {
            return parameters;
        }
        return parameters;
    }

    /**
     * Check if the args passed to jurl2sgsi command is valid.
     */
    private GSICmdObj isCommandValid(Update update) {
        GSICmdObj gsiCmdObj = new GSICmdObj();
        String msg = update.getMessage().getText().replace(Config.getDefConfig("bot-hotkey") + this.getAlias() + " ", "");
        String[] msgComparableRaw = update.getMessage().getText().split(" "), paramComparableRaw;
        boolean canContinueLoop = false;
        String url, gsi, param;

        if (msgComparableRaw.length >= 3) {
            try {
                url = msg.split(" ")[1];
                gsiCmdObj.setUrl(url);
                gsi = msg.split(" ")[2];
                gsiCmdObj.setGsi(gsi);
                param = msg.replace(msgComparableRaw[0], "").replace(msgComparableRaw[1], "").replace(msgComparableRaw[2], "").trim();
                param = try2AvoidCodeInjection(param);
                paramComparableRaw = param.split(" ");

                if (param.contains("-nv")) noticeSGSI = "<b>SGSI Notice</b>\nThis SGSI requires the vendor to have the same version of the system!\n\n";

                StringBuilder stringBuilder = new StringBuilder();
                for (String string : paramComparableRaw) {
                    if (string.startsWith("-")) canContinueLoop = true;
                    if (!string.startsWith("-")) break;
                    if (canContinueLoop) stringBuilder.append(string).append(" ");
                }

                developerNoticeSGSI = param.replace(String.valueOf(stringBuilder), "");
                if (developerNoticeSGSI.contains(param)) developerNoticeSGSI = "";
                if (!developerNoticeSGSI.equals("")) developerNoticeSGSI =
                        "<b>Developer Note</b>\n"
                        + param.replace(String.valueOf(stringBuilder), "")
                        + "\n\n";

                param = String.valueOf(stringBuilder);

                gsiCmdObj.setParam(param);
                gsiCmdObj.setUpdate(update);
                return gsiCmdObj;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Check if the SGSI is valid.
     * It checks if the tool is updated (if has S support), check if the ROM exists too.
     */
    private boolean isSGSIValid(String gsi) {
        File[] supportedSGSIsS = ArrayUtils.addAll(supportedSGSIs12);

        if (supportedSGSIsS == null) return false;

        boolean canRunYet = true;

        try {
            String sgsi2 = null;

            if (gsi.contains(":")) {
                sgsi2 = gsi.split(":")[0];
            }
            if (canRunYet) {
                for (File supportedSGSI : Objects.requireNonNull(supportedSGSIsS)) {
                    if (Objects.requireNonNullElse(sgsi2, gsi).equals(supportedSGSI.getName())) return true;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Avoid users abuse, only users with port permission can use jurl2sgsi command.
     */
    private boolean userHasPortPermissions(String idAsString) {
        if (Objects.equals(Config.getDefConfig("bot-master"), idAsString)) {
            return true;
        }
        String portConfigFile = "configs/allowed2port.json";
        return Arrays.asList(Objects.requireNonNull(JSONs.getArrayFromJSON(portConfigFile)).toArray()).contains(idAsString);
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
            else if (modelName.equals("a30"))
                modelName = "Samsung Galaxy A30";
            else if (modelName.equals("a20"))
                modelName = "Samsung Galaxy A20";
            else if (modelName.equals("a10"))
                modelName = "Samsung Galaxy A10";
            else if (modelName.equals("LE2123"))
                modelName = "OnePlus 9 Pro";
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


    /**
     * Create a SGSI with one method.
     */
    private void createSGSI(GSICmdObj gsiCmdObj, TelegramBot bot) {
        /*
         * Variables to bash
         */
        ProcessBuilder pb;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        /*
         * Pre-final SGSI process variables
         */
        boolean success = false;
        Update update = gsiCmdObj.getUpdate();
        StringBuilder fullLogs = new StringBuilder();
        String builder = update.getMessage().getFrom().getFirstName();
        Long builderID = update.getMessage().getFrom().getId();

        /*
         * Start the SGSI process
         */
        pb = new ProcessBuilder("/bin/bash", "-c",
                "cd " + toolPath + " ; ./url2SGSI.sh '" + gsiCmdObj.getUrl() + "' " + gsiCmdObj.getGsi() + " " + gsiCmdObj.getParam()
        );
        fullLogs.append("<code>-> Starting process...</code>");

        /*
         * Send the message, it's SGSI time!
         */
        int id = bot.sendReply(fullLogs.toString(), update);

        /*
         * GSI build process
         */
        try {
            /*
             * Start process
             */
            pb.redirectErrorStream(true);
            Process process = pb.start();

            /*
             * Prepare in/output log
             */
            inputStream = process.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);

            /*
             * Some variables (to get buffer output using readLine())
             */
            String line;

            /*
             * Avoid aria2 logs
             */
            boolean weDontNeedAria2Logs = true;

            while ((line = bufferedReader.readLine()) != null) {
                line = "<code>" + line + "</code>";
                if (line.contains("Downloading firmware")) {
                    weDontNeedAria2Logs = false;
                    fullLogs.append("\n").append(line);
                    bot.editMessage(fullLogs.toString(), update, id);
                }

                if (line.contains("Extracting")) {
                    weDontNeedAria2Logs = true;
                }

                if (weDontNeedAria2Logs) {
                    fullLogs.append("\n").append(line);
                    bot.editMessage(fullLogs.toString(), update, id);
                    if (line.contains("SGSI done")) {
                        success = true;
                    }
                }
            }

            /*
             * If the SGSI got true boolean, it will create gzip, upload, prepare message and send it to channel/group
             */
            if (success) {
                fullLogs.append("\n").append("<code>-> Creating gzip...</code>");
                bot.editMessage(fullLogs.toString(), update, id);

                /*
                 * Get files inside XiaoxindadaSGSIs/output
                 */
                String[] gzipFiles = listFilesForFolder(new File("XiaoxindadaSGSIs" + "/output"));

                /*
                 * Gzip the files
                 */
                for (String gzipFile : gzipFiles) {
                    new FileTools().gzipFile(gzipFile, gzipFile + ".gz");
                }

                /*
                 * Create ArrayList to save A/B, Aonly & vendorOverlays files
                 */
                ArrayList<String> arr = new ArrayList<>();

                /*
                 * A/B, Aonly & vendorOverlay Atomic variables
                 */
                AtomicReference<String> aonly = new AtomicReference<>("");
                AtomicReference<String> ab = new AtomicReference<>("");
                AtomicReference<String> vendorOverlays = new AtomicReference<>("");
                AtomicReference<String> odmOverlays = new AtomicReference<>("");

                /*
                 * Try to get files inside XiaoxindadaSGSIs/output and set into correct variable (ex: A/B image to A/B variable)
                 */
                try (Stream<Path> paths = Files.walk(Paths.get("XiaoxindadaSGSIs/output/"))) {
                    paths
                            .filter(Files::isRegularFile)
                            .forEach(fileName -> {
                                if (fileName.toString().endsWith(".gz") || fileName.toString().endsWith(".txt")) {
                                    arr.add(fileName.toString());
                                    if (fileName.toString().contains("Aonly")) {
                                        aonly.set(FilenameUtils.getBaseName(fileName.toString()) + "." + FilenameUtils.getExtension(fileName.toString()));
                                    } else if (fileName.toString().contains("AB")) {
                                        ab.set(FilenameUtils.getBaseName(fileName.toString()) + "." + FilenameUtils.getExtension(fileName.toString()));
                                    } else if (fileName.toString().contains("VendorOverlays")) {
                                        vendorOverlays.set(FilenameUtils.getBaseName(fileName.toString()) + "." + FilenameUtils.getExtension(fileName.toString()));
                                    } else if (fileName.toString().contains("ODMOverlays")) {
                                        odmOverlays.set(FilenameUtils.getBaseName(fileName.toString()) + "." + FilenameUtils.getExtension(fileName.toString()));
                                    }
                                }
                                if (fileName.toString().contains(".txt") && !fileName.toString().contains("System-Tree")) {
                                    infoSGSI = fileName.toString();
                                }
                            });
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }

                /*
                 * Now say the bot will upload files to SourceForge
                 */
                fullLogs.append("\n").append("<code>-> Uploading to SF...</code>");
                bot.editMessage(fullLogs.toString(), update, id);

                /*
                 * SourceForge upload time
                 */
                String re = new SourceForgeUpload().uploadSgsi(arr, gsiCmdObj.getGsi());
                re = re + "/";

                /*
                 * Check the SGSI name has special name, like this:
                 * !jurl2sgsi <url link> Generic:StatiXOS-Nuclear
                 * The name of this ROM is 'StatiXOS Nuclear' (without quotes), the '-' (char) will be the replacement char, to be used as a space
                 */
                if (gsiCmdObj.getGsi().contains(":")) {
                    gsiCmdObj.setGsi(gsiCmdObj.getGsi().split(":")[1]);
                    gsiCmdObj.setGsi(gsiCmdObj.getGsi().replace("-", " "));
                }
                /*
                 * Prepare SGSI message
                 */
                SendMessage sendMessage = new SendMessage();
                sendMessage.setDisableWebPagePreview(true);
                sendMessage.enableHtml(true);

                /*
                 * Prepare InlineKeyboardButton
                 */
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

                if (!aonly.toString().trim().equals("")) {
                    List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText("Aonly Download");
                    inlineKeyboardButton.setUrl("https://sourceforge.net/projects/"  + SourceForgeSetup.getSfConf("bot-sf-proj") + "/files/SGSI/" + re + aonly);
                    rowInline2.add(inlineKeyboardButton);
                    rowsInline.add(rowInline2);
                }

                if (!ab.toString().trim().equals("")) {
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText("A/B Download");
                    inlineKeyboardButton.setUrl("https://sourceforge.net/projects/"  + SourceForgeSetup.getSfConf("bot-sf-proj") + "/files/SGSI/" + re + ab);
                    rowInline.add(inlineKeyboardButton);
                    rowsInline.add(rowInline);
                }

                if (!vendorOverlays.toString().trim().equals("")) {
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText("Vendor Overlays Download");
                    inlineKeyboardButton.setUrl("https://sourceforge.net/projects/"  + SourceForgeSetup.getSfConf("bot-sf-proj") + "/files/SGSI/" + re + vendorOverlays);
                    rowInline.add(inlineKeyboardButton);
                    rowsInline.add(rowInline);
                }

                if (!odmOverlays.toString().trim().equals("")) {
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    inlineKeyboardButton.setText("ODM Overlays Download");
                    inlineKeyboardButton.setUrl("https://sourceforge.net/projects/"  + SourceForgeSetup.getSfConf("bot-sf-proj") + "/files/SGSI/" + re + odmOverlays);
                    rowInline.add(inlineKeyboardButton);
                    rowsInline.add(rowInline);
                }

                /*
                 * Finish InlineKeyboardButton setup
                 */
                markupInline.setKeyboard(rowsInline);
                sendMessage.setReplyMarkup(markupInline);

                /*
                 * Info of SGSI image
                 */
                String descSGSI = "" + new FileTools().readFile(infoSGSI).trim();

                /*
                 * Prepare message id
                 */
                int idSGSI;

                /*
                 * Send SGSI message
                 */
                sendMessage.setText("<b>" + gsiCmdObj.getGsi() + " SGSI</b>"
                        + "\n<b>From</b> " + getModelOfOutput(toolPath + "output")
                        + "\n\n<b>Information</b>\n<code>" + descSGSI
                        + "</code>\n\n"
                        + noticeSGSI
                        + developerNoticeSGSI
                        + "<b>✵ RK137 GSI ✵</b>" + "\n"
                        + "<a href=\"https://t.me/rk137gsi\">Channel</a> | <a href=\"https://github.com/rk137gsi\">GitHub</a> |  <a href=\"https://sourceforge.net/projects/gsis137/files/SGSI\">SF Folder</a>"
                        + "\n\n<b>Credits :</b>" + "\n"
                        + "<a href=\"https://github.com/Erfanoabdi\">Erfan</a>" + " | "
                        + "<a href=\"https://github.com/xiaoxindada\">Xiaoxindada</a>" + " | " 
                        + "<a href=\"https://github.com/phhusson\">Phh</a>" + " | " 
                        + "<a href=\"https://github.com/TrebleExperience\">Treble Exp</a>"
                );
                sendMessage.setChatId(Objects.requireNonNull(SourceForgeSetup.getSfConf("bot-announcement-id")));
                idSGSI = bot.sendMessageAsyncBase(sendMessage, update);

                fullLogs.append("\n").append("-> Finished!");
                bot.editMessage(fullLogs.toString(), update, id);

                /*
                 * Reply kthx
                 */
                if (idSGSI != 0) bot.sendReply("Done! Here is the <a href=\"" + "https://t.me/" + Config.getDefConfig("publicChannel")  + "/" + idSGSI + "\">link</a> post", update);

                /*
                 * Delete output/input folder with two codes (The first seems not worked so to make sure, use other code for it)
                 */
                FileUtils.deleteDirectory(new File(toolPath + "output"));
                if (FileTools.checkIfFolderExists(toolPath + "output")) {
                    if (FileTools.deleteFolder(toolPath + "output")) {
                        logger.info("Output folder deleted");
                    }
                }

                FileUtils.deleteDirectory(new File(toolPath + "tmp"));
                if (FileTools.checkIfFolderExists(toolPath + "tmp")) {
                    if (FileTools.deleteFolder(toolPath + "tmp")) {
                        logger.info("tmp folder deleted");
                    }
                }

                /*
                 * Cleanup variables
                 */
                ab.set(null);
                aonly.set(null);
                vendorOverlays.set(null);
                odmOverlays.set(null);
                infoSGSI = null;
                developerNoticeSGSI = null;
                arr.clear();
                gsiCmdObj.clean();
            } else {
                bot.sendReply(messageError, update);
            }
        } catch (Exception ex) {
            bot.sendReply(messageError, update);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioException) {
                    logger.error(ioException.getMessage());
                }
            }

            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException ioException) {
                    logger.error(ioException.getMessage());
                }
            }

            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ioException) {
                    logger.error(ioException.getMessage());
                }
            }
        }
    }

    /**
     * Add port permission using user id.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean addPortPerm(String id) {
        try {
            if (FileTools.checkFileExistsCurPath("configs/allowed2port.json")) {
                ArrayList arrayList = JSONs.getArrayFromJSON("configs/allowed2port.json");
                if (arrayList != null) {
                    arrayList.add(id);
                }
                JSONs.writeArrayToJSON(arrayList, "configs/allowed2port.json");
            } else {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(id);
                JSONs.writeArrayToJSON(arrayList, "configs/allowed2port.json");
            }
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Common message for list/jurl2sgsi args
     */
    public void sendSupportedROMs(Update update, TelegramBot bot, PrefObj prefs) {
        File[] supportedSGSIsS = ArrayUtils.addAll(supportedSGSIs12);

        if (supportedSGSIsS != null) {
            bot.sendReply(prefs.getString("xgsi_supported_types")
                    .replace("%1",
                            Arrays.toString(supportedSGSIs12).replace(toolPath + "other/roms/", "")
                                    .replace("[", "")
                                    .replace("]", "")), update);
        } else {
            bot.sendReply(prefs.getString("xgsi_something_is_wrong"), update);
        }
    }
}
