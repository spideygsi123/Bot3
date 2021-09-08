package com.vegazsdev.bobobot.commands.info;

import com.vegazsdev.bobobot.TelegramBot;
import com.vegazsdev.bobobot.core.command.Command;
import com.vegazsdev.bobobot.db.PrefObj;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Start extends Command {

    public Start() {
        super("start");
    }

    @Override
    public void botReply(Update update, TelegramBot bot, PrefObj prefs) {
        /*
         * Prepare SendMessage
         */
        SendMessage message = new SendMessage();
        message.setDisableWebPagePreview(true);
        message.enableHtml(true);
        message.setChatId(update.getMessage().getChatId().toString());
        message.setText(prefs.getString("start"));
        if (update.getMessage().getReplyToMessage() != null) message.setReplyToMessageId(update.getMessage().getReplyToMessage().getMessageId());

        /*
         * Prepare InlineKeyboardButton
         */
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        /*
         * Prepare InlineKeyboardButton: Source code
         */
        List<InlineKeyboardButton> ListInlineKeyboardButtonSourceCode = new ArrayList<>();
        InlineKeyboardButton ListInlineKeyboardButton = new InlineKeyboardButton();
        ListInlineKeyboardButton.setText(prefs.getString("start_source_code"));
        ListInlineKeyboardButton.setUrl("https://github.com/TrebleExperience/Bot3");
        ListInlineKeyboardButtonSourceCode.add(ListInlineKeyboardButton);
        rowsInline.add(ListInlineKeyboardButtonSourceCode);

        /*
         * Prepare InlineKeyboardButton: TrebleExperience Channel
         */
        List<InlineKeyboardButton> ListInlineKeyboardButtonChannel = new ArrayList<>();
        InlineKeyboardButton ListInlineKeyboardButtonChannelBTN = new InlineKeyboardButton();
        ListInlineKeyboardButtonChannelBTN.setText(prefs.getString("start_rk137gsi"));
        ListInlineKeyboardButtonChannelBTN.setUrl("https://t.me/rk137gsi");
        ListInlineKeyboardButtonChannel.add(ListInlineKeyboardButtonChannelBTN);
        rowsInline.add(ListInlineKeyboardButtonChannel);

        /*
         * Finish InlineKeyboardButton setup
         */
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        /*
         * Send the message
         */
        bot.sendMessageAsyncBase(message, update);
    }
}
