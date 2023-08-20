package org.ModersHelperBot.service;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;

public class TelegramBot extends TelegramLongPollingBot {
    private static @NotNull TelegramBot TELEGRAM_BOT;
    private String message;
    private int messagesCount;

    public TelegramBot() {
        TELEGRAM_BOT = this;
        this.message = "";
        this.messagesCount = 0;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            message += update.getMessage().getText() + "\n";
            messagesCount++;
            long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getChat().getFirstName();
            switch (message) {
                case "/start": {
                    try {
                        startCommandReceived(chatId, firstName);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } default: {
                    if (messagesCount == 2 || (messagesCount == 1 && message.split("\n").length > messagesCount)) {
                        messagesCount = 0;
                        new Url(chatId, message).urlsAndCommandsExtraction(message);
                        message = "";
                    } else {
                        sendMessage(chatId, "Я не вижу здесь ссылки, а вы?");
                    }
                }
            }
        }
    }

    private static void startCommandReceived(long chatId, String firstName) throws TelegramApiException {
        sendMessage(chatId, "Привет, " + firstName + "!");
    }

    public static void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            TELEGRAM_BOT.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String getBotToken() {
        return "6587926634:AAGPNBJqewwxR4xe85vlRpXW1O3Rx3xS0Ms";
    }

    @Override
    public String getBotUsername() {
        return "ModersHelperBot";
    }
}
