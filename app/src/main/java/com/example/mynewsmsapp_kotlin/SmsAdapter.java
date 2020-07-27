package com.example.mynewsmsapp_kotlin;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder>{
    private static final String TAG = " [MY_DEBUG] " + SmsAdapter.class.getSimpleName();

    ArrayList<String> sms_messages_list;
    Context context;
    //    String[] sms_message_string_array;
//    Object[] sms_message_object_array;

    public  SmsAdapter(Context ct, ArrayList<String> array_list){
        context = ct;
        sms_messages_list = array_list;
    }

    public void insert(int position, String new_sms) {
        final String TAG_insert = " insert(): ";
        Log.d(TAG, TAG_insert + " called ");
        sms_messages_list.add(position, new_sms);
        notifyItemInserted(position);
    }

    public void append(Collection new_messages){
        final String TAG_append = " append(): ";
        Log.d(TAG, TAG_append + " called ");
        sms_messages_list.addAll(new_messages);
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
//        sms_message_object_array = sms_messages_list.toArray();
        holder.sms_text.setText(sms_messages_list.get(position).toString());
    }

    @Override
    public int getItemCount() {
//        sms_message_object_array = sms_messages_list.toArray();
        return sms_messages_list.size();
    }

    //inner class
    public class SmsViewHolder extends  RecyclerView.ViewHolder{
        TextView sms_text;
        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            sms_text = itemView.findViewById(R.id.sms_text);
        }
    }
}