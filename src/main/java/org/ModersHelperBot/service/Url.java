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
    @Getter @Setter private String url;
    @Getter @Setter private String commands;

    public Url(String url) {
        this.url = url;
        this.commands = "";
    }

    public void urlsAndCommandsExtraction() {
        String urls = "";
        Pattern pattern = Pattern.compile("(paste.mineland\\.\\S+)");
        Matcher matcher = pattern.matcher(getUrl());
        int start = 0;
        while (matcher.find(start)) {
            String foundedUrl = matcher.group();
            urls += foundedUrl + "\n";
            start = matcher.end();
        }
        String command = "\n" + getUrl().substring(start);
        List<String> allCommands = new ArrayList<>(Arrays.asList(command.split("\n")));
        allCommands.removeIf(userCommand -> userCommand.isEmpty() || userCommand.contains("Держи логи игрока")
                || userCommand.contains("Держи историю выданных наказаний игрока"));
        setUrl(urls);
        setCommands(String.join("\n", allCommands));
        handleUrls();
    }

    private void handleUrls() {
        String[] urls = urlsSplitAndFormat(getUrl());
        if (urls.length % 2 == 0 && commands.isEmpty()) {
            for (int urlCount = 0; urlCount < urls.length - 1; urlCount++) {
                new Statistic(parseJson(urls[urlCount]), parseJson(urls[++urlCount]));
            }
        } else {
            String[] userCommands = getCommands().split("\n");
            for (String url : urls) {
                new Logs(parseJson(url)).logsWithCommandsExtraction(userCommands);
            }
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

    public static String parseJson(String url) {
        Data data = null;
        if (url.contains(".me")) {
            url = url.substring(0, 26) + "documents/" + url.substring(26);
        } else if (url.contains(".net")) {
            url = url.substring(0, 27) + "documents/" + url.substring(27);
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                data = new Gson().fromJson(reader, Data.class);
                return data.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data != null ? data.getData() : "";
    }
}