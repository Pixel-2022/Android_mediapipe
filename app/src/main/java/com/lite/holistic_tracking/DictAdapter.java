package com.lite.holistic_tracking;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class DictAdapter extends RecyclerView.Adapter<DictAdapter.ViewHolder>{
    private LayoutInflater inflater;
    int[] images;
    private Context context;
    //백연결
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private int p_userId= MainActivity.p_userID;
    private String stringp_userId=String.valueOf(p_userId);
    private String BASE_URL=LoginActivity.getBASE_URL();

    //Item의 클릭 상태를 저장할 array 객체
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    //직전에 클릭했던 item의 position
    private int prePosition = -1;
    //
    private ArrayList<Dict> dict;
    public DictAdapter(Context context, ArrayList<Dict> dict){
        this.inflater = LayoutInflater.from(context);
        this.dict=dict;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context= parent.getContext();
        this.context=parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_word,parent, false);
        return new ViewHolder(view);
    }

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Dict dict1=dict.get(position);
        holder.textView.setText(dict1.getWord());
        Glide.with(holder.imageView.getContext()).load(dict1.getImage()).into(holder.imageView);
    }


    @Override
    public int getItemCount() {
        return dict.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        ImageView imageView;
        ImageView save;
        ImageView showVideo;
        ImageView dictAct;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            imageView = itemView.findViewById(R.id.dict_image);
            textView = itemView.findViewById(R.id.dict_name);
            save=itemView.findViewById(R.id.save_word);
            showVideo=itemView.findViewById(R.id.show_video);
            dictAct = itemView.findViewById(R.id.dict_act);
            dictAct.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("아이템 클릭", String.valueOf(getAdapterPosition()));
                }
            });
            save.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Log.e("포지션", String.valueOf(getAdapterPosition()));
                    custom_dialog(view, getAdapterPosition());
                }
            });
            showVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    custom_dialog2(view,getAdapterPosition());
                    Log.d("비디오에서도 position이 나오나요?", String.valueOf(getAdapterPosition()));

                }
            });

            //카드 선택 시 이동
            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    MainActivity main = (MainActivity) DictAdapter.this.context;
                    // main.toMain(b);
                }
            });
        }
    }


    //검색 용
    public void filterList(ArrayList<Dict> filterList){
        dict= filterList;
        notifyDataSetChanged();
    }

    public void custom_dialog(View v, int position){
        View dialogView = inflater.inflate(R.layout.dialog_wordadd,null);

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView ok_btn = dialogView.findViewById(R.id.ok_btn);
        ok_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Dict dict2=dict.get(position);
                String word22=dict2.getWord();
                Log.e("추가할 값을 뽑아볼게용", word22);

                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                retrofitInterface = retrofit.create(RetrofitInterface.class);

                HashMap<String, String> map=new HashMap<>();
                //Log.e("확인 용",stringp_userId);
                map.put("UserId", stringp_userId);
                map.put("Word", word22);

                Call<JsonElement> call1=retrofitInterface.addList(map);
                call1.enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if(response.code()==200){
                            Log.e("('a' ", "추가 성공!");
                        }
                        else{
                            Log.e("(._. ", "추가 실패!");
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        Log.e("('-' 여기는 딕트 (", "연결 실패!");
                    }
                });
                alertDialog.dismiss();
            }
        });
    }
    public void custom_dialog2(View v,int position){
        Dict dict2=dict.get(position);
        VideoView vv;
        View dialogView = inflater.inflate(R.layout.dialog_video,null);

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setView(dialogView);

        vv= dialogView.findViewById(R.id.videoV);
        //URL서ㅕㄹ정
        Uri videoUri= Uri.parse(dict2.getVideoURL());
        vv.setMediaController(new MediaController(context));
        vv.setVideoURI(videoUri);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //비디오 시작
                vv.start();
            }
        });

        TextView ok_btn = dialogView.findViewById(R.id.ok_btn);
        ok_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                alertDialog.dismiss();
            }
        });
    }
}