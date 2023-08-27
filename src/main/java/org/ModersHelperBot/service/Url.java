package org.ModersHelperBot.service;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Url {
    private final long chatId;
    @Getter @Setter private String url;
    @Getter @Setter private String commands;

    public Url(long chatId, String url) {
        this.chatId = chatId;
        this.url = url;
        this.commands = "";
    }

    public void urlsAndCommandsExtraction(String message) {
        String urls = "";
        Pattern pattern = Pattern.compile("(paste.mineland\\.\\S+)");
        Matcher matcher = pattern.matcher(message);
        int start = 0;
        while (matcher.find(start)) {
            String foundedUrl = matcher.group();
            urls += foundedUrl + "\n";
            start = matcher.end();
        }
        String command = "\n" + message.substring(start);
        List<String> allCommands = new ArrayList<>(Arrays.asList(command.split("\n")));
        allCommands.removeIf(userCommand -> userCommand.isEmpty() || userCommand.contains("Держи логи игрока")
                || userCommand.contains("Держи историю наказаний игрока"));
        setUrl(urls);
        setCommands(String.join("\n", allCommands));
        urlProcessing();
    }

    private void urlProcessing() {
        String[] urls = urlsSplitAndFormat(getUrl());
        int length = urls.length;
        if (length == 1 && commands.isEmpty()) {
            collectStatistic(parseJson(urls[0]), parseJson(urls[0]));
        } else if (length % 2 == 0 && commands.isEmpty()) {
            for (int urlCount = 0; urlCount < length; urlCount++) {
                collectStatistic(parseJson(urls[urlCount]), parseJson(urls[++urlCount]));
            }
        } else {
            logsWithCommandsExtraction(urls);
        }
    }

    private String[] urlsSplitAndFormat(String url) {
        String[] urls = url.split("\n");
        for (int index = 0; index < urls.length; index++) {
            if (urls[index].startsWith("paste")) {
                urls[index] = "https://" + urls[index];
            }
        }
        return urls;
    }

    private String parseJson(String url) {
        if (url.contains(".me")) {
            url = url.substring(0, 26) + "documents/" + url.substring(26);
        } else if (url.contains(".net")) {
            url = url.substring(0, 27) + "documents/" + url.substring(27);
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                Data data = new Gson().fromJson(reader, Data.class);
                return data.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    private void logsWithCommandsExtraction(String[] urls) {
        String[] userCommands = getCommands().split("\n");
        final int MAX_LOGS_TO_SEND = 24;
        for (String userCommand : userCommands) {
            String logsForSend = "";
            int logsCount = 0;
            if (userCommand.isEmpty()) {
                continue;
            }
            for (String url : urls) {
                Logs logs = new Logs(parseJson(url));
                logsForSend += "Найденные вхождения команд " + logs.getNickname() + ":\n\n";
                String[] allOccurrences = logs.getOccurrencesOfCommand(userCommand).split("\\n");
                for (String occurrence : allOccurrences) {
                    if (logsCount == MAX_LOGS_TO_SEND) {
                        TelegramBot.sendMessage(chatId, logsForSend);
                        logsForSend = "Найденные вхождения команд " + logs.getNickname() + ":\n\n";
                        logsCount = 0;
                    }
                    logsForSend += occurrence + "\n";
                    logsCount++;
                }
                if (logsForSend.split(" ").length == 4) {
                    logsForSend = "";
                }
            }
            if (logsForSend.length() > 0) {
                TelegramBot.sendMessage(chatId, logsForSend);
            }
        }
    }

    private void collectStatistic(String fullogs, String mhistory) {
        Logs logs = new Logs(fullogs);
        Logs history = new Logs(mhistory);
        String nickname = logs.getNickname();
        int reports = logs.countOfReports();
        int warns = logs.countOfWarns();
        int bans = history.countOfBans();
        int mutes = history.countOfMutes();
        String unbannedPlayers = logs.findPlayersWithRemovedPunish("/unban ");
        String unmutedPlayers = logs.findPlayersWithRemovedPunish("/unmute ");
        // если был отправлен mhistory, в котором хранятся муты и баны
        if (reports == 0 && warns == 0) {
            logs = new Logs(mhistory);
            reports = logs.countOfReports();
            warns = logs.countOfWarns();
        }
        // если был отправлен fullogs, в котором хранятся репорты и варны
        if (bans == 0 && mutes == 0) {
            history = new Logs(fullogs);
            bans = history.countOfBans();
            mutes = history.countOfMutes();
        }
        if (!nickname.isEmpty()) {
            sendStatistic(nickname, bans, mutes, reports, warns, unbannedPlayers, unmutedPlayers);
        } else {
            TelegramBot.sendMessage(chatId, "Страница пуста и/или указан некорректный адрес.\n");
        }
    }

    private void sendStatistic(String nickname, int bans, int mutes, int reports, int warns, String unbannedPlayers, String unmutedPlayers) {
        TelegramBot.sendMessage(chatId, "Статистика " + nickname + "\n" +
                (reports != 0 ? "Репорты: " + reports + "\n" : "Нет разобранных репортов\n") +
                (mutes != 0 ? "Муты: " + mutes + "\n" : "Нет выданных мутов\n") +
                (bans != 0 ? "Баны: " + bans + "\n" : "Нет выданных банов\n") +
                (warns != 0 ? "Варны: " + warns + "\n" : "Нет выданных варнов\n") +
                (!unbannedPlayers.isEmpty() ? "\nСписок разбаненных игроков:\n" + unbannedPlayers : "") +
                (!unmutedPlayers.isEmpty() ? "\nСписок размученных игроков:\n" + unmutedPlayers : ""));
    }
}