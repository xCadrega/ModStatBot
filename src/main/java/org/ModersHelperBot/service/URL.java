package org.ModersHelperBot.service;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URL {
    private final long chatId;
    private String url;

    public URL(long chatId, String url) {
        this.chatId = chatId;
        this.url = url;
    }

    private void setUrl(String anotherUrl) {
        this.url = anotherUrl;
    }

    public void urlsExtraction(String message) {
        String urls = "";
        Pattern pattern = Pattern.compile("(https?://\\S+)");
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
        setUrl(urls);
        urlProcessing();
    }

    public boolean isValidUrl(String url) {
        return (url.startsWith("https://paste.mineland") ||
                url.startsWith("paste.mineland"));
    }

    public void urlProcessing() {
        String[] urls;
        if (url.length() > 42) {
            if (url.contains("\n")) {
                urls = correctUrls(url, "\n");
                urlAndCommandExtraction(urls);
            } else if (url.contains(" ")) {
                urls = correctUrls(url, " ");
                urlAndCommandExtraction(urls);
            }
        } else {
            extractionLogsFromUrl(url);
        }
    }

    private void urlAndCommandExtraction(String[] urls) {
        urls[0] = urls[0].trim();
        if (urls.length > 1) {
            for (int i = 1; i < urls.length; i++) {
                if (!isValidUrl(urls[i])) {
                    String fullogs = parseJson(urls[0]);
                    Logs logs = new Logs(fullogs);
                    String[] allOccurrences = logs.getOccurrencesOfCommand(urls[i]).split("\\n");
                    String logsForSend = "";
                    int logsCount = 0;
                    for (String occurrence : allOccurrences) {
                        if (logsCount == 24) {
                            TelegramBot.sendMessage(chatId, logsForSend);
                            logsForSend = "";
                            logsCount = 0;
                        }
                        logsForSend += occurrence + "\n";
                        logsCount++;
                    }
                    if (logsForSend.length() > 0) {
                        TelegramBot.sendMessage(chatId, logsForSend);
                    }
                } else {
                    extractionLogsFromUrls(urls);
                    break;
                }
            }
        }
    }

    private String[] correctUrls(String url, String regex) {
        String[] urls = url.split(regex);
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
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            Gson gson = new Gson();
            Data data = gson.fromJson(reader, Data.class);
            String allData = data.getData();
            if (allData == null) {
                TelegramBot.sendMessage(chatId, "Страница пуста");
            } else {
                return allData;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    private void extractionLogsFromUrls(String[] urls) {
        if (isValidUrl(urls[1])) {
            String fullogs = parseJson(urls[0]);
            String mhistory = parseJson(urls[1]);
            collectStatistic(fullogs, mhistory);
        }
    }

    private void extractionLogsFromUrl(String url) {
        if (url.startsWith("paste.mineland")) {
            url = "https://" + url;
        }
        String fullogs = parseJson(url);
        collectStatistic(fullogs, fullogs);
    }

    private void collectStatistic(String fullogs, String mhistory) {
        String[] reportsAndWarns = countingReportsAndWarns(fullogs);
        String[] bansAndMutes = countingBansAndMutes(mhistory);
        String nickname = reportsAndWarns[0];
        int reports = Integer.parseInt(reportsAndWarns[1]);
        int warns = Integer.parseInt(reportsAndWarns[2]);
        // если был отправлен mhistory, в котором хранятся муты и баны
        if (reports == 0 && warns == 0) {
            reportsAndWarns = countingReportsAndWarns(mhistory);
            reports = Integer.parseInt(reportsAndWarns[1]);
            warns = Integer.parseInt(reportsAndWarns[2]);
        }
        int bans = Integer.parseInt(bansAndMutes[0]);
        int mutes = Integer.parseInt(bansAndMutes[1]);
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