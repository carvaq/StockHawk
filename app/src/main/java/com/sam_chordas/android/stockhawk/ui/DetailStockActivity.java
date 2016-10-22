package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.sam_chordas.android.stockhawk.R;

public class DetailStockActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_stock);

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            String selectedSymbol = getIntent().getStringExtra(LineGraphFragment.EXTRA_SYMBOL_DETAIL);
            Bundle arguments = new Bundle();
            arguments.putString(LineGraphFragment.EXTRA_SYMBOL_DETAIL, selectedSymbol);

            LineGraphFragment fragment = new LineGraphFragment();
            fragment.setArguments(arguments);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
