package com.filantrop.pvnclient.views;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerAdaptor;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<FptnServer> fptnServerList;
    private FptnServerAdaptor adaptor;

    private FptnServerViewModel fptnViewModel = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        intializeVariable();
    }
    private void intializeVariable() {
        fptnServerList = new ArrayList<>();
        fptnServerList.add(new FptnServer("server1", "username", "password", "127.0.0.1", 443));
        fptnServerList.add(new FptnServer("server2", "username", "password", "127.0.0.1", 443));
        fptnServerList.add(new FptnServer("server3", "username", "password", "127.0.0.1", 443));

        adaptor = new FptnServerAdaptor();
        adaptor.setFptnServerList(fptnServerList);

        recyclerView = findViewById(R.id.servers_list_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setAdapter(adaptor);
    }
}
