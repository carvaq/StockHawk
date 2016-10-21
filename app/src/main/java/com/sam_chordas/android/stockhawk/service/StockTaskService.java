package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private static final String YAHOO_BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private static final String YAHOO_SELECT_STATEMENT = "select * from yahoo.finance.quotes where symbol in (";
    private static final String UTF_8 = "UTF-8";
    private static final String URL_FINAL_PART = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append(YAHOO_BASE_URL);
            urlStringBuilder.append(URLEncoder.encode(YAHOO_SELECT_STATEMENT, UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StockIntentService.TAG_INIT.equals(params.getTag()) ||
                StockIntentService.TAG_PERIODIC.equals(params.getTag())) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                // Init task. Populates DB with quotes for the symbols seen below
                addUrlEncodedPath(urlStringBuilder, "\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")");
            } else {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    int symbolIndex = initQueryCursor.getColumnIndex(StockIntentService.EXTRA_SYMBOL);
                    mStoredSymbols.append("\"")
                            .append(initQueryCursor.getString(symbolIndex))
                            .append("\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                addUrlEncodedPath(urlStringBuilder, mStoredSymbols.toString());
            }
        } else if (params.getTag().equals(StockIntentService.TAG_ADD)) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString(StockIntentService.EXTRA_SYMBOL);
            addUrlEncodedPath(urlStringBuilder, "\"" + stockInput + "\")");
        }
        // finalize the URL for the API query.
        urlStringBuilder.append(URL_FINAL_PART);

        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        String urlString = urlStringBuilder.toString();
        try {
            getResponse = fetchData(urlString);
            result = GcmNetworkManager.RESULT_SUCCESS;
            insertResultsInDatabase(getResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void insertResultsInDatabase(String response) {
        try {
            ContentValues contentValues = new ContentValues();
            // update ISCURRENT to 0 (false) so new data is current
            if (isUpdate) {
                contentValues.put(QuoteColumns.ISCURRENT, 0);
                mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                        null, null);
            }
            ArrayList<ContentProviderOperation> operations = Utils.quoteJsonToContentVals(response);
            if (!operations.isEmpty()) {
                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, operations);
            }
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(LOG_TAG, "Error applying batch insert", e);
        }
    }

    private void addUrlEncodedPath(StringBuilder urlStringBuilder, String textToBeEncoded) {
        try {
            urlStringBuilder.append(URLEncoder.encode(textToBeEncoded, UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
