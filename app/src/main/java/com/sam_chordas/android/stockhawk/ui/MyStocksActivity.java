package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private Intent mServiceIntent;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private SharedPreferences mDefaultSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        setContentView(R.layout.activity_my_stocks);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra(StockIntentService.EXTRA_TAG, StockIntentService.TAG_INIT);
            if (isConnected()) {
                startService(mServiceIntent);
            } else {
                networkToast();
            }
        }
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        boolean isPercent = mDefaultSharedPreferences.getBoolean(getString(R.string.pref_unit_key), true);
        mCursorAdapter = new QuoteCursorAdapter(this, findViewById(R.id.no_stocks_message), isPercent,
                new QuoteCursorAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClicked(QuoteCursorAdapter.ViewHolder vh, int position) {
                        Cursor cursor = mCursorAdapter.getCursor();
                        cursor.moveToPosition(position);
                        String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));

                        Intent intent = new Intent(mContext, LineGraphActivity.class);
                        intent.putExtra(LineGraphActivity.EXTRA_SYMBOL_DETAIL, symbol);

                        Pair<View, String> pair = new Pair<View, String>(
                                vh.symbol, getString(R.string.transition_name_symbol));
                        ActivityOptionsCompat activityOptions =
                                ActivityOptionsCompat.makeSceneTransitionAnimation(MyStocksActivity.this, pair);
                        ActivityCompat.startActivity(MyStocksActivity.this, intent, activityOptions.toBundle());
                    }
                });

        recyclerView.setAdapter(mCursorAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //logEvent("", name, type);
                if (isConnected()) {
                    showSearchDialog();
                } else {
                    networkToast();
                }
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        if (isConnected()) {
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

    private void showSearchDialog() {
        new MaterialDialog.Builder(mContext)
                .title(R.string.symbol_search)
                .content(R.string.content_test)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .negativeText(android.R.string.cancel)
                .input(R.string.input_hint, R.string.input_prefill, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        // On FAB click, receive user input. Make sure the stock doesn't already exist
                        // in the DB and proceed accordingly
                        onInputAdded(input);
                    }

                    private void onInputAdded(CharSequence input) {
                        Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                new String[]{input.toString()}, null);
                        if (c != null && c.getCount() != 0) {
                            showCenterMessageToUser(R.string.toast_stock_already_saved);
                        } else {
                            // Add the stock to DB
                            mServiceIntent.putExtra(StockIntentService.EXTRA_TAG,
                                    StockIntentService.TAG_ADD);
                            mServiceIntent.putExtra(StockIntentService.EXTRA_SYMBOL, input.toString());
                            startService(mServiceIntent);
                        }
                        if (c != null) {
                            c.close();
                        }
                    }
                })
                .show();
    }


    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void networkToast() {
        showShortMessageToUser(R.string.toast_network_required);
    }

    private void showShortMessageToUser(@StringRes int messageResId) {
        Toast.makeText(mContext, getString(messageResId), Toast.LENGTH_SHORT).show();
    }

    private void showCenterMessageToUser(@StringRes int messageResId) {
        Toast toast =
                Toast.makeText(MyStocksActivity.this, messageResId,
                        Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            mCursorAdapter.toggleUnit();

            boolean isPercent = mDefaultSharedPreferences.getBoolean(getString(R.string.pref_unit_key), true);
            mDefaultSharedPreferences
                    .edit()
                    .putBoolean(getString(R.string.pref_unit_key), !isPercent)
                    .apply();
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}
