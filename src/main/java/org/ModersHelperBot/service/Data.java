package org.ModersHelperBot.service;

import com.google.gson.annotations.SerializedName;

public class Data {
    @SerializedName("data")
    private String logData;

    public String getData() {
        return logData;
    }

}
