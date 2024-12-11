package com.filantrop.pvnclient.repository;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServerDto;

import java.util.List;

import lombok.Getter;

public class FptnServerAdapter extends BaseAdapter {

    @Getter
    private List<FptnServerDto> fptnServerDtoList;

    public void setFptnServerDtoList(List<FptnServerDto> fptnServerDtoList) {
        this.fptnServerDtoList = fptnServerDtoList;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return fptnServerDtoList != null ? fptnServerDtoList.size() : 0;
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
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_recycler_server_item, parent, false);
        }
        FptnServerDto server = fptnServerDtoList.get(position);

        TextView host = view.findViewById(R.id.fptn_server_host);
        host.setText(server.getHost());

        TextView name = view.findViewById(R.id.fptn_server_name);
        name.setText(server.getName());

        return view;
    }
}
