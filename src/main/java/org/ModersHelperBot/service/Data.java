package org.ModersHelperBot.service;

import com.google.gson.annotations.SerializedName;

public class Data {
    @SerializedName("data")
    private String data;

    public Data(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}