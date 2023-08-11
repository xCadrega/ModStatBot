package org.ModersHelperBot.service;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramBot extends TelegramLongPollingBot {
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getChat().getFirstName();
            switch (messageText) {
                case "/start" -> {
                    try {
                        startCommandReceived(chatId, firstName);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
                default -> {
                    URL url = new URL(chatId, messageText);
                    if (url.isValidLink(messageText)) {
                        url.urlProcessing();
                    } else if (messageText.startsWith("Держи логи игрока")) {
                        String urls = "";
                        String regexPattern = "(https?://\\S+)";
                        Pattern pattern = Pattern.compile(regexPattern);
                        Matcher matcher = pattern.matcher(messageText);
                        int start = 0;
                        while (matcher.find(start)) {
                            String foundedUrl = matcher.group();
                            if (start == 0) {
                                urls += foundedUrl;
                            } else {
                                urls += "\n" + foundedUrl;
                            }
                            start = matcher.end();
                        }
                        url.setURL(urls);
                        url.urlProcessing();
                    } else {
                            String answer = "Я не вижу здесь ссылки, а вы?";
                            sendMessage(chatId, answer);
                        }
                }
            }
        }
    }

    private void startCommandReceived(long chatId, String firstName) throws TelegramApiException {
        String answer = "Привет, " + firstName + "!";
        sendMessage(chatId, answer);
    }

    public void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String getBotToken() {
        return "6517224899:AAF5YK650WJfp8mqpzsuYGOb7iXLzuQvUAY";
    }

    @Override
    public String getBotUsername() {
        return "polygon_for_tests_bot";
    }
}
