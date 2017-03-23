package com.coolweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.WeatherBean;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private AppCompatTextView mTitleCity;
    private AppCompatTextView mTvUpdatetime;
    private AppCompatTextView mTvDegree;
    private AppCompatTextView mTvWeatherinfo;
    private LinearLayout mLlForecast;
    private AppCompatTextView mTvAqi;
    private AppCompatTextView mTvPm25;
    private AppCompatTextView mTvComfort;
    private AppCompatTextView mTvCarwash;
    private AppCompatTextView mTvSport;
    private ScrollView mSvWeather;
    private AppCompatImageView mImgBingpic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //实现背景图和状态栏融合到一起
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);
        initView();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = preferences.getString("weather", null);
        String bing_pic = preferences.getString("bing_pic", null);
        if (bing_pic != null) {
            Glide.with(this).load(bing_pic).into(mImgBingpic);
        } else {
            loadBingPic();
        }
        if (weather != null) {
            //有缓存时直接解析数据
            WeatherBean weatherBean = Utility.handleWeatherResponse(weather);
            showWeatherInfo(weatherBean);
        } else {
            //无缓存时去服务器查询
            String weather_id = getIntent().getStringExtra("weather_id");
            mSvWeather.setVisibility(View.GONE);
            requestWeather(weather_id);
        }
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("bing_pic", bingPic);
                edit.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(mImgBingpic);
                    }
                });
            }
        });

    }

    /**
     * 根据天气id请求城市天气信息
     *
     * @param weatherId
     */
    private void requestWeather(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=18a4d147e1c64cab8fdbc03f1bc5c69e";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final WeatherBean weatherBean = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weatherBean != null && "ok".equals(weatherBean.getStatus())) {
                            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            edit.putString("weather", responseText);
                            edit.apply();
                            showWeatherInfo(weatherBean);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        });
        loadBingPic();
    }

    /**
     * 处理并展示Weather实体类中的数据
     *
     * @param weather
     */
    private void showWeatherInfo(WeatherBean weather) {
        String cityName = weather.getBasic().getCity();
        String updateTime = weather.getBasic().getUpdate().getLoc().split(" ")[1];
        String degree = weather.getNow().getTmp() + "℃";
        String weatherInfo = weather.getNow().getCond().getTxt();
        mTitleCity.setText(cityName);
        mTvUpdatetime.setText(updateTime);
        mTvDegree.setText(degree);
        mTvWeatherinfo.setText(weatherInfo);
        mLlForecast.removeAllViews();
        for (WeatherBean.DailyForecastBean forecastBean : weather.getDaily_forecast()) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mLlForecast, false);
            AppCompatTextView mTvDate = (AppCompatTextView) view.findViewById(R.id.tv_date);
            AppCompatTextView mTvInfo = (AppCompatTextView) view.findViewById(R.id.tv_info);
            AppCompatTextView mTvMax = (AppCompatTextView) view.findViewById(R.id.tv_max);
            AppCompatTextView mTvMin = (AppCompatTextView) view.findViewById(R.id.tv_min);
            mTvDate.setText(forecastBean.getDate());
            mTvInfo.setText(forecastBean.getCond().getTxt_d());
            mTvMax.setText(forecastBean.getTmp().getMax());
            mTvMin.setText(forecastBean.getTmp().getMin());
            mLlForecast.addView(view);
        }
        if (weather.getAqi() != null) {
            mTvAqi.setText(weather.getAqi().getCity().getAqi());
            mTvPm25.setText(weather.getAqi().getCity().getPm25());
        }
        String comfort = "舒适度：" + weather.getSuggestion().getComf().getTxt();
        String carWash = "汽车指数：" + weather.getSuggestion().getCw().getTxt();
        String sport = "运动建议：" + weather.getSuggestion().getSport().getTxt();
        mTvComfort.setText(comfort);
        mTvCarwash.setText(carWash);
        mTvSport.setText(sport);
        mSvWeather.setVisibility(View.VISIBLE);

    }

    /**
     * 初始化各控件
     */
    private void initView() {
        mTitleCity = (AppCompatTextView) findViewById(R.id.title_city);
        mTvUpdatetime = (AppCompatTextView) findViewById(R.id.tv_updatetime);
        mTvDegree = (AppCompatTextView) findViewById(R.id.tv_degree);
        mTvWeatherinfo = (AppCompatTextView) findViewById(R.id.tv_weatherinfo);
        mLlForecast = (LinearLayout) findViewById(R.id.ll_forecast);
        mTvAqi = (AppCompatTextView) findViewById(R.id.tv_aqi);
        mTvPm25 = (AppCompatTextView) findViewById(R.id.tv_pm25);
        mTvComfort = (AppCompatTextView) findViewById(R.id.tv_comfort);
        mTvCarwash = (AppCompatTextView) findViewById(R.id.tv_carwash);
        mTvSport = (AppCompatTextView) findViewById(R.id.tv_sport);
        mSvWeather = (ScrollView) findViewById(R.id.sv_weather);
        mImgBingpic = (AppCompatImageView) findViewById(R.id.img_bingpic);
    }
}
