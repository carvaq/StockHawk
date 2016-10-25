package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.ChartEntry;
import com.sam_chordas.android.stockhawk.data.ChartStockData;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.ChartAsyncTask;
import com.sam_chordas.android.stockhawk.rest.ChartDataReadyListener;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by Caro Vaquero
 * Date: 22.10.2016
 * Project: StockHawk
 */

public class LineGraphFragment extends BaseFragment implements ChartDataReadyListener {
    public static final String EXTRA_SYMBOL_DETAIL = "symbol_selected";
    private static final String EXTRA_STOCK_DATA = "stock_data";

    private static final String[] PROJECTION = new String[]{
            QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.NAME,
            QuoteColumns.ISUP};

    private static final int CURSOR_LOADER_ID = 1;

    private static final int INDEX_COLUMN_BIDPRICE = 0;
    private static final int INDEX_COLUMN_PERCENT_CHANGE = 1;
    private static final int INDEX_COLUMN_CHANGE = 2;
    private static final int INDEX_COLUMN_NAME = 3;
    private static final int INDEX_COLUMN_ISUP = 4;

    private LineChartView mLineChartView;
    private String mSelectedSymbol;
    private ChartStockData mChartStockData;
    private TextView mErrorMessage;
    private View mChartContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedSymbol = getArguments().getString(EXTRA_SYMBOL_DETAIL);
        } else if (savedInstanceState != null) {
            mSelectedSymbol = savedInstanceState.getString(EXTRA_SYMBOL_DETAIL);
            mChartStockData = savedInstanceState.getParcelable(EXTRA_STOCK_DATA);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_line_graph, container, false);
        mErrorMessage = (TextView) view.findViewById(R.id.error_message);
        mChartContainer = view.findViewById(R.id.content_line_graph);

        if (mSelectedSymbol != null) {
            hideErrorMessage();
            mLineChartView = (LineChartView) view.findViewById(R.id.linechart);

            if (mChartStockData == null) {
                if (Helper.isConnected(getActivity())) {
                    new ChartAsyncTask(this).execute(mSelectedSymbol);
                } else {
                    showErrorMessage(R.string.toast_network_required);
                }
            }
            setStockPanelInformation(view);
        } else {
            showErrorMessage(R.string.no_stock_selected);
            view.findViewById(R.id.error_message).setVisibility(View.VISIBLE);
            view.findViewById(R.id.content_line_graph).setVisibility(View.GONE);
        }
        return view;
    }

    private void showErrorMessage(@StringRes int resId) {
        mChartContainer.setVisibility(View.GONE);
        mErrorMessage.setText(resId);
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    private void hideErrorMessage() {
        mChartContainer.setVisibility(View.VISIBLE);
        mErrorMessage.setVisibility(View.GONE);
    }

    private void setStockPanelInformation(View view) {
        Cursor cursor = getActivity().getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                PROJECTION,
                QuoteColumns.ISCURRENT + " = ? AND " + QuoteColumns.SYMBOL + " = ?",
                new String[]{"1", mSelectedSymbol},
                null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(INDEX_COLUMN_NAME);
            String bidPrice = cursor.getString(INDEX_COLUMN_BIDPRICE);
            String percentChange = cursor.getString(INDEX_COLUMN_PERCENT_CHANGE);
            String change = cursor.getString(INDEX_COLUMN_CHANGE);

            getActivity().setTitle(name);
            int isUp = cursor.getInt(INDEX_COLUMN_ISUP);
            TextView viewSymbol = (TextView) view.findViewById(R.id.stock_symbol);
            TextView viewBid = (TextView) view.findViewById(R.id.stock_bid);
            TextView viewChange = (TextView) view.findViewById(R.id.stock_change);
            TextView viewChangePercent = (TextView) view.findViewById(R.id.stock_change_percent);
            if (isUp == 1) {
                viewChange.setBackgroundResource(R.drawable.percent_change_pill_green);
                viewChangePercent.setBackgroundResource(R.drawable.percent_change_pill_green);
            } else {
                viewChange.setBackgroundResource(R.drawable.percent_change_pill_red);
                viewChangePercent.setBackgroundResource(R.drawable.percent_change_pill_red);
            }
            viewSymbol.setText(mSelectedSymbol);
            viewBid.setText(bidPrice);
            viewChange.setText(change);
            viewChangePercent.setText(percentChange);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_SYMBOL_DETAIL, mSelectedSymbol);
        outState.putParcelable(EXTRA_STOCK_DATA, mChartStockData);
    }


    public void onDataReady(ChartStockData data) {
        mChartStockData = data;
        List<PointValue> pointValues = new ArrayList<>(data.getEntries().size());
        for (int i = 0; i < data.getEntries().size(); i ++) {
            ChartEntry chartEntry = data.getEntries().get(i);
            pointValues.add(new PointValue(i, (float) chartEntry.getClose()));
        }
        showStockBidPriceValues(pointValues);
    }

    @Override
    public void onError() {
        showErrorMessage(R.string.general_error);
    }

    private void showStockBidPriceValues(List<PointValue> pointValues) {
        int lineColor;
        int pointsColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lineColor = getResources().getColor(R.color.primary, getActivity().getTheme());
            pointsColor = getResources().getColor(R.color.primary_darker, getActivity().getTheme());
        } else {
            lineColor = getResources().getColor(R.color.primary);
            pointsColor = getResources().getColor(R.color.primary_darker);
        }

        mLineChartView.setViewportCalculationEnabled(false);
        mLineChartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);

        final Viewport v = new Viewport(mLineChartView.getMaximumViewport());
        v.bottom = (float) (mChartStockData.getMin() - 10);
        v.top = (float) (mChartStockData.getMax() + 10);
        v.left = 0;
        v.right = pointValues.size();
        mLineChartView.setMaximumViewport(v);
        mLineChartView.setCurrentViewport(v);

        Line line = new Line(pointValues)
                .setColor(lineColor)
                .setHasLabels(false)
                .setHasLabelsOnlyForSelected(true)
                .setPointColor(pointsColor)
                .setHasPoints(true);

        LineChartData data = new LineChartData();

        List<Line> lines = new ArrayList<>(1);
        lines.add(line);
        data.setLines(lines);

        Axis axisY = new Axis().setHasLines(true);
        data.setAxisYLeft(axisY);
        mLineChartView.setLineChartData(data);
    }

}
