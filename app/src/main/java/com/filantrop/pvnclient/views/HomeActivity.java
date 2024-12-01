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
import com.filantrop.pvnclient.repository.FptnServerRepo;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FptnServerAdaptor adaptor;
    private List<FptnServer> fptnServerList;
    private FptnServerViewModel fptnViewModel = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        intializeVariable();
    }
    private void intializeVariable() {
        adaptor = new FptnServerAdaptor(this, fptnServerList);
        adaptor.setFptnServerList(fptnServerList);

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getAllServersLiveData().observe(this, new Observer<List<FptnServer>>() {
            @Override
            public void onChanged(List<FptnServer> servers) {
                if(servers != null) {
                    fptnServerList = servers;
                    adaptor.setFptnServerList(fptnServerList);
                }
            }
        });
        recyclerView = findViewById(R.id.servers_list_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setAdapter(adaptor);


//        fptnViewModel.makeAPIcall();
//        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
////        fptnServerList =
////        repo = new FptnServerRepo()
//        fptnServerList = new ArrayList<>();
////        fptnServerList = fptnViewModel.getFptnServerRepo().getAllServersLiveData().getValue();
//        fptnServerList.add(new FptnServer("server1", "username", "password", "127.0.0.1", 443));
//        fptnServerList.add(new FptnServer("server2", "username", "password", "127.0.0.1", 443));
//        fptnServerList.add(new FptnServer("server3", "username", "password", "127.0.0.1", 443));
//
//        adaptor = new FptnServerAdaptor();
//        adaptor.setFptnServerList(fptnServerList);
//
//        recyclerView = findViewById(R.id.servers_list_recycler_view);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
//        recyclerView.setAdapter(adaptor);
    }
}
