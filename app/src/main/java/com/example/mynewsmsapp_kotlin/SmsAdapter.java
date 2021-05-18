package com.example.mynewsmsapp_kotlin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder>{
    private static final String TAG = " [MY_DEBUG] " + SmsAdapter.class.getSimpleName();
    public static HashMap<String, Integer> map_address_to_position = new HashMap<>();
    public static HashMap<Integer, String> map_position_to_address = new HashMap<>();
    ArrayList<String> sms_messages_list = new ArrayList<>();
    Context context;

    public  SmsAdapter(Context ct, ArrayList<String> array_list){
        this.context = ct;
        this.sms_messages_list.addAll(array_list);
        sms_messages_list.clear();
    }

    public void clearItems(){
        sms_messages_list.clear();
        notifyDataSetChanged();
    }

    public void insertPerson(int position, String address) {
        final String TAG_insert = " insertPerson(): ";
//        Log.d(TAG, "SmsAdapter: insertPerson(): adding a new item: " + address + " in adapter at index + " + position);
        sms_messages_list.add(position, address);
        map_address_to_position.put(address, position);
        map_position_to_address.put(position, address);
        notifyDataSetChanged();
    }

    private void  refreshMapAddressPosition(){
        map_address_to_position.clear();
        map_position_to_address.clear();
        for(int i=0; i<sms_messages_list.size(); i++){
            map_position_to_address.put(i, sms_messages_list.get(i));
            map_address_to_position.put(sms_messages_list.get(i), i);
        }
    }

    public  void addAllItems(ArrayList<String> list){
        sms_messages_list.addAll(list);
    }

    public void insert(int position, String new_sms) {
        final String TAG_insert = " insert(): ";
//        Log.d(TAG, TAG_insert + " called ");
//        Log.d(TAG, "SmsAdapter: insert(): adding a new message in adapter at index + " + position);
        sms_messages_list.add(position, new_sms);
        notifyDataSetChanged();
    }

    public void removePerson(int position){
        Log.d(TAG, "SmsAdapter: removePerson(): removing the person'conversation at position : " + position);
        sms_messages_list.remove(position);
        refreshMapAddressPosition();
        notifyDataSetChanged();
    }

    public void append(int position, Collection new_messages){
        final String TAG_append = " append(): ";
//        Log.d(TAG, TAG_append + " called ");
//        Log.d(TAG, "SmsAdapter: append(): appending at index " + position);
        sms_messages_list.addAll(position, new_messages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layout_inflater = LayoutInflater.from(context);
        View view = layout_inflater.inflate(R.layout.sms_row, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        holder.sms_text.setText(MainActivity.getContactName(context, sms_messages_list.get(position).toString()));
    }

    @Override
    public int getItemCount() {
        return sms_messages_list.size();
    }

    //inner class
    public class SmsViewHolder extends  RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        TextView sms_text;
        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            sms_text = itemView.findViewById(R.id.sms_text);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            String person_name = MainActivity.getContactName(context, sms_messages_list.get(position));
//            Toast.makeText(context, "Position : " + position + " - " + sms_messages_list.get(position) + " alias " + person_name, Toast.LENGTH_SHORT).show();
            Intent start_chat_windows_activity = new Intent(MainActivity.instance(), ChatWindowActivity.class);
            start_chat_windows_activity.putExtra("address", sms_messages_list.get(position));
            context.startActivity(start_chat_windows_activity);
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getLayoutPosition();
//            Toast.makeText(context, String.format("long pressed on position:%d where address is %s", position, map_position_to_address.get(position)), Toast.LENGTH_SHORT).show();
            showDialog(map_position_to_address.get(position));
            return true;
        }

        private void showDialog(final String address){
            final AlertDialog.Builder alert = new AlertDialog.Builder(context);
            View mView = MainActivity.instance().getLayoutInflater().inflate(R.layout.longclickdialog,null);
            Button btn_cancel = (Button)mView.findViewById(R.id.btn_cancel_convo);
            Button btn_delete = (Button)mView.findViewById(R.id.btn_delete_convo);
            TextView txt_title = (TextView)mView.findViewById(R.id.txt_longclickdialogtitle);
            txt_title.setText(String.format("Delete conversation of %s ?", MainActivity.getContactName(context, address)));
            alert.setView(mView);
            final AlertDialog alertDialog = alert.create();
            alertDialog.setCanceledOnTouchOutside(false);
            btn_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Cancelled delete operation", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
            });
            btn_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Messages will be deleted!", Toast.LENGTH_SHORT).show();
                    deleteConvo(address);
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        }

        private void deleteConvo(final String address){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(DbOperationsUtility.getInstance().deleteConversation(address, context)){
                        Log.d(TAG, "SmsViewHolder: run(): Conversation of " + address + " deleted successfully");
                        Handler handler = MainActivity.instance().getHandler();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.instance(), "Conversation of " + address + " deleted successfully!", Toast.LENGTH_SHORT).show();
                                removePerson(getLayoutPosition());
                            }
                        });
                    }
                    else{
                        Log.d(TAG, "SmsViewHolder: run(): Conversation of " + address + " could not be deleted");
                    }
                }
            }).start();
        }
    }
}