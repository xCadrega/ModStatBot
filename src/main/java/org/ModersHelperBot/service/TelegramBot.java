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
            long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getChat().getFirstName();
            switch (message) {
                case "/start": {
                    sendMessage(chatId, "Привет, " + firstName + "!");
                } default: {
                    if (update.getMessage().getText().contains("paste.mineland")) {
                        message += update.getMessage().getText() + "\n";
                        messagesCount++;
                        if (messagesCount == 2 || (messagesCount == 1 && message.split("\n").length > messagesCount)) {
                            new Url(chatId, message).urlsAndCommandsExtraction(message);
                            messagesCount = 0;
                            message = "";
                        }
                    } else {
                        sendMessage(chatId, "Я не вижу здесь ссылки, а вы?");
                    }
                }
            }
        }
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