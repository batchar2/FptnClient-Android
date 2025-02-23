package org.fptn.vpnclient.views.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.fptn.vpnclient.R;
import org.fptn.vpnclient.database.model.FptnServerDto;
import org.fptn.vpnclient.utils.CountryFlags;

import java.util.List;

import lombok.Getter;

@Getter
public class FptnServerAdapter extends BaseAdapter {

    @Getter
    private final List<FptnServerDto> fptnServerDtoList;
    private final int layoutViewResourceId;

    public FptnServerAdapter(List<FptnServerDto> fptnServerDtoList, int layoutViewResourceId) {
        this.fptnServerDtoList = fptnServerDtoList;
        this.layoutViewResourceId = layoutViewResourceId;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return fptnServerDtoList != null ? fptnServerDtoList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return fptnServerDtoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return fptnServerDtoList.get(position).id;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(layoutViewResourceId, parent, false);
        }
        FptnServerDto server = fptnServerDtoList.get(position);

        TextView host = view.findViewById(R.id.fptn_server_host);
        if (server.countryCode != null) {
            host.setText(CountryFlags.getCountryFlagByCountryCode(server.countryCode));
            host.setVisibility(VISIBLE);

            if (layoutViewResourceId == R.layout.home_list_recycler_server_item) {
                view.findViewById(R.id.ivCountry).setVisibility(GONE);
            }
        } else {
            host.setVisibility(GONE);

            if (layoutViewResourceId == R.layout.home_list_recycler_server_item) {
                view.findViewById(R.id.ivCountry).setVisibility(VISIBLE);
            }
        }

        TextView name = view.findViewById(R.id.fptn_server_name);
        name.setText(server.name);

        return view;
    }
}
