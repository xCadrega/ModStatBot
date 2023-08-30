package org.ModersHelperBot.service;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramBot extends TelegramLongPollingBot {
    private static TelegramBot TELEGRAM_BOT = null;
    private static long chatId;
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
            String userCommand = update.getMessage().getText();
            chatId = update.getMessage().getChatId();
            switch (userCommand) {
                case "/start" -> sendMessage("Привет, " + update.getMessage().getChat().getFirstName() + "!");
                default -> {
                    if (userCommand.contains("paste.mineland")) {
                        message += "\n" + update.getMessage().getText();
                        messagesCount++;
                        handleMessages();
                    } else {
                        sendMessage("Я не вижу здесь ссылки, а вы?");
                    }
                }
            }
        }
    }

    private void handleMessages() {
        if (messagesCount == 1 && message.split("\n").length > 2) {
            processOneMessage();
        } else {
            List<String> messages = new ArrayList<>(Arrays.asList(message.split("\n")));
            messages.removeIf(String::isEmpty);
            String nickname = extractNickname(messages.get(0));
            for (int index = 1; index <= messages.size() - 1; index++) {
                if (messages.get(index).contains(nickname)) {
                    processTwoMessages(messages, index);
                }
            }
        }
    }

    private String extractNickname(String firstMessageLine) {
        Pattern pattern = Pattern.compile("(?<=\\s)[^:\\s]+(?=:)");
        Matcher matcher = pattern.matcher(firstMessageLine);
        return matcher.find() ? matcher.group() : "";
    }

    private void processOneMessage() {
        new Url(message).urlsAndCommandsExtraction();
        messagesCount = 0;
        message = "";
    }

    private void processTwoMessages(List<String> messages, int index) {
        String foundedMessage = messages.get(index);
        String messageToSend = messages.get(0) + "\n" + foundedMessage;
        messages.remove(0);
        messages.remove(foundedMessage);
        new Url(messageToSend).urlsAndCommandsExtraction();
        message = String.join("\n", messages);
        messagesCount -= 2;
        if (messages.size() >= 2) {
            handleMessages();
        }
    }

    public static void sendMessage(String textToSend) {
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