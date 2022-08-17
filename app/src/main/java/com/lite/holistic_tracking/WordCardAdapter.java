package com.lite.holistic_tracking;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WordCardAdapter extends RecyclerView.Adapter<WordCardAdapter.ItemViewHolder> {
    //삭제 위해서 백이랑 연결
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private int p_userId= MainActivity.p_userID;
    private String BASE_URL=LoginActivity.getBASE_URL();
    private String stringp_userId=String.valueOf(p_userId);
    int del=-1;
    //
    private LayoutInflater inflater;
    //adapter에 들어갈 list
    private ArrayList<Data> listData = new ArrayList<>();

    //Item의 클릭 상태를 저장할 array 객체
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    //직전에 클릭했던 item의 position
    private int prePosition = -1;
    private ArrayList<Data> data;
    private ArrayList<Data> data3;
    public WordCardAdapter(Context context, ArrayList<Data> data){
        this.inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wordcard_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Data data1 = data.get(position);
        holder.wordTitle.setText(data1.getWord());
        holder.wordImage.setImageResource(R.drawable.test1);
//        holder.onBind(listData.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
    void addItem(Data data){
        listData.add(data);
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder{
        private Data data;
        public final View mView;
        private TextView wordTitle;
        private ImageView wordImage;
        private LinearLayout expanded;
        private ImageButton wordDeleteBtn;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            wordTitle = itemView.findViewById(R.id.wordTitle);
            wordImage = itemView.findViewById(R.id.wordImage);
            expanded = itemView.findViewById(R.id.expandedLayout);


            wordDeleteBtn = itemView.findViewById(R.id.wordDeleteBtn);

            mView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    int isVisible = expanded.getVisibility();

                    if(isVisible==8){
                        expanded.setVisibility(v.VISIBLE);
                    }else if(isVisible==0){
                        expanded.setVisibility(v.GONE);
                    }

                }
            });

            wordDeleteBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    int position=getAdapterPosition();
                    custom_dialog(v, position);
                }
            });
        }
//        void onBind(Data data){
//            wordTitle.setText(data.getTitle());
//            wordImage.setImageResource(data.getResId());

//        }
    }
    public void custom_dialog(View v, int position){
        View dialogView = inflater.inflate(R.layout.dialog_delete,null);

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView ok_btn = dialogView.findViewById(R.id.ok_btn);
        ok_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //값 알아내기
                Data data2=data.get(position);
                //Log.e("도대체 포지션 값이 뭔데", position+"");
                String word22= data2.getWord();
                Log.e("data에 포지션 1 더한 값 // 값을 뽑아볼게용", word22);

                //삭제
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                retrofitInterface = retrofit.create(RetrofitInterface.class);

                HashMap<String, String> map=new HashMap<>();
                //Log.e("확인 용",stringp_userId);

                map.put("UserId", stringp_userId);
                map.put("Word", word22);
                System.out.println(map);

                Call<JsonElement> call2=retrofitInterface.delList(map);
                call2.enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if(response.code()==200){
                            Log.e("삭제성공", data2.getWord());
                            Fragment_WordCard.delFilter(data2.getWord());
                        }
                        else{
                            Log.e("(._. ", "삭제 실패!");
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        Log.e("여기는 워드 카드 어댑터 ('-' (", "연결 실패!");
                    }
                });
                alertDialog.dismiss();
            }
        });
        TextView cancel_btn = dialogView.findViewById(R.id.cancel_btn);
        cancel_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                alertDialog.dismiss();
            }
        });
    }

    //검색 용
    public void filterList(ArrayList<Data> filterList){
        data= filterList;
        notifyDataSetChanged();
    }

    public void refresh1(ArrayList<Data> dellist){
        data=dellist;
        notifyDataSetChanged();
    }


}