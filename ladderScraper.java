/*
Author: serverlinkdev@gmail.com
License: GPL version 3.0
Date: 09/10/2020
 */

package com.myBot.tia;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
from nolja
some maybe useful commands:
        .stat <name> -> returns rating, win, loss
        .stat_leaders  -> returns top N rated players
*/

public class ladderScraper {

    private int cursor = 0;
    private int longest = 0;
    private StringBuilder columnNames;
    private ArrayList<Integer> idxOfBar;
    private List<String> playersList;
    private String sendMe = null;
    private int widthOfTable = 0;

    public String fetch() {

        columnNames = new StringBuilder();
        playersList = new ArrayList<>();
        Document doc = null;

        // save the entire page into a variable called doc
        try {
            doc = Jsoup.connect("https://jointheladder.com/ladders/?mode=viewladder&lid=2").get();
            List<String> combined = new ArrayList<String>();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // The site has only one table, so we get that table and then ALL rows
        // into a list.
        Elements rows = doc.select("table").select("tr");

        // find longest player name, helps us to auto adjust player name field
        int longest = findLongestName(rows);

        createTableHeaderRow();
        widthOfTable = columnNames.toString().trim().length();
        findIndexesOfBarsInHeaderRow();

        for (int i = 1; i < rows.size(); i++) // skip col headers in first row
        {
            // A string we'll build up with all their data and bars to separate
            StringBuilder playerSB = new StringBuilder();
            // This is a row of data for some player
            Element row = rows.get(i);
            // This is a list of the data for the player
            Elements cols = row.select("td");

            // Get each of the player data we care about into a variable
            String name = cols.get(2).text().trim();
            String rating = cols.get(3).text();
            String wins = cols.get(5).text();
            String losses = cols.get(6).text();

            // add the player name
            playerSB.append("| " + name); // left justify names
            cursor = (longest + 3) - playerSB.length(); // 2 padding 1 bar = 3
            for (int j = 0; j < cursor; j++) playerSB.append(" "); // fill with spaces
            playerSB.append("|");

            // add the rest of players data to table
            addDataToRowAndJustify(idxOfBar.get(1), rating, playerSB);
            addDataToRowAndJustify(idxOfBar.get(2), wins, playerSB);
            addDataToRowAndJustify(idxOfBar.get(3), losses, playerSB);

            playersList.add(playerSB.toString()); // formatted row per user
        }

        generateDiscordResponse();
        return sendMe;
    }

    void addDataToRowAndJustify(int idxOfBar,
                                String someValue,
                                StringBuilder sb) {
        cursor = sb.toString().length();
        int midpoint = (idxOfBar - cursor) / 2;
        int padUntil = cursor + midpoint - (someValue.length() / 2);
        for (int i = cursor; i < padUntil; i++) sb.append(" ");
        sb.append(someValue);
        cursor = sb.toString().length();
        for (int j = cursor; j < idxOfBar; j++) sb.append(" ");
        sb.append("|");
    }

    void createTableHeaderRow() {
        columnNames.append("|");
        // dynamically place word Player in middle of the longest named player
        // this will auto adjust the width of our entire table
        int mid = 3; // half way of Player text
        cursor = ((longest + 2) / 2) - mid; // +2 is padding ea side of longest players name
        for (int j = 0; j < cursor; j++)
            columnNames.append(" "); // fill with spaces
        columnNames.append("Player");

        cursor = (longest + 3) - columnNames.length(); // 2 is padding & 1 for bar
        for (int j = 0; j < cursor; j++)
            columnNames.append(" "); // fill with spaces
        columnNames.append("| rating | wins | losses |\n");
    }

    void findIndexesOfBarsInHeaderRow() {
        idxOfBar = new ArrayList<>();
        int index = 1;
        while (index != -1) {
            index = columnNames.indexOf("|", index + 1);
            if (index != -1) idxOfBar.add(index);
        }
    }

    int findLongestName(Elements rows) {
        int len = 0;
        for (int i = 1; i < rows.size(); i++) // skip col headers in first row
        {
            Element row = rows.get(i);
            Elements cols = row.select("td");
            String name = cols.get(2).text().trim();
            len = name.length();
            longest = longest > len ? longest : len;
            len = 0;
        }
        // in case all users had less than 6 chars in names, we go to 6
        // in essence this forces longest to be a min of 6 in length.
        longest = longest > 6 ? longest : 6;
        return longest;
    }

    void generateDiscordResponse() {
        StringBuilder response= new StringBuilder();
        response.append("`\n");

        StringBuilder dashedLine = new StringBuilder();
        for (int i=0; i<widthOfTable; i++) dashedLine.append("-");

        response.append(dashedLine.toString() + "\n");
        response.append(columnNames.toString());
        response.append(dashedLine.toString() + "\n");

        for (int i = 0; i < playersList.size(); i++) {
            response.append(playersList.get(i) + "\n");
        }
        response.append(dashedLine.toString() + "`\n");
        sendMe = response.toString();
    }

}
