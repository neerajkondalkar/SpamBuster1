package com.example.mynewsmsapp_kotlin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class PredictionProbingRunnable implements Runnable {
    private static String TAG = "[MY_DEBUG] [API]";
    String[] id;
    String number;
    String[] message_body;
    //get the context from the backgorund service  ClassificationSyncService
    Context context;

    PredictionProbingRunnable(Context context, String[] id, String number, String[] message_body){
        this.id = id;
        this.number = number;
        this.message_body = message_body;
        this.context = context;
//        context =  new ClassificationSyncService().context;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        boolean http_req_success = false;
        http_req_success = checkNetwork();

        if(!http_req_success){
            Log.d(TAG, "PredictionProbingRunnable: run(): internet not available, skipping classification for now");
        }
        else{
            URL url = null;
            try {
                url = new URL("http://192.168.1.102:5000/predict");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection)url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }

            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");

            con.setDoOutput(true);

//            String id = "1000";
//            String[] message_body = new String[5];
//            message_body[0] = "Hi, I am in a meeting. Will call back later.";
//            message_body[1] = "IMPORTANT - You could be entitled up to £3,160 in compensation from mis-sold PPI on a credit card or loan. Please reply PPI for info or STOP to opt out.";
//            message_body[2] = "A [redacted] loan for £950 is approved for you if you receive this SMS. 1 min verification & cash in 1 hr at www.[redacted].co.uk to opt out reply stop";
//            message_body[3] = "You have still not claimed the compensation you are due for the accident you had. To start the process please reply YES. To opt out text STOP";
//            message_body[4] = "Our records indicate your Pension is under performing to see higher growth and up to 25% cash release reply PENSION for a free review. To opt out reply STOP";
            // String number = "\"9999977777\"";
            JSONArray ja = new JSONArray();

            for(int i=0; i<id.length; i++){
                JSONObject jo = new JSONObject();
//                Integer idint = Integer.parseInt(id) + i;
                try {
//                    jo.put("id", String.valueOf(idint));
                    jo.put("id", id[i]);
                    jo.put("message_body", message_body[i]);
                    ja.put(jo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            JSONObject mainObj = new JSONObject();
            try {
                mainObj.put("entries", ja);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String jsonInputString = mainObj.toString();

            Log.d(TAG, "PredictionRunnable: run(): \"Printing json main object");
            Log.d(TAG, "PredictionRunnable: run(): " + jsonInputString);

            try(OutputStream os = con.getOutputStream()){
                byte[] input = new byte[0];
                try {
                    input = jsonInputString.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    os.write(input, 0, input.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            int code = 0;
            try {
                code = con.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("[MY_DEBUG]");
            System.out.println("[MY_DEBUG] HTTP POST request done.");
            System.out.println("[MY_DEBUG] Response code: " + code);

            try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))){
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                // System.out.println(response.toString());
                System.out.println("[MY_DEBUG] Complete response:");
                System.out.println("[MY_DEBUG] " + response.toString());
                JSONObject obj = null;
                try {
                    obj = new JSONObject(response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                System.out.println("[MY_DEBUG] length of JSON obj : " + obj.length());

                JSONArray result_ja = (JSONArray) obj.get("result");
                System.out.println("[MY_DEBUG] Printing <JSONOArray> result_ja :" + result_ja);
                System.out.println("[MY_DEBUG] length of result_ja : " + result_ja.length());
                System.out.println("[MY_DEBUG] looping through the json array ");

                for(int i=0; i<result_ja.length(); i++){
                    JSONObject tempjo = (JSONObject) result_ja.get(i);
                    System.out.printf("[MY_DEBUG] [%d]  %s\n", i, tempjo.toString());
                    try{
                        System.out.println("[MY_DEBUG] extract ID and spam prediction from the JSON object");
                        System.out.printf("[MY_DEBUG] [%d]  %s  -   %s\n", i, tempjo.get("id").toString(), tempjo.get("spam").toString());
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        try{
                            System.out.println("[MY_DEBUG] got error : " + tempjo.get("error").toString());
                        }
                        catch(Exception e1){
                            e1.printStackTrace();
                        }
                    }
                    System.out.println("[MY_DEBUG]");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkNetwork() {
        boolean wifiAvailable = false;
        boolean mobileAvailable = false;
        ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = conManager.getAllNetworkInfo();
        for (NetworkInfo netInfo : networkInfo) {
            if (netInfo.getTypeName().equalsIgnoreCase("WIFI"))
                if (netInfo.isConnected())
                    wifiAvailable = true;
            if (netInfo.getTypeName().equalsIgnoreCase("MOBILE"))
                if (netInfo.isConnected())
                    mobileAvailable = true;
        }
        return wifiAvailable || mobileAvailable;
    }
}
