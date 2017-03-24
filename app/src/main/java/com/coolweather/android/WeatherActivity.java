package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.WeatherBean;
import com.coolweather.android.service.AutoUpdateService;
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
    private AppCompatTextView mTvflu;
    private AppCompatTextView mTvSport;
    private ScrollView mSvWeather;
    private AppCompatImageView mImgBingpic;
    public SwipeRefreshLayout mSwipeRefresh;
    private AppCompatImageView mIvHome;
    public DrawerLayout mDrawLayout;
    private String mBing_pic;
    private AppCompatTextView mTvUv;
    private FloatingActionButton mFab;
    private SharedPreferences mPreferences;

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
        //设置下拉刷新的颜色
        mSwipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = mPreferences.getString("weather", null);
        final String weatherId;
        mBing_pic = mPreferences.getString("bing_pic", null);
        if (mBing_pic != null) {
            Glide.with(this).load(mBing_pic).into(mImgBingpic);
        } else {
            loadBingPic();
        }
        if (weatherString != null) {
            //有缓存时直接解析数据
            WeatherBean weatherBean = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weatherBean);
        } else {
            //无缓存时去服务器查询
            weatherId = getIntent().getStringExtra("weather_id");
            mSvWeather.setVisibility(View.GONE);
            requestWeather(weatherId);
        }
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefresh.setRefreshing(true);
                String weatherString = mPreferences.getString("weather", null);
                WeatherBean weatherBean = Utility.handleWeatherResponse(weatherString);
                String id = weatherBean.getBasic().getId();
                requestWeather(id);
            }
        });

        /**
         * 设置抽屉布局的监听
         */
        mIvHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawLayout.openDrawer(GravityCompat.START);
            }
        });

        /**
         * 设置FloatActionButton的监听
         */
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(WeatherActivity.this, "等待后续添加...", Toast.LENGTH_SHORT).show();
            }
        });
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
    public void requestWeather(String weatherId) {
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
                            showSnackBar();


                        }
                        mSwipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSnackBar();
                        mSwipeRefresh.setRefreshing(false);
                    }
                });
            }


        });
        loadBingPic();
    }

    /**
     * 展示SnackBar的提示信息
     */
    public void showSnackBar() {
        Snackbar snackbar = Snackbar.make(mSvWeather, "你目前处于没网的二次元世界", Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.colortouming));
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(getResources().getColor(R.color.colorsnackbar));
        snackbar.setActionTextColor(getResources().getColor(R.color.colorsnackbar));
        snackbar.setAction("查看网络", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到系统的网络设置界面
                Intent intent = null;
                // 先判断当前系统版本
                if (Build.VERSION.SDK_INT > 10) {  // 3.0以上
                    intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                } else {
                    intent = new Intent();
                    intent.setClassName("com.android.settings", "com.android.settings.WirelessSettings");
                }
                startActivity(intent);
            }
        });
        snackbar.show();
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
            mTvMax.setText(forecastBean.getTmp().getMax() + "℃");
            mTvMin.setText(forecastBean.getTmp().getMin() + "℃");
            mLlForecast.addView(view);
        }
        if (weather.getAqi() != null) {
            mTvAqi.setText(weather.getAqi().getCity().getAqi());
            mTvPm25.setText(weather.getAqi().getCity().getPm25());
        }
        String comfort = "舒适度：" + weather.getSuggestion().getComf().getTxt();
        String flu = "感冒指数：" + weather.getSuggestion().getFlu().getTxt();
        String sport = "运动建议：" + weather.getSuggestion().getSport().getTxt();
        String uv = "紫外线指数：" + weather.getSuggestion().getUv().getTxt();
        mTvComfort.setText(comfort);
        mTvflu.setText(flu);
        mTvSport.setText(sport);
        mTvUv.setText(uv);
        mSvWeather.setVisibility(View.VISIBLE);

        /**
         *  启动后台更新天气的服务
         */
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

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
        mTvflu = (AppCompatTextView) findViewById(R.id.tv_flu);
        mTvSport = (AppCompatTextView) findViewById(R.id.tv_sport);
        mSvWeather = (ScrollView) findViewById(R.id.sv_weather);
        mImgBingpic = (AppCompatImageView) findViewById(R.id.img_bingpic);
        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mIvHome = (AppCompatImageView) findViewById(R.id.iv_home);
        mDrawLayout = (DrawerLayout) findViewById(R.id.draw_layout);
        mTvUv = (AppCompatTextView) findViewById(R.id.tv_uv);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
    }
}
