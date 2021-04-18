package com.example.mynewsmsapp_kotlin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

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
    private static String TAG = "[MY_DEBUG]";
    String id;
    String number;
    String message_body;
    //get the context from the backgorund service  ClassificationSyncService
    Context context;

    PredictionProbingRunnable(Context context, String id, String number, String message_body){
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
                url = new URL("http://192.168.146.2:5000/predict");
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

            //JSON String need to be constructed for the specific resource.
            //We may construct complex JSON using any third-party JSON libraries such as jackson or org.json
//            String id = "\"1000\"";
//            String number = "\"9999977777\"";

//            String message_body = "\"Hi, I am in a meeting. Will call back later.\"";
//            message_body = "\"URGENT! Your Mobile No 07808726822 was awarded a L2,000 Bonus Caller Prize on 02/09/03! This is our 2nd attempt to contact YOU! Call 0871-872-9758 BOX95QU\"";

            String jsonInputString = "{\"id\": " + id + ", \"number\": " + number + ", \"message_body\": "+ message_body + "}";

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
            System.out.println("[MY_DEBUG] message body : " + message_body);
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
                System.out.println("[MY_DEBUG] extracted id = " + obj.get("id"));
                System.out.println("[MY_DEBUG] Extracted spam = " + obj.get("spam"));
                System.out.println("[MY_DEBUG]");
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
