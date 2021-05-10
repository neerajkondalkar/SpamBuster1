package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.example.mynewsmsapp_kotlin.ChatWindowActivity.hashmap_indexofmessage_to_tableallid_ChatWindowActivity;
import static com.example.mynewsmsapp_kotlin.ChatWindowActivity.hashmap_tableallid_to_spam_ChatWindowActivity;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.HAM;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.SPAM;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.UNCLASSIFIED;

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
//        final String TAG_insert = " insertPerson(): ";
//        Log.d(TAG, "ChatWindowSmsAdapter: insertPerson(): adding a new item: " + address + " in adapter at index + " + position);
        sms_messages_list.add(position, address);
        notifyDataSetChanged();
    }

    public void insert(int position, String new_sms) {
        final String TAG_insert = " insert(): ";
//        Log.d(TAG, TAG_insert + " called ");
//        Log.d(TAG, "ChatWindowSmsAdapter: insert(): adding a new message in adapter at index + " + position);
        sms_messages_list.add(position, new_sms);
        notifyDataSetChanged();
    }

    public void append(int position, Collection new_messages){
        final String TAG_append = " append(): ";
        Log.d(TAG, TAG_append + " called ");
//        Log.d(TAG, "ChatWindowSmsAdapter: append(): appending at index " + position);
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

        @SuppressLint("DefaultLocale")
        @Override
        public void onClick(View v) {
//            int position = getLayoutPosition();
//            String person_name = MainActivity.getContactName(context, sms_messages_list.get(position));
//            Toast.makeText(context, "Position : " + position + " - " + sms_messages_list.get(position) + " alias " + person_name, Toast.LENGTH_SHORT).show();
//            Intent start_chat_windows_activity = new Intent(MainActivity.instance(), ChatWindowActivity.class);
//            start_chat_windows_activity.putExtra("address", sms_messages_list.get(position));
//            context.startActivity(start_chat_windows_activity);
            int position = getLayoutPosition();
            String tableallID = hashmap_indexofmessage_to_tableallid_ChatWindowActivity.get(position);
            String spam = hashmap_tableallid_to_spam_ChatWindowActivity.get(tableallID);
            String spam_status;
            if(spam.equals(SPAM)){
                spam_status = "SPAM";
            }
            else if(spam.equals(HAM)){
                spam_status = "HAM";
            }
            else{
                spam_status = "UNCLASSIFIED";
            }
            Log.d(TAG, String.format(" click at position: %d,   itemID : %s ,   spam = %s\n", position, tableallID, spam));
            Toast.makeText(context, String.format(" click at position: %d,   itemID : %s, it is %s", position, tableallID, spam_status), Toast.LENGTH_LONG).show();

            //show dialog only if SPAM or HAM,  and not UNCLASSIFIED (maybe later we can put that)
            if(!spam.equals(UNCLASSIFIED)) {
                showDialog(spam);
            }
        }

    private void showDialog(String spam){
            final String option;
        String option1;
        if(spam.equals(SPAM)){
            option1 = "INBOX";
        }
        else{
            option1 = "SPAM";
        }
        option = option1;
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        View mView = ChatWindowActivity.instance().getLayoutInflater().inflate(R.layout.dummy_dialogue,null);
        Button btn_cancel = (Button)mView.findViewById(R.id.btn_cancel);
        Button btn_option = (Button)mView.findViewById(R.id.btn_okay);
        btn_option.setText("Move to " + option);
        alert.setView(mView);
        final AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        btn_option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "You clicked on option " + option, Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
    }

}