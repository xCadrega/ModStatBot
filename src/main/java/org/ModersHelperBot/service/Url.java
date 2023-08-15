package org.ModersHelperBot.service;

import com.google.gson.Gson;

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
    private String url;
    private String commands;

    public Url(long chatId, String url) {
        this.chatId = chatId;
        this.url = url;
        this.commands = "";
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCommands() {
        return commands;
    }

    public void setCommands(String commands) {
        this.commands = commands;
    }

    public void urlsAndCommandsExtraction(String message) {
        String urls = "";
        String command = "";
        Pattern pattern = Pattern.compile("(paste.mineland\\.\\S+)|(https?://paste.mineland\\.\\S+)");
        Matcher matcher = pattern.matcher(message);
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
        command += "\n" + message.substring(start);
        List<String> allCommands = new ArrayList<>(Arrays.asList(command.split("\n")));
        allCommands.removeIf(userCommand -> userCommand.isEmpty() || userCommand.contains("Держи логи игрока"));
        setUrl(urls);
        setCommands(String.join("\n", allCommands));
        urlProcessing();
    }

    public void urlProcessing() {
        String[] urls = correctUrls(getUrl());
        int length = urls.length;
        if (length == 1 && commands.isEmpty()) {
            collectStatistic(parseJson(urls[0]), parseJson(urls[0]));
        } else if (length == 2 && commands.isEmpty()) {
            collectStatistic(parseJson(urls[0]), parseJson(urls[1]));
        } else if (length % 2 == 0 && commands.isEmpty()) {
            for (int urlCount = 0; urlCount < length; urlCount++) {
                collectStatistic(parseJson(urls[urlCount]), parseJson(urls[++urlCount]));
            }
        } else {
            logsWithCommandsExtraction(urls);
        }
    }

    private void logsWithCommandsExtraction(String[] urls) {
        String[] userCommands = getCommands().split("\n");
        for (String userCommand : userCommands) {
            String logsForSend = "";
            int logsCount = 0;
            for (String url : urls) {
                String fullLogs = parseJson(url);
                Logs logs = new Logs(fullLogs);
                String nickname = logs.getNickname();
                logsForSend += "Найденные вхождения команд " + nickname + ":\n\n";
                String[] allOccurrences = logs.getOccurrencesOfCommand(userCommand).split("\\n");
                for (String occurrence : allOccurrences) {
                    if (logsCount == 24) {
                        TelegramBot.sendMessage(chatId, logsForSend);
                        logsForSend = "";
                        logsCount = 0;
                    }
                    logsForSend += occurrence + "\n";
                    logsCount++;
                }
            }
            if (logsForSend.length() > 0) {
                TelegramBot.sendMessage(chatId, logsForSend);
            }
        }
    }

    private String[] correctUrls(String url) {
        String[] urls = url.split("\n");
        int length = urls.length;
        for (int index = 0; index < length; index++) {
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
                String allData = data.getData();
                if (allData == null) {
                    TelegramBot.sendMessage(chatId, "Страница пуста");
                } else {
                    return allData;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }


    private void collectStatistic(String fullogs, String mhistory) {
        String[] reportsAndWarns = countingReportsAndWarns(fullogs);
        String[] bansAndMutes = countingBansAndMutes(mhistory);
        String nickname = reportsAndWarns[0];
        int reports = Integer.parseInt(reportsAndWarns[1]);
        int warns = Integer.parseInt(reportsAndWarns[2]);
        int bans = Integer.parseInt(bansAndMutes[0]);
        int mutes = Integer.parseInt(bansAndMutes[1]);
        // если был отправлен mhistory, в котором хранятся муты и баны
        if (reports == 0 && warns == 0) {
            reportsAndWarns = countingReportsAndWarns(mhistory);
            reports = Integer.parseInt(reportsAndWarns[1]);
            warns = Integer.parseInt(reportsAndWarns[2]);
        }
        // если был отправлен fullogs, в котором хранятся репорты и варны
        if (bans == 0 && mutes == 0) {
            bansAndMutes = countingBansAndMutes(fullogs);
            bans = Integer.parseInt(bansAndMutes[0]);
            mutes = Integer.parseInt(bansAndMutes[1]);
        }
        sendStatistic(nickname, bans, mutes, reports, warns);
    }

    private String[] countingReportsAndWarns(String fullogs) {
        Logs logs = new Logs(fullogs);
        String nickname = logs.getNickname();
        int reports = logs.countOfReports();
        int warns = logs.countOfWarns();
        return new String[] {nickname, String.valueOf(reports), String.valueOf(warns)};
    }

    private String[] countingBansAndMutes(String mhistory) {
        Logs logs = new Logs(mhistory);
        int bans = logs.countOfBans();
        int mutes = logs.countOfMutes();
        return new String[] {String.valueOf(bans), String.valueOf(mutes)};
    }

    private void sendStatistic(String nickname, int bans, int mutes, int reports, int warns) {
        String statistic = "Статистика " + nickname + "\n" +
                (reports != 0 ? "Репорты: " + reports + "\n" : "Нет разобранных репортов\n") +
                (warns != 0 ? "Варны: " + warns + "\n" : "Нет выданных варнов\n") +
                (bans != 0 ? "Баны: " + bans + "\n" : "Нет выданных банов\n") +
                (mutes != 0 ? "Муты: " + mutes + "\n" : "Нет выданных мутов\n");
        TelegramBot.sendMessage(chatId, statistic);
    }
}