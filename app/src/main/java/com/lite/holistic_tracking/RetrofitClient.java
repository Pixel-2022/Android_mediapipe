package com.lite.holistic_tracking;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class RetrofitClient {
    private Retrofit retrofit;
    public void generateClient(){

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(6000, TimeUnit.SECONDS)
                .connectTimeout(6000, TimeUnit.SECONDS)
                .build();
        retrofit = new Retrofit.Builder()
//                ⚠[주의!] 자신의 ip 주소로 변경할 것!
                .baseUrl("http://192.168.43.46:5000") //Make sure to include your local machine or server ip/url
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }
    public RetrofitInterface getApi(){
        return retrofit.create(RetrofitInterface.class);
    }
}