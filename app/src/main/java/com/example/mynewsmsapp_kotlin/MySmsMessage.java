package com.example.mynewsmsapp_kotlin;

import android.util.Log;

public class MySmsMessage {
    private  String address;
    private String date;
    private String datesent;
    private String body;
    private String tableallid;
    private String corressinboxid;
    private String spam;

    MySmsMessage(String address, String date, String datesent, String body){
        this.address = address;
        this.body = body;
        this.date = date;
        this.datesent = datesent;
    }
    MySmsMessage(){ }
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

    //optional
    public String getTableallid() {
        return tableallid;
    }
    public void setTableallid(String tableallid) {
        this.tableallid = tableallid;
    }
    public String getCorressinboxid() {
        return corressinboxid;
    }
    public void setCorressinboxid(String corressinboxid) {
        this.corressinboxid = corressinboxid;
    }
    public String getSpam() {
        return spam;
    }
    public void setSpam(String spam) {
        this.spam = spam;
    }

    public boolean isMessageOTP(){
        String message = this.body;
        String[] checkotpstr = message.split(" ");
        for(String str : checkotpstr){
            if (str.equalsIgnoreCase("otp")){
                return true;
            }
        }
        return false;
    }

    public static boolean isMessageOTP(String message){
        String[] checkotpstr = message.split(" ");
        for(String str : checkotpstr){
            if (str.equalsIgnoreCase("otp") && (checkotpstr.length < 25)){
                return true;
            }
        }
        return false;
    }
}
