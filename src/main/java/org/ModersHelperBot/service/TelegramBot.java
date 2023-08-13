package org.ModersHelperBot.service;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {
    private static TelegramBot TELEGRAM_BOT = null;

    public TelegramBot() {
        TELEGRAM_BOT = this;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
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
                    URL url = new URL(chatId, message);
                    if (url.isValidUrl(message)) {
                        url.urlProcessing();
                    } else if (message.contains("paste.mineland")) {
                        url.urlsExtraction(message);
                    } else {
                        String answer = "Я не вижу здесь ссылки, а вы?";
                        sendMessage(chatId, answer);
                    }
                }
            }
        }
    }

    private static void startCommandReceived(long chatId, String firstName) throws TelegramApiException {
        String answer = "Привет, " + firstName + "!";
        sendMessage(chatId, answer);
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
        return "token";
    }

    @Override
    public String getBotUsername() {
        return "username";
    }
}
