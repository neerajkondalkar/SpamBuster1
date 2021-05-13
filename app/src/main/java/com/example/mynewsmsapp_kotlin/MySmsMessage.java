package com.example.mynewsmsapp_kotlin;

public class MySmsMessage {
    private  String address;
    private String date;
    private String datesent;
    private String body;

    MySmsMessage(String address, String date, String datesent, String body){
        this.address = address;
        this.body = body;
        this.date = date;
        this.datesent = datesent;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDatesent() {
        return datesent;
    }

    public void setDatesent(String datesent) {
        this.datesent = datesent;
    }
}
