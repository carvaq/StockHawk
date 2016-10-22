package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by Caro Vaquero
 * Date: 22.10.2016
 * Project: StockHawk
 */

public class LineGraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_SYMBOL_DETAIL = "symbol_selected";

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedSymbol = getArguments().getString(EXTRA_SYMBOL_DETAIL);
        } else if (savedInstanceState != null) {
            mSelectedSymbol = savedInstanceState.getString(EXTRA_SYMBOL_DETAIL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_line_graph, container, false);
        if (mSelectedSymbol != null) {
            view.findViewById(R.id.nothing_selected_yet).setVisibility(View.GONE);
            view.findViewById(R.id.content_line_graph).setVisibility(View.VISIBLE);
            mLineChartView = (LineChartView) view.findViewById(R.id.linechart);
            getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

            setStockPanelInformation(view);
        } else {
            view.findViewById(R.id.nothing_selected_yet).setVisibility(View.VISIBLE);
            view.findViewById(R.id.content_line_graph).setVisibility(View.GONE);
        }
        return view;
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
    public void onResume() {
        super.onResume();
        if (mSelectedSymbol != null) {
            getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_SYMBOL_DETAIL, mSelectedSymbol);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), QuoteProvider.Quotes.CONTENT_URI,
                PROJECTION,
                QuoteColumns.SYMBOL + " = ?",
                new String[]{mSelectedSymbol}
                , null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() > 0) {
            float max = Float.MIN_VALUE;
            float min = Float.MAX_VALUE;
            int x = 0;

            List<PointValue> values = new ArrayList<>(data.getCount());
            data.moveToFirst();
            do {
                float value = Float.parseFloat(data.getString(INDEX_COLUMN_BIDPRICE));
                if (value > max) max = value;
                else if (value < min) min = value;
                values.add(new PointValue(x, value));
                x++;
            } while (data.moveToNext());

            //This is done so that the line doesn't stick to an edge
            max += 10;
            min -= 10;

            showStockBidPriceValues(values, (int) Math.floor(min), (int) Math.ceil(max));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLineChartView.setLineChartData(new LineChartData());
    }

    private void showStockBidPriceValues(List<PointValue> pointValues, int min, int max) {
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
        v.bottom = min;
        v.top = max;
        v.left = 0;
        v.right = pointValues.size();
        mLineChartView.setMaximumViewport(v);
        mLineChartView.setCurrentViewport(v);

        Line line = new Line(pointValues)
                .setColor(lineColor)
                .setHasLabels(false)
                .setShape(ValueShape.DIAMOND)
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
