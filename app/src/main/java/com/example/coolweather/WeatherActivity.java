package com.example.coolweather;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //初始化各控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if(weatherString != null){
            //如果有缓存则直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }else{
            //无缓存时去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_id");
            //把ScrollView暂时设为不可见
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

    }

    /*
    * 根据天气id请求城市天气信息    myKey=a561dc9f87b2451f9ddcd9774b1d76a0
    **/
    public void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid="
                + weatherId + "&key=a561dc9f87b2451f9ddcd9774b1d76a0";
        //注意：setOKHttpRequest中第一个参数是传入的weatherUrl，即地址
        HttpUtil.setOKHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather != null && "ok".equals(weather.status)){
                             SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败 in onResponse()", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败 in onFailure()", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 处理并展示Weather实体类中的数据
     * */
    public void showWeatherInfo(Weather weather){
        Log.d("WeatherActivity", "showWeatherInfo(Weather weather)");
       String cityName = weather.basic.cityName;
       String updateTime = weather.basic.update.updateTime.split(" ")[1];
       String degree = weather.now.temperature + "℃";
       String weatherInfo = weather.now.more.info;
       titleCity.setText(cityName);
       titleUpdateTime.setText(updateTime);
       degreeText.setText(degree);
       weatherInfoText.setText(weatherInfo);
       //先清除forecastLayout中的view
       forecastLayout.removeAllViews();
       for(Forecast forecast : weather.forecastList){
           //动态加载forecast_item
           View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);

           TextView dateText = (TextView) view.findViewById(R.id.date_text);
           TextView infoText = (TextView) view.findViewById(R.id.info_text);
           TextView maxText = (TextView) view.findViewById(R.id.max_text);
           TextView minText = (TextView) view.findViewById(R.id.min_text);
           dateText.setText(forecast.date);
           infoText.setText(forecast.more.info);
           maxText.setText(forecast.temperature.max);
           minText.setText(forecast.temperature.min);
           //加载
           forecastLayout.addView(view);
       }
       if(weather.aqi != null){
           aqiText.setText(weather.aqi.city.aqi);
           pm25Text.setText(weather.aqi.city.pm25);
       }
       comfortText.setText("舒适度：" + weather.suggestion.comfort.info);
       carWashText.setText("洗车指数：" + weather.suggestion.carwash.info);
       sportText.setText("运动建议：" + weather.suggestion.sport.info);
       //把ScrollView重新设置为可见
        weatherLayout.setVisibility(View.VISIBLE);
    }
}