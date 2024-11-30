package com.filantrop.pvnclient.repository;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;

import java.util.List;

public class FptnServerAdaptor extends RecyclerView.Adapter<FptnServerAdaptor.FptnServerViewHodler> {
    private List<FptnServer> fptnServerList;

    public void setFptnServerList(List<FptnServer> fptnServerList) {
        this.fptnServerList = fptnServerList;
    }

    @NonNull
    @Override
    public FptnServerViewHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.each_server_item, parent, false);
        return new FptnServerViewHodler(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FptnServerViewHodler holder, int position) {
        FptnServer server = fptnServerList.get(position);
        holder.name.setText(server.getName());
        holder.host.setText(server.getHost());
        holder.port.setText(String.valueOf(server.getPort()));
    }

    @Override
    public int getItemCount() {
        if (fptnServerList == null) {
            return 0;
        }
        return fptnServerList.size();
    }


    public class FptnServerViewHodler extends  RecyclerView.ViewHolder {
        private TextView name;
        private TextView host;
        private TextView port;
        public FptnServerViewHodler(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.fptn_server_name);
            host = itemView.findViewById(R.id.fptn_server_host);
            port = itemView.findViewById(R.id.fptn_server_port);
        }
    }
}
