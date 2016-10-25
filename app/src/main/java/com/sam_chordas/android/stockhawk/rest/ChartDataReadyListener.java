package com.sam_chordas.android.stockhawk.rest;

import com.sam_chordas.android.stockhawk.data.ChartStockData;

/**
 * Created by Caro Vaquero
 * Date: 25.10.2016
 * Project: StockHawk
 */

public interface ChartDataReadyListener {
    void onDataReady(ChartStockData data);

    void onError();
}
