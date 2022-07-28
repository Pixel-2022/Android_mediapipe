package com.lite.holistic_tracking;

public class Data {
    private int id;
    private Boolean star;
    private int UserId;
    private String Word;

    public Data(int id, int userid, Boolean star, String Word){
        this.id = id;
        this.UserId=userid;
        this.Word = Word;
        this.star = star;
    }
    public int getid(){
        return id;
    }
    public void setid(int id){
        this.id = id;
    }
    public int getUserId(){
        return UserId;
    }
    public void setUserId(int UserId){
        this.UserId = UserId;
    }
    public Boolean getStar(){
        return star;
    }
    public void setStar(Boolean star){
        this.star = star;
    }
    public String getWord(){
        return Word;
    }
    public void setWord(String Word){
        this.Word = Word;
    }

}
