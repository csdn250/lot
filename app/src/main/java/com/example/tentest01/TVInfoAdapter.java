package com.example.tentest01;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TVInfoAdapter extends RecyclerView.Adapter<TVInfoAdapter.MyViewHolder> {
    private List<String> listInfo;
    private Context mContext;

    public TVInfoAdapter(List<String> listInfo, Context mContext) {
        this.listInfo = listInfo;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        MyViewHolder holder = new MyViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.tvInfo.setText(listInfo.get(position));
    }

    @Override
    public int getItemCount() {
        return listInfo == null ? 0 : listInfo.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder
    {
        private TextView tvInfo;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInfo=itemView.findViewById(R.id.tvInfo);
        }
    }
}
