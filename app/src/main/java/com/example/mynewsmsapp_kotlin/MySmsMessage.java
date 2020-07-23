package com.example.mynewsmsapp_kotlin;

public class MySmsMessage {
    private  String address;
    private String body;
    private long date;
    MySmsMessage(String address, String body, long date){
        this.address = address;
        this.body = body;
        this.date = date;
    }
}
