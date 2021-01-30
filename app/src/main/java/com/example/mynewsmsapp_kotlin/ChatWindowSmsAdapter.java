package com.example.mynewsmsapp_kotlin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChatWindowSmsAdapter extends RecyclerView.Adapter<ChatWindowSmsAdapter.SmsViewHolder>{
    private static final String TAG = " [MY_DEBUG] ";

    ArrayList<String> sms_messages_list = new ArrayList<>();
    Context context;

    public  ChatWindowSmsAdapter(Context ct, ArrayList<String> array_list){
        this.context = ct;
        this.sms_messages_list.addAll(array_list);
        sms_messages_list.clear();
    }

    public void insertPerson(int position, String address) {
        final String TAG_insert = " insertPerson(): ";
        Log.d(TAG, "ChatWindowSmsAdapter: insertPerson(): adding a new item: " + address + " in adapter at index + " + position);
        sms_messages_list.add(position, address);
        notifyDataSetChanged();
    }

    public void insert(int position, String new_sms) {
        final String TAG_insert = " insert(): ";
        Log.d(TAG, TAG_insert + " called ");
        Log.d(TAG, "ChatWindowSmsAdapter: insert(): adding a new message in adapter at index + " + position);
        sms_messages_list.add(position, new_sms);
        notifyDataSetChanged();
    }

    public void append(int position, Collection new_messages){
        final String TAG_append = " append(): ";
        Log.d(TAG, TAG_append + " called ");
        Log.d(TAG, "ChatWindowSmsAdapter: append(): appending at index " + position);
        sms_messages_list.addAll(position, new_messages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layout_inflater = LayoutInflater.from(context);
        View view = layout_inflater.inflate(R.layout.chatwindows_smsrow, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        holder.chat_sms_text.setText(MainActivity.getContactName(context, sms_messages_list.get(position).toString()));
    }

    @Override
    public int getItemCount() {
        return sms_messages_list.size();
    }

    //inner class
    public class SmsViewHolder extends  RecyclerView.ViewHolder implements View.OnClickListener{
        TextView chat_sms_text;
        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            chat_sms_text = itemView.findViewById(R.id.chat_sms_text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            String person_name = MainActivity.getContactName(context, sms_messages_list.get(position));
            Toast.makeText(context, "Position : " + position + " - " + sms_messages_list.get(position) + " alias " + person_name, Toast.LENGTH_SHORT).show();
            Intent start_chat_windows_activity = new Intent(MainActivity.instance(), ChatWindowActivity.class);
            start_chat_windows_activity.putExtra("address", sms_messages_list.get(position));
            context.startActivity(start_chat_windows_activity);
        }
    }
}