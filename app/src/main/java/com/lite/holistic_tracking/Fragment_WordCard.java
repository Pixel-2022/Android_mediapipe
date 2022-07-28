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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Fragment_WordCard extends Fragment {
    //백
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private String BASE_URL=LoginActivity.getBASE_URL();
    private RecyclerView recyclerView;

    String[] words;
    Boolean[] stars;
    int[] ids;
    int[] userids;
    private int p_userId= MainActivity.p_userID;
    private String stringp_userId=String.valueOf(p_userId);
    //
    private View v;
    WordCardAdapter adapter;
    private RecyclerView.LayoutManager mLayoutManager;
    ArrayList<Data> dataList=new ArrayList();


    EditText searchET;
    ArrayList<Data> filteredList;



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        v= inflater.inflate(R.layout.f2_wordcard,container,false);
        Context context = v.getContext();
        recyclerView = v.findViewById(R.id.cardList);
        filteredList=new ArrayList<>();
        searchET=v.findViewById(R.id.search_edit2);


        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new WordCardAdapter(context,dataList);

        recyclerView.setAdapter(adapter);


        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
        HashMap<String, String> map=new HashMap<>();
        //Log.e("확인 용",stringp_userId);
        map.put("UserId", stringp_userId);

        Call<JsonElement> call = retrofitInterface.getListAll(map);
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if(response.body() !=null ){
                    JsonArray ListResponseArray = response.body().getAsJsonArray();


                    ids=new int[ListResponseArray.size()];
                    stars=new Boolean[ListResponseArray.size()];
                    userids=new int[ListResponseArray.size()];
                    words=new String[ListResponseArray.size()];

                    for (int i=0; i<ListResponseArray.size();i++){
                        JsonElement jsonElement = ListResponseArray.get(i);
                        int id = jsonElement.getAsJsonObject().get("id").getAsInt();
                        Boolean star = jsonElement.getAsJsonObject().get("star").getAsBoolean();
                        int uid=jsonElement.getAsJsonObject().get("UserId").getAsInt();
                        String word = jsonElement.getAsJsonObject().get("Word").getAsString();

                        ids[i] = id;
                        stars[i]=star;
                        userids[i]=uid;
                        words[i]=word;
                    }
                    //userid 같은 것 들만 리사이클러에 추가
                    for (int i=0; i< ListResponseArray.size(); i++){
                        if(userids[i]==p_userId){
                            dataList.add(new Data(ids[i],  userids[i], stars[i], words[i]));
                        }
                    }
                    System.out.println(dataList);
                    Log.e("AAA","일단 받아는 왔다.");
                    adapter=new WordCardAdapter(context, dataList);
                    mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
                    recyclerView.setLayoutManager(mLayoutManager);
                    recyclerView.setAdapter(adapter);

                }else{Log.e("AAA","내용물이 비어있습니다.");}
            }
            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e("연결이", "실패했습니다...");
                dataList.add(new Data(2,  p_userId, false, "예시"));
                adapter=new WordCardAdapter(context, dataList);
                mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
                recyclerView.setLayoutManager(mLayoutManager);
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

//        SearchView searchView = (SearchView) v.findViewById(R.id.searchView);
//        searchView.setIconifiedByDefault(false);
//
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String a_query) {
//                // to do
//                searchView.clearFocus();
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String a_newText) {
//                // to do
//                return false;
//            }
//        });
        return v;
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
