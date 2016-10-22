package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

public class LineGraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        mSelectedSymbol = savedInstanceState != null ? savedInstanceState.getString(EXTRA_SYMBOL_DETAIL) :
                getIntent().getStringExtra(EXTRA_SYMBOL_DETAIL);

        mLineChartView = (LineChartView) findViewById(R.id.linechart);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        setHeaderInformation();
    }

    private void setHeaderInformation() {
        Cursor cursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                PROJECTION,
                QuoteColumns.ISCURRENT + " = ? AND " + QuoteColumns.SYMBOL + " = ?",
                new String[]{"1", mSelectedSymbol},
                null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(INDEX_COLUMN_NAME);
            String bidPrice = cursor.getString(INDEX_COLUMN_BIDPRICE);
            String percentChange = cursor.getString(INDEX_COLUMN_PERCENT_CHANGE);
            String change = cursor.getString(INDEX_COLUMN_CHANGE);

            getSupportActionBar().setTitle(name);
            int isUp = cursor.getInt(INDEX_COLUMN_ISUP);
            TextView viewSymbol = (TextView) findViewById(R.id.stock_symbol);
            TextView viewBid = (TextView) findViewById(R.id.stock_bid);
            TextView viewChange = (TextView) findViewById(R.id.stock_change);
            TextView viewChangePercent = (TextView) findViewById(R.id.stock_change_percent);
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
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_SYMBOL_DETAIL, mSelectedSymbol);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                PROJECTION,
                QuoteColumns.SYMBOL + " = ?",
                new String[]{mSelectedSymbol}
                , null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() > 0) {
            float max = 0;
            float min = 0;
            int maxSetCount = 0;
            int minSetCount = 0;

            data.moveToFirst();

            List<PointValue> values = new ArrayList<>();
            int x = 0;
            do {
                float value = Float.parseFloat(data.getString(INDEX_COLUMN_BIDPRICE));
                if (value > max) {
                    max = value;
                    maxSetCount++;
                } else if (value < min) {
                    min = value;
                    minSetCount++;
                }
                values.add(new PointValue(x, value));
                x++;
            } while (data.moveToNext());

            if (maxSetCount < 3) {
                max *= 2;
            } else if (minSetCount < 3) {
                min *= 2;
            }

            showData(values, (int) Math.floor(min), (int) Math.ceil(max));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLineChartView.setLineChartData(new LineChartData());
    }

    private void showData(List<PointValue> pointValues, int min, int max) {
        int lineColor;
        int pointsColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lineColor = getColor(R.color.primary);
            pointsColor = getColor(R.color.primary_darker);
        } else {
            lineColor = getResources().getColor(R.color.primary);
            pointsColor = getResources().getColor(R.color.primary_darker);
        }

        final Viewport v = new Viewport(mLineChartView.getMaximumViewport());
        v.bottom = min;
        v.top = max;
        v.left = 0;
        v.right = pointValues.size();
        mLineChartView.setMaximumViewport(v);
        mLineChartView.setCurrentViewport(v);

        mLineChartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        mLineChartView.setScrollEnabled(true);

        Line line = new Line(pointValues)
                .setColor(lineColor)
                .setHasLabels(false)
                .setShape(ValueShape.DIAMOND)
                .setHasLabelsOnlyForSelected(true)
                .setPointColor(pointsColor)
                .setHasPoints(true);

        List<Line> lines = new ArrayList<>();
        lines.add(line);

        LineChartData data = new LineChartData();
        data.setLines(lines);

        Axis axisY = new Axis().setHasLines(true);
        data.setAxisYLeft(axisY);

        mLineChartView.setViewportCalculationEnabled(false);

        mLineChartView.setLineChartData(data);
    }
}
