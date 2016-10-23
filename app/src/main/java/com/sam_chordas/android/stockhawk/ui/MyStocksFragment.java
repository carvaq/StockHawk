package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

/**
 * Created by Caro Vaquero
 * Date: 22.10.2016
 * Project: StockHawk
 */

public class MyStocksFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        QuoteCursorAdapter.OnItemClickListener {
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private static final String BTN_ADD_ID = "add-id";
    private static final String BTN_SEARCH_ID = "search-id";
    private static final String BTN_SEARCH_NAME = "search-stock";
    private static final String BTN_ADD_NAME = "add";
    private static final String STOCK_ID = "stock-id";
    private static final String TYPE_BUTTON = "button";
    private static final String TYPE_STOCK = "stock";

    private Intent mServiceIntent;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private SharedPreferences mDefaultSharedPreferences;
    private Cursor mLatestCursor;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(getActivity(), StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra(StockIntentService.EXTRA_TAG, StockIntentService.TAG_INIT);
            if (Helper.isConnected(getActivity())) {
                getActivity().startService(mServiceIntent);
            } else {
                networkToast();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_my_stocks, container, false);
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        boolean isPercent = mDefaultSharedPreferences.getBoolean(getString(R.string.pref_unit_key), true);
        mCursorAdapter = new QuoteCursorAdapter(getActivity(), view.findViewById(R.id.no_stocks_message),
                isPercent, this);
        recyclerView.setAdapter(mCursorAdapter);


        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logEvent(BTN_ADD_ID, BTN_ADD_NAME, TYPE_BUTTON);
                if (Helper.isConnected(getActivity())) {
                    showSearchDialog();
                } else {
                    networkToast();
                }
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        AdView mAdView = (AdView) view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        return view;
    }


    private void showSearchDialog() {
        new MaterialDialog.Builder(getActivity())
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
                        Cursor c = getActivity().getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                new String[]{input.toString()}, null);
                        if (c != null && c.getCount() != 0) {
                            showCenterMessageToUser(R.string.toast_stock_already_saved);
                        } else {
                            // Add the stock to DB
                            mServiceIntent.putExtra(StockIntentService.EXTRA_TAG,
                                    StockIntentService.TAG_ADD);
                            mServiceIntent.putExtra(StockIntentService.EXTRA_SYMBOL, input.toString());
                            getActivity().startService(mServiceIntent);
                            logSearch(input.toString());
                        }
                        if (c != null) {
                            c.close();
                        }
                        logEvent(BTN_SEARCH_ID, BTN_SEARCH_NAME, TYPE_BUTTON);
                    }
                })
                .show();
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
        Toast.makeText(getActivity(), getString(messageResId), Toast.LENGTH_SHORT).show();
    }

    private void showCenterMessageToUser(@StringRes int messageResId) {
        Toast toast =
                Toast.makeText(getActivity(), messageResId,
                        Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
        toast.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.my_stocks, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_change_units) {
            mCursorAdapter.toggleUnit();

            boolean isPercent = mDefaultSharedPreferences.getBoolean(getString(R.string.pref_unit_key), true);
            mDefaultSharedPreferences
                    .edit()
                    .putBoolean(getString(R.string.pref_unit_key), !isPercent)
                    .apply();
            getActivity().getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClicked(QuoteCursorAdapter.ViewHolder vh, int position) {
        Cursor cursor = mCursorAdapter.getCursor();
        cursor.moveToPosition(position);
        String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));

        Pair<View, String> pair = new Pair<View, String>(
                vh.symbol, getString(R.string.transition_name_symbol));
        ActivityOptionsCompat activityOptions =
                ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), pair);

        ((OnStockSelectionListener) getActivity()).onStockSelected(symbol, activityOptions.toBundle());
        logEvent(STOCK_ID + position, symbol, TYPE_STOCK);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(getActivity(), QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //so we don't get the ISCURRENT = 0 updates
        if (mLatestCursor == null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCursorAdapter.swapCursor(mLatestCursor);
                    mLatestCursor = null;
                }
            }, 200);
        }
        mLatestCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    public interface OnStockSelectionListener {
        void onStockSelected(String symbol, Bundle activityOptions);
    }
}
