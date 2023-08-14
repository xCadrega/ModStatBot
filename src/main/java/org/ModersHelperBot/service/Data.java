package org.ModersHelperBot.service;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

public class Data {
    @SerializedName("data")
    @Getter private String data;

    public Data(String data) {
        this.data = data;
    }
}