package org.ModersHelperBot.service;

import lombok.Getter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Logs {
    @Getter private final String nickname;
    private final List<String> logs;
    private final int year;
    private final String[] dates;

    public Logs(String data) {
        this.logs = Arrays.asList(data.split("\\n"));
        this.nickname = readNickname();
        this.year = LocalDate.now().getYear();
        this.dates = new String[7];
        fillDays();
    }

    private void fillDays() {
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        LocalDate endOfWeek = LocalDate.now().minusDays(dayOfWeek + 1);
        LocalDate startOfWeek = endOfWeek.minusDays(6);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM");
        for (int index = 0; index < 7; index++) {
            dates[index] = startOfWeek.format(dateFormatter);
            startOfWeek = startOfWeek.plusDays(1);
        }
    }

    private String readNickname() {
        return logs.stream()
                .map(log -> log.split(" "))
                .findFirst()
                .map(elements -> elements.length >= 3 ? elements[3] : "")
                .orElse("");
    }

    public int countOfWarns() {
        int count = 0;
        for (String date : dates) {
            count += (int) logs.stream()
                    .filter(log -> (log.contains(date) && log.contains("Answer to")))
                    .count();
        }
        return count;
    }

    public int countOfReports() {
        int count = 0;
        for (String date : dates) {
            count += (int) logs.stream()
                    .filter(log -> (log.contains(date) && (log.contains("Accepted report from")
                            || log.contains("Denied report from")
                            || log.contains("Replied to question"))))
                    .count();
        }
        return count;
    }

    public int countOfBans() {
        int banCount = 0;
        int multiaccountBanCount = 0;
        for (String date : dates) {
            banCount += (int) logs.stream()
                    .filter(log -> (log.contains(date + "." + year) && log.contains("Бан игроку")))
                    .count();
            banCount -= (int) logs.stream()
                    .filter(log -> (log.contains(date + "." + year) && log.contains("Бан игроку") && log.contains("3.1 -")))
                    .count();
            multiaccountBanCount += (int) logs.stream()
                    .filter(log -> (log.contains(date + "." + year) && log.contains("1.2 -")))
                    .count();
        }
        banCount -= multiaccountBanCount;
        banCount += multiaccountBanCount / 3;
        return banCount;
    }

    public int countOfMutes() {
        int count = 0;
        for (String date : dates) {
            count += (int) logs.stream()
                    .filter(log -> (log.contains(date + "." + year) && log.contains("Мут игроку")))
                    .count();
            count -= (int) logs.stream()
                    .filter(log -> (log.contains(date + "." + year) && log.contains("Мут игроку admin")))
                    .count();
        }
        return count;
    }

    public String findPlayersWithRemovedPunish(String removalPunishCommand) {
        List<String> notPunished = new ArrayList<>();
        Pattern pattern = Pattern.compile(removalPunishCommand + "(\\S+)");
        for (String date : dates) {
            for (int index = 0; index < logs.size(); index++) {
                Matcher matcher = pattern.matcher(logs.get(index));
                if (matcher.find() && logs.get(index).contains(date)) {
                    String player = matcher.group(1);
                    if (notRepunished(player, removalPunishCommand, index)) {
                        notPunished.add(player);
                    }
                }
            }
        }
        return String.join(", ", notPunished);
    }

    private boolean notRepunished(String player, String removalPunishCommand, int index) {
        for (int logToCheck = 1; logToCheck < 9 && index + logToCheck <= logs.size(); logToCheck++) {
            if (logs.get(index + logToCheck).contains("/punish " + player)
                    || logs.get(index + logToCheck).contains(removalPunishCommand.charAt(0)
                    + removalPunishCommand.substring(3) + player)) {
                return false;
            }
        }
        return true;
    }

    public String getOccurrencesOfCommand(String command) {
        return logs.stream()
                .filter(log -> log.contains(command))
                .collect(Collectors.joining("\n"));
    }
}