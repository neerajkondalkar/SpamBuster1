package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;

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

    public void removeMessage(int position){
        sms_messages_list.remove(position);
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
                showDialog(tableallID, spam);
            }
        }

    private void showDialog(final String tableallid, final String spam){
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
        View mView = ChatWindowActivity.instance().getLayoutInflater().inflate(R.layout.movemessagedialog,null);
        Button btn_cancel = (Button)mView.findViewById(R.id.btn_cancel);
        Button btn_option = (Button)mView.findViewById(R.id.btn_okay);
        TextView txt_title = (TextView)mView.findViewById(R.id.txt_movetotitle);
        Button btn_delete = (Button)mView.findViewById(R.id.btn_delete);
        txt_title.setText(String.format("Move message id: %s to %s ?", tableallid, option));
        alert.setView(mView);
        final AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        //cancel button just exits the dialog box
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
                //if option=="SPAM"
                if(option.equals("SPAM")){
                    //spawn a  runnable to update the TABLEALL with column_spam = "SPAM"
                    moveToSpam(tableallid);
                }
                else{
                    moveToInbox(tableallid);
                }
                alertDialog.dismiss();
            }
        });
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMessage(tableallid);
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private  void moveToSpam(String id) {
            new Thread(new MoveToRunnable(context, id, SPAM)).start();
    }
    private  void moveToInbox(String id){
            new Thread(new MoveToRunnable(context, id, HAM)).start();
    }
    private void deleteMessage(final String id){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(DbOperationsUtility.getInstance().deleteMessage(id, context)){
                        Log.d(TAG, "SmsViewHolder: run(): Message with id:" + id + " has been deleted successfully!");
                        Handler handler = ChatWindowActivity.instance().getHandler();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ChatWindowActivity.instance(), "Message with id:" + id + " has been deleted successfully!", Toast.LENGTH_SHORT).show();
                                removeMessage(getLayoutPosition());
                            }
                        });
                    }
                    else{
                        Log.d(TAG, "SmsViewHolder: run(): Could not delete message with id:" + id);
                    }
                }
            }).start();
    }
    }
}