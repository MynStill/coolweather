package com.example.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.db.City;
import com.example.coolweather.db.Country;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTRY = 2;

    private ProgressDialog progressDialog;

    private TextView textView;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     * */
    private List<Province> provinceList;

    /**
     * 市列表
     * */
    private List<City> cityList;

    /**
     * 县列表
     * */

    private List<Country> countryList;

    /**
     * 选中的省
     * */
    private Province selectedProvince;

    /**
     * 选中的市
     * */
    private City selectedCity;

    /**
     * 选中的当前的级别
     * */
    private int currentLevel;

    @Override
    public View onCreateView( LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        textView = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    //当前为省，点击进入市
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    //当前为市，点击进入县
                    selectedCity = cityList.get(position);
                    queryCountries();
                }else if(currentLevel == LEVEL_COUNTRY){
                    //当前为县，点击进入天气界面(活动)，并且关掉当前界面
                    String weatherId = countryList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    getActivity().finish();

                    //testActivity --->ok
                    /*Intent intent = new Intent(getActivity(), TestActivity.class);
                    startActivity(intent);*/
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTRY){
                    //当前为县，点击返回县
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    //当前为市，点击返回省
                    queryProvinces();
                }
            }
        });
        queryProvinces();//一开始就要初始化省的数据
    }

    /**
     * 查询全国所有的省，优先从数据库中查找，如果没有查询到再到服务器中查询
     * */
    private void queryProvinces(){
        textView.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if(provinceList.size() > 0){
            dataList.clear();
            for(Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged(); //实现更新
            listView.setSelection(0);   //从第0个item开始显示
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询全国所有的市，优先从数据库中查找，如果没有查询到再到服务器中查询
     * */
    private void queryCities(){
        textView.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        //在选中的省里面，获取所有市的数据
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0){
            dataList.clear();
            for(City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询全国所有的县，优先从数据库中查找，如果没有查询到再到服务器中查询
     * */
    private void queryCountries(){
        textView.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        //在选中市中获取所有县的数据
        countryList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(Country.class);
        if(countryList.size() > 0){
            dataList.clear();
            for(Country country : countryList){
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTRY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "country");
        }
    }

    /**
     * 根据传入的地址和类型从服务器中查询省市县数据
     * */
    private void queryFromServer(String address, final String type){
        showProgressDialog();
        HttpUtil.setOKHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    //把省数据写进数据库中
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    //把市数据写进数据库中
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                }else if("country".equals(type)){
                    //把县数据写进数据库中
                    result = Utility.handleCountryResponse(responseText, selectedCity.getId());
                }
                //写进数据库中之后，再次调用上面的函数来重新加载数据，由于牵扯到UI操作，借助runOnUiThread实现
                if(result){
                    getActivity().runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("country".equals(type)){
                                queryCountries();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
    }

    /**
     * 显示进度对话框
     * */
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     * */
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
