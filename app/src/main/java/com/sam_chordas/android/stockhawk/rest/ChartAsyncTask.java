package com.sam_chordas.android.stockhawk.rest;

import android.os.AsyncTask;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.ChartStockData;
import com.sam_chordas.android.stockhawk.ui.LineGraphFragment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Caro Vaquero
 * Date: 25.10.2016
 * Project: StockHawk
 */

public class ChartAsyncTask extends AsyncTask<String, Void, ChartStockData> {
    private final String CHART_URL = "http://chartapi.finance.yahoo.com/instrument/1.0/%s/chartdata;type=close;range=1d/json";

    private ChartDataReadyListener mChartDataReadyListener;

    public ChartAsyncTask(ChartDataReadyListener chartDataReadyListener) {
        mChartDataReadyListener = chartDataReadyListener;
    }

    @Override
    protected ChartStockData doInBackground(String... params) {
        ChartStockData chartStockData = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(String.format(CHART_URL, params[0]));
            urlConnection = (HttpURLConnection) url.openConnection();

            int code = urlConnection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                StringBuilder result = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                chartStockData = Utils.parseChartJson(result.toString());
            }
        } catch (Exception e) {
            Log.e(LineGraphFragment.class.getSimpleName(), "doInBackground: ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return chartStockData;
    }

    @Override
    protected void onPostExecute(ChartStockData data) {
        super.onPostExecute(data);
        if (data != null) {
            mChartDataReadyListener.onDataReady(data);
        } else {
            mChartDataReadyListener.onError();
        }
    }
}
