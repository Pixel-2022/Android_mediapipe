package com.lite.holistic_tracking;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Fragment_WordDict extends Fragment {
    //백엔드 연동
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private View v;
    private RecyclerView recyclerView;
    private LinearLayoutManager LinearLayoutManager;
    ArrayList<Dict> dataList=new ArrayList();
    String[] names;
    String[] images;
    String[] videoURLs;
    private String BASE_URL=LoginActivity.getBASE_URL();

    //필터
    EditText searchET;
    ArrayList<Dict> filteredList;
    DictAdapter adapter;



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        v= inflater.inflate(R.layout.f2_worddict,container,false);
        Context context = v.getContext();

        filteredList=new ArrayList<>();
        // 리사이클
        recyclerView = v.findViewById(R.id.recyclerView1);
        searchET=v.findViewById(R.id.search_edit);


        //retrofit build
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
        Call<JsonElement> call = retrofitInterface.getDictAll();
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                JsonArray DictResponseArray = response.body().getAsJsonArray();

                //배열 선언
                names = new String[DictResponseArray.size()];
                images = new String[DictResponseArray.size()];
                videoURLs = new String[DictResponseArray.size()];
                for (int i=0; i<DictResponseArray.size();i++){
                    JsonElement jsonElement = DictResponseArray.get(i);
                    String name = jsonElement.getAsJsonObject().get("Word").getAsString();
                    String videoURL = jsonElement.getAsJsonObject().get("videoURL").getAsString();
                    String wordImg = jsonElement.getAsJsonObject().get("wordImg").getAsString();
                    names[i]=name;
                    images[i]=wordImg;
                    videoURLs[i] = videoURL;
                    dataList.add(new Dict(names[i], images[i], videoURLs[i]));
                }
                Log.e("dataList : ",dataList.get(0).getWord());
                adapter=new DictAdapter(context, dataList);

                LinearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
                recyclerView.setLayoutManager(LinearLayoutManager);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e("실패군","실패다");
                dataList.add(new Dict("실패예요","http://drive.google.com/uc?export=view&id=1djpCxxUwXj12MzTCdl-wDZqZIuuJTl7B/view", "ASdasdasdafasdsafad"));
                adapter=new DictAdapter(context, dataList);
                LinearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
                recyclerView.setLayoutManager(LinearLayoutManager);
                recyclerView.setAdapter(adapter);
            }
        });

        searchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String searchText=searchET.getText().toString();
                searchFilter(searchText);
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume(){
        super.onResume();
    }
    public void searchFilter(String searchText){
        filteredList.clear();

        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).getWord().toLowerCase().contains(searchText.toLowerCase())) {
                filteredList.add(dataList.get(i));
            }
        }
        adapter.filterList(filteredList);
    }

}
