package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

public class MainActivity extends BaseActivity implements MyStocksFragment.OnStockSelectionListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private boolean mTwoPane;
    private static final String GRAPH_FRAGMENT_TAG = "graph_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (findViewById(R.id.fragment_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                LineGraphFragment fragment = new LineGraphFragment();
                String selectedSymbol = getIntent().getStringExtra(LineGraphFragment.EXTRA_SYMBOL_DETAIL);

                if (selectedSymbol != null) {
                    Bundle arguments = new Bundle();
                    arguments.putString(LineGraphFragment.EXTRA_SYMBOL_DETAIL, selectedSymbol);
                    fragment.setArguments(arguments);
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment, GRAPH_FRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        if (Helper.isConnected(this)) {
            startPeriodicUpdateTask();
        }
    }


    private void startPeriodicUpdateTask() {
        long period = 3600L;
        long flex = 10L;
        // create a periodic task to pull stocks once every hour after the app has been opened. This
        // is so Widget data stays up to date.
        PeriodicTask periodicTask = new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(StockIntentService.TAG_PERIODIC)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();
        // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
        // are updated.
        GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }

    @Override
    public void onStockSelected(String symbol, Bundle activityOptions) {
        if (mTwoPane) {
            LineGraphFragment fragment = new LineGraphFragment();
            Bundle arguments = new Bundle();
            arguments.putString(LineGraphFragment.EXTRA_SYMBOL_DETAIL, symbol);
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, GRAPH_FRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(MainActivity.this, DetailStockActivity.class);
            intent.putExtra(LineGraphFragment.EXTRA_SYMBOL_DETAIL, symbol);
            ActivityCompat.startActivity(MainActivity.this, intent, activityOptions);
        }
    }
}
