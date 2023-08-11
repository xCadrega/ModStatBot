package org.ModersHelperBot.service;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class URL {
    private final long chatId;
    private String url;
    private final TelegramBot bot;

    public URL(long chatId, String url) {
        this.chatId = chatId;
        this.url = url;
        this.bot = new TelegramBot();
    }

    public void setURL(String anotherURL) {
        this.url = anotherURL;
    }

    public void urlProcessing() {
        String[] urls;
        if (url.length() > 50) {
            if (url.contains("\n")) {
                urls = checkCorrectURL(url, "\n");
                URLAndCommandExtraction(urls);
            } else if (url.contains(" ")) {
                urls = checkCorrectURL(url, " ");
                URLAndCommandExtraction(urls);
            }
        } else {
            linkExtraction(url);
        }
    }

    private void URLAndCommandExtraction(String[] urls) {
        urls[0] = urls[0].trim();
        if (urls.length > 1) {
            for (int i = 1; i < urls.length; i++) {
                if (!isValidLink(urls[i])) {
                    String fullogs = parseJson(urls[0]);
                    Logs logs = new Logs(fullogs);
                    List<String> allOccurrences = Arrays.asList(logs.occurrencesOfCommand(urls[i]).split("\\n"));
                    String logsForSend = "";
                    int logsCount = 0;
                    for (String occurrence : allOccurrences) {
                        if (logsCount == 24) {
                            bot.sendMessage(chatId, logsForSend);
                            logsForSend = "";
                            logsCount = 0;
                        }
                        logsForSend += occurrence + "\n";
                        logsCount++;
                    }
                    if (logsForSend.length() > 0) {
                        bot.sendMessage(chatId, logsForSend);
                    }
                } else {
                    linksExtraction(urls);
                    break;
                }
            }
        }
    }

    private String[] checkCorrectURL(String url, String regex) {
        String[] urls = url.split(regex);
        int length = urls.length;
        if (length > 1) {
            for (int index = 0; index < length; index++) {
                if (urls[index].startsWith("paste")) {
                    urls[index] = httpAdd(urls[index]);
                }
            }
        }
        return urls;
    }

    public String httpAdd(String url) {
        return "https://" + url;
    }

    public boolean isValidLink(String url) {
        return (url.startsWith("https://paste.mineland") ||
                url.startsWith("paste.mineland"));
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
                bot.sendMessage(chatId, "Страница пуста");
            } else {
                return allData;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    private void linksExtraction(String[] urls) {
        if (isValidLink(urls[1])) {
            String fullogs = parseJson(urls[0]);
            String mhistory = parseJson(urls[1]);
            collectStatistic(fullogs, mhistory);
        }
    }

    private void linkExtraction(String url) {
        if (url.startsWith("paste.mineland")) {
            url = httpAdd(url);
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
        if (reports == 0 && warns == 0) {
            reportsAndWarns = countingReportsAndWarns(mhistory);
            reports = Integer.parseInt(reportsAndWarns[1]);
            warns = Integer.parseInt(reportsAndWarns[2]);
        }
        int bans = Integer.parseInt(bansAndMutes[1]);
        int mutes = Integer.parseInt(bansAndMutes[2]);
        if (bans == 0 && mutes == 0) {
            bansAndMutes = countingBansAndMutes(fullogs);
            bans = Integer.parseInt(bansAndMutes[1]);
            mutes = Integer.parseInt(bansAndMutes[2]);
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
        String nickname = logs.getNickname();
        int bans = logs.countOfBans();
        int mutes = logs.countOfMutes();
        return new String[] {nickname, String.valueOf(bans), String.valueOf(mutes)};
    }

    private void sendStatistic(String nickname, int bans, int mutes, int reports, int warns) {
        String statistic = "Статистика " + nickname + "\n" +
                (reports != 0 ? "Репорты: " + reports + "\n" : "Нет разобранных репортов\n") +
                (warns != 0 ? "Варны: " + warns + "\n" : "Нет выданных варнов\n") +
                (bans != 0 ? "Баны: " + bans + "\n" : "Нет выданных банов\n") +
                (mutes != 0 ? "Муты: " + mutes + "\n" : "Нет выданных мутов\n");
        bot.sendMessage(chatId, statistic);
    }
}
