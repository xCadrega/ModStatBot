package org.ModersHelperBot.service;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String userMessage = update.getMessage().getText();
            String firstName = update.getMessage().getChat().getFirstName();
            switch (userMessage) {
                case "/start": {
                    sendMessage(chatId, "Привет, " + firstName + "!");
                    break;
                } default: {
                    if (update.getMessage().getText().contains("paste.mineland")) {
                        message += "\n" + update.getMessage().getText();
                        messagesCount++;
                        messagesHandling(chatId);
                    } else {
                        sendMessage(chatId, "Я не вижу здесь ссылки, а вы?");
                    }
                }
            }
        }
    }

    private void messagesHandling(long chatId) {
        if (messagesCount == 1 && message.split("\n").length > 2) {
            new Url(chatId, message).urlsAndCommandsExtraction(message);
            messagesCount = 0;
            message = "";
        } else {
            List<String> messages = new ArrayList<>(Arrays.asList(message.split("\n")));
            messages.removeIf(String::isEmpty);
            Pattern pattern = Pattern.compile("(?<=\\s)[^:\\s]+(?=:)");
            Matcher matcher = pattern.matcher(messages.get(0));
            String nickname = "";
            if (matcher.find()) {
                nickname = matcher.group();
            }
            for (int i = 1; i <= messages.size() - 1; i++) {
                if (messages.get(i).contains(nickname)) {
                    String occurence = messages.get(i);
                    String messageToSend = messages.get(0) + "\n" + occurence;
                    messages.remove(0);
                    messages.remove(occurence);
                    new Url(chatId, messageToSend).urlsAndCommandsExtraction(messageToSend);
                    message = String.join("\n", messages);
                    if (messages.size() >= 2) {
                        messagesHandling(chatId);
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