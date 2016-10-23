package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.LineGraphFragment;


public class MyStocksRemoteViewsService extends RemoteViewsService {

    public MyStocksRemoteViewsService() {
    }


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MyStockRemoteViewsFactory();
    }

    private class MyStockRemoteViewsFactory implements RemoteViewsFactory {

        private final String[] PROJECTION = new String[]{
                QuoteColumns._ID,
                QuoteColumns.SYMBOL,
                QuoteColumns.PERCENT_CHANGE,
                QuoteColumns.CHANGE,
                QuoteColumns.ISUP};

        private Cursor mCursor;
        private int mIsUpColor;
        private int mIsDownColor;

        @Override
        public void onCreate() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mIsUpColor = getResources().getColor(R.color.material_green_700, getTheme());
                mIsDownColor = getResources().getColor(R.color.material_red_700, getTheme());
            } else {
                mIsUpColor = getResources().getColor(R.color.material_green_700);
                mIsDownColor = getResources().getColor(R.color.material_red_700);
            }
        }

        @Override
        public void onDataSetChanged() {

            final long identityToken = Binder.clearCallingIdentity();
            mCursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    PROJECTION,
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
        }

        @Override
        public int getCount() {
            return mCursor != null ? mCursor.getCount() : 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (mCursor == null || !mCursor.moveToPosition(position)) return null;

            boolean isPercent = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getBoolean(getString(R.string.pref_unit_key), true);

            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_list_item);

            String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
            remoteViews.setTextViewText(R.id.widget_stock_symbol, symbol);

            if (mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.ISUP)) == 1) {
                remoteViews.setTextColor(R.id.widget_change, mIsUpColor);
            } else {
                remoteViews.setTextColor(R.id.widget_change, mIsDownColor);
            }
            if (isPercent) {
                remoteViews.setTextViewText(R.id.widget_change,
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
            } else {
                remoteViews.setTextViewText(R.id.widget_change,
                        mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE)));
            }
            Intent fillIntent = new Intent();
            fillIntent.putExtra(LineGraphFragment.EXTRA_SYMBOL_DETAIL, symbol);
            remoteViews.setOnClickFillInIntent(R.id.widget_row, fillIntent);
            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            if (mCursor.moveToPosition(position))
                return mCursor.getLong(0);
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
