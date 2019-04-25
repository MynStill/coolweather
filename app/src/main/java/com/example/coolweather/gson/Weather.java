package com.example.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;


/**
 * 总的天气实体类用来引用其他所有的各个实体类
 * */
public class Weather {
    public String status;

    public Basic basic;

    public AQI aqi;

    public Now now;

    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}
