package com.filantrop.pvnclient.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;

import java.util.List;


//public class FptnServerAdaptor implements SpinnerAdapter {
//    private List<FptnServer> fptnServerList;
//    private Context context;
//
//    public FptnServerAdaptor(Context context, List<FptnServer> fptnServerList) {
//        this.context = context;
//        this.fptnServerList = fptnServerList;
//    }
//    public void setFptnServerList(List<FptnServer> fptnServerList) {
//        this.fptnServerList = fptnServerList;
//        notifyAll();
////        notifyDataSetChanged();
//    }
//
//    @Override
//    public View getDropDownView(int position, View convertView, ViewGroup parent) {
//        return null;
//    }
//
//    @Override
//    public void registerDataSetObserver(DataSetObserver observer) {
//
//    }
//
//    @Override
//    public void unregisterDataSetObserver(DataSetObserver observer) {
//
//    }
//
//    @Override
//    public int getCount() {
//        return 0;
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return null;
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return 0;
//    }
//
//    @Override
//    public boolean hasStableIds() {
//        return false;
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        return null;
//    }
//
//    @Override
//    public int getItemViewType(int position) {
//        return 0;
//    }
//
//    @Override
//    public int getViewTypeCount() {
//        return 0;
//    }
//
//    @Override
//    public boolean isEmpty() {
//        return false;
//    }

//    @NonNull
//    @Override
//    public FptnServerViewHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_recycler_server_item, parent, false);
//        return new FptnServerViewHodler(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull FptnServerViewHodler holder, int position) {
//        FptnServer server = fptnServerList.get(position);
//        holder.name.setText(server.getName());
//        holder.host.setText(server.getHost());
//        holder.port.setText(String.valueOf(server.getPort()));
//    }
//
//    @Override
//    public int getItemCount() {
//        if (fptnServerList == null) {
//            return 0;
//        }
//        return fptnServerList.size();
//    }
//
//    public class FptnServerViewHodler extends RecyclerView.ViewHolder {
//        private TextView name;
//        private TextView host;
//        private TextView port;
//        public FptnServerViewHodler(@NonNull View itemView) {
//            super(itemView);
//            name = itemView.findViewById(R.id.fptn_server_name);
//            host = itemView.findViewById(R.id.fptn_server_host);
//            port = itemView.findViewById(R.id.fptn_server_port);
//        }
//    }
//}


public class FptnServerAdaptor extends BaseAdapter {
    private List<FptnServer> fptnServerList;
    private Context context;

    public FptnServerAdaptor(Context context, List<FptnServer> fptnServerList) {
        this.context = context;
        this.fptnServerList = fptnServerList;
    }
    public void setFptnServerList(List<FptnServer> fptnServerList) {
        this.fptnServerList = fptnServerList;
        notifyDataSetChanged();
    }

//    @NonNull
//    @Override
//    public FptnServerViewHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_recycler_server_item, parent, false);
//        return new FptnServerViewHodler(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull FptnServerViewHodler holder, int position) {
//        FptnServer server = fptnServerList.get(position);
//        holder.name.setText(server.getName());
//        holder.host.setText(server.getHost());
//        holder.port.setText(String.valueOf(server.getPort()));
//    }

//    @Override
//    public int getItemCount() {
//        if (fptnServerList == null) {
//            return 0;
//        }
//        return fptnServerList.size();
//    }

    @Override
    public int getCount() {
        return fptnServerList != null ? fptnServerList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View view, ViewGroup parent) {
//        View rootView = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.home_recycler_server_item, parent, false);
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_recycler_server_item, parent, false);

        TextView name = itemView.findViewById(R.id.fptn_server_name);
        TextView host = itemView.findViewById(R.id.fptn_server_host);
//        ImageView imgIcon = itemView.findViewById(R.id.fptn_server_arrow_icon);
//        imgIcon.setVisibility(View.GONE);
//        TextView port = itemView.findViewById(R.id.fptn_server_port);
        FptnServer server = fptnServerList.get(position);

        name.setText(server.getName());
        host.setText(server.getHost()); // + ":" + server.getPort());
//        port.setText(String.valueOf(server.getPort()));
        return itemView;
//        return new FptnServerViewHodler(rootView, fptnServerList.get(position));

//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_recycler_server_item, parent, false);
//        return new FptnServerViewHodler(view);
//        return null;
    }

//    public class FptnServerViewHodler extends View {
//        private TextView name;
//        private TextView host;
//        private TextView port;
//        public FptnServerViewHodler(@NonNull View itemView, FptnServer server) {
//            super(itemView.getContext());
//            name = itemView.findViewById(R.id.fptn_server_name);
//            host = itemView.findViewById(R.id.fptn_server_host);
//            port = itemView.findViewById(R.id.fptn_server_port);
//
//            name.setText(server.getName());
//            host.setText(server.getHost());
//            port.setText(String.valueOf(server.getPort()));
//        }
//    }
}


//
//public class FptnServerAdaptor extends RecyclerView.Adapter<FptnServerAdaptor.FptnServerViewHodler> {
//    private List<FptnServer> fptnServerList;
//    private Context context;
//
//    public FptnServerAdaptor(Context context, List<FptnServer> fptnServerList) {
//        this.context = context;
//        this.fptnServerList = fptnServerList;
//    }
//    public void setFptnServerList(List<FptnServer> fptnServerList) {
//        this.fptnServerList = fptnServerList;
//        notifyDataSetChanged();
//    }
//
//    @NonNull
//    @Override
//    public FptnServerViewHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_recycler_server_item, parent, false);
//        return new FptnServerViewHodler(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull FptnServerViewHodler holder, int position) {
//        FptnServer server = fptnServerList.get(position);
//        holder.name.setText(server.getName());
//        holder.host.setText(server.getHost());
//        holder.port.setText(String.valueOf(server.getPort()));
//    }
//
//    @Override
//    public int getItemCount() {
//        if (fptnServerList == null) {
//            return 0;
//        }
//        return fptnServerList.size();
//    }
//
//    public class FptnServerViewHodler extends RecyclerView.ViewHolder {
//        private TextView name;
//        private TextView host;
//        private TextView port;
//        public FptnServerViewHodler(@NonNull View itemView) {
//            super(itemView);
//            name = itemView.findViewById(R.id.fptn_server_name);
//            host = itemView.findViewById(R.id.fptn_server_host);
//            port = itemView.findViewById(R.id.fptn_server_port);
//        }
//    }
//}
