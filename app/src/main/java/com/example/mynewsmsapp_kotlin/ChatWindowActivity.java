package com.example.mynewsmsapp_kotlin;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

public class ChatWindowActivity extends AppCompatActivity {
    public static final String TAG = "[MY_DEBUG]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatwindow);
        String address = getIntent().getStringExtra("address");
        Log.d(TAG, "ChatWindowActivity: onCreate(): address received through extras : " + address);
    }
}