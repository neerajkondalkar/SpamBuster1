package com.example.mynewsmsapp_kotlin;

import android.content.Context;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PredictionUtility {
    private static final String TAG = "[MY_DEBUG]";
    Context context;
    HashMap<String, Integer> result_map;
    PredictionUtility(Context context) {
        this.context = context;
        result_map = new HashMap<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public HashMap<String, Integer> makePrediction(Map<String, String> map){
        boolean http_req_success = new SBNetworkUtility().checkNetwork(context);

        if(!http_req_success){
            Log.d(TAG + "[API]", "NewSmsMessageRunnable: run(): internet not available, skipping classification for now");
            result_map = null;
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
//            String[] id = new String[1];
//            id[0] = id_str;
////            String[] message_body = new String[5];
//            String[] message_body = new String[1];
//            message_body[0] = sms_body_str;
//            message_body[0] = "Hi, I am in a meeting. Will call back later.";
//            message_body[1] = "IMPORTANT - You could be entitled up to £3,160 in compensation from mis-sold PPI on a credit card or loan. Please reply PPI for info or STOP to opt out.";
//            message_body[2] = "A [redacted] loan for £950 is approved for you if you receive this SMS. 1 min verification & cash in 1 hr at www.[redacted].co.uk to opt out reply stop";
//            message_body[3] = "You have still not claimed the compensation you are due for the accident you had. To start the process please reply YES. To opt out text STOP";
//            message_body[4] = "Our records indicate your Pension is under performing to see higher growth and up to 25% cash release reply PENSION for a free review. To opt out reply STOP";
            // String number = "\"9999977777\"";
            Set<Map.Entry<String, String>> entryset = map.entrySet();
            JSONArray ja = new JSONArray();
            for(Map.Entry<String, String> entry : entryset){
                JSONObject jo = new JSONObject();
                try {
                    String id = entry.getKey();
                    String message_body = entry.getValue();
                    jo.put("id", id);
                    jo.put("message_body", message_body);
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

            Log.d(TAG + "[API]", "NewSmsMessageRunnable: makePrediction(): \"Printing json main object");
            Log.d(TAG + "[API]", "NewSmsMessageRunnable: makePrediction(): " + jsonInputString);

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
            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): ");
            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): HTTP POST request done.");
            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): Response code: " + code);

            try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))){
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                // System.out.println(response.toString());
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): Complete response:");
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): " + response.toString());
                JSONObject obj = null;
                try {
                    obj = new JSONObject(response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): length of JSON obj : " + obj.length());

                JSONArray result_ja = (JSONArray) obj.get("result");
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): Printing <JSONOArray> result_ja :" + result_ja);
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): length of result_ja : " + result_ja.length());
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): looping through the json array ");

                for(int i=0; i<result_ja.length(); i++){
                    JSONObject tempjo = (JSONObject) result_ja.get(i);
                    System.out.printf("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): [%d]  %s\n", i, tempjo.toString());
                    try{
                        System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): extract ID and spam prediction from the JSON object");
                        System.out.printf("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): [%d]  %s  -   %s\n", i, tempjo.get("id").toString(), tempjo.get("spam").toString());
                        if(tempjo.get("spam").toString().equals("1")){
                            result_map.put(tempjo.get("id").toString(), 1);
//                            return 1;
                        }
                        else if(tempjo.get("spam").toString().equals("0")){
                            result_map.put(tempjo.get("id").toString(), 0);
//                            return 0;
                        }
                        else{
                            result_map.put(tempjo.getString("id"), -1);
//                            return -1;
                        }

                    }
                    catch(Exception e){
                        e.printStackTrace();
                        try{
                            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): got error : " + tempjo.get("error").toString());
//                            result_map.put(tempjo.getString("id"), -1);
//                            return -1;
                            return null;
                        }
                        catch(Exception e1){
                            e1.printStackTrace();
//                            result_map.put(tempjo.getString("id"), -1);
//                            return -1;
                            return null;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result_map;
    }
}

