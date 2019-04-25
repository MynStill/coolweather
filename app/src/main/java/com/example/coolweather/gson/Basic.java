package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;    //int err?

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
