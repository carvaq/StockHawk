package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.text.TextUtils;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.ChartEntry;
import com.sam_chordas.android.stockhawk.data.ChartStockData;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();
    private static String NULL = "null";
    private static String WEIRD_FORMAT_START = "finance_charts_json_callback(";

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(String json) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
        try {
            jsonObject = new JSONObject(json);
            if (jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    safeAddOperation(batchOperations, jsonObject);
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            safeAddOperation(batchOperations, jsonObject);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    private static void safeAddOperation(ArrayList<ContentProviderOperation> batchOperations, JSONObject jsonObject) {
        ContentProviderOperation contentProviderOperation = buildBatchOperation(jsonObject);
        if (contentProviderOperation != null) {
            batchOperations.add(contentProviderOperation);
        }
    }

    private static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format(Locale.getDefault(), "%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    private static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format(Locale.getDefault(), "%.2f", round);
        StringBuilder builder = new StringBuilder(change);
        builder.insert(0, weight);
        builder.append(ampersand);
        change = builder.toString();
        return change;
    }

    private static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        try {
            String bid = jsonObject.getString("Bid");
            String symbol = jsonObject.getString("symbol");

            if (!TextUtils.isEmpty(bid) && !bid.contains(NULL)) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                        QuoteProvider.Quotes.CONTENT_URI);
                String change = jsonObject.getString("Change");

                builder.withValue(QuoteColumns.SYMBOL, symbol.toLowerCase());
                builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(bid));
                builder.withValue(QuoteColumns.PERCENT_CHANGE,
                        truncateChange(jsonObject.getString("ChangeinPercent"), true));
                builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
                builder.withValue(QuoteColumns.ISCURRENT, 1);
                builder.withValue(QuoteColumns.CREATED, System.currentTimeMillis());
                builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
                if (change.charAt(0) == '-') {
                    builder.withValue(QuoteColumns.ISUP, 0);
                } else {
                    builder.withValue(QuoteColumns.ISUP, 1);
                }
                return builder.build();
            } else {
                Log.d(LOG_TAG, "Search with " + symbol + " was not successful");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ChartStockData parseChartJson(String json) {

        String cleanJson;
        if (json.contains(WEIRD_FORMAT_START)) {
            cleanJson = json.substring(WEIRD_FORMAT_START.length(), json.length() - 1);
            Log.d(LOG_TAG, "parseChartJson: " + cleanJson);
        } else {
            cleanJson = json;
        }
        ChartStockData data = null;

        try {
            JSONObject jsonObject = new JSONObject(cleanJson);
            data = new ChartStockData();
            JSONObject meta = jsonObject.getJSONObject("meta");
            data.setCompanyName(meta.getString("Company-Name"));
            data.setPreviousClose(meta.getDouble("previous_close"));
            JSONObject ranges = jsonObject.getJSONObject("ranges");
            JSONObject close = ranges.getJSONObject("close");
            data.setMax(close.getDouble("max"));
            data.setMin(close.getDouble("min"));
            JSONArray series = jsonObject.getJSONArray("series");
            ArrayList<ChartEntry> entries = new ArrayList<>();
            for (int i = 0; i < series.length(); i++) {
                JSONObject index = series.getJSONObject(i);
                entries.add(new ChartEntry(index.getLong("Timestamp"), index.getDouble("close")));
            }
            data.setEntries(entries);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "parseChartJson: ", e);
        }
        return data;
    }
}
