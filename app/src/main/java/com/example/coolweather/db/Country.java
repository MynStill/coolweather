package com.example.coolweather.db;

import org.litepal.crud.LitePalSupport;

public class Country extends LitePalSupport {
    private int id;

    private String countryName;

    private int countryCode;

    private int cityId;

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public String getCountryName(){
        return countryName;
    }

    public void setCountryName(String countryName){
        this.countryName = countryName;
    }

    public int getCountryCode(){
        return countryCode;
    }

    public void setCountryCode(int countryCode){
        this.countryCode = countryCode;
    }

    public int getCityId(){
        return cityId;
    }

    public void setCityId(int cityId){
        this.cityId = cityId;
    }
}
