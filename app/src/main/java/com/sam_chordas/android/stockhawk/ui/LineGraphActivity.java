package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.renderer.AxisRenderer;
import com.db.chart.view.ChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;


public class LineGraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_SYMBOL_DETAIL = "symbol_selected";

    private static final String[] PROJECTION = new String[]{
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.CREATED,
            QuoteColumns.ISUP};

    private static final int CURSOR_LOADER_ID = 1;
    private static final int INDEX_COLUMN__ID = 0;
    private static final int INDEX_COLUMN_SYMBOL = 1;
    private static final int INDEX_COLUMN_BIDPRICE = 2;
    private static final int INDEX_COLUMN_PERCENT_CHANGE = 3;
    private static final int INDEX_COLUMN_CHANGE = 4;
    private static final int INDEX_COLUMN_CREATED = 5;
    private static final int INDEX_COLUMN_ISUP = 6;

    private ChartView mLineChartView;
    private String mSelectedSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        mSelectedSymbol = savedInstanceState != null ? savedInstanceState.getString(EXTRA_SYMBOL_DETAIL) :
                getIntent().getStringExtra(EXTRA_SYMBOL_DETAIL);

        mLineChartView = (ChartView) findViewById(R.id.linechart);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mSelectedSymbol.toUpperCase());
        }

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
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
                new String[]{mSelectedSymbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() > 0) {
            int numberOfDataSets = data.getCount();
            String[] labels = new String[numberOfDataSets];
            float[] values = new float[numberOfDataSets];
            float max = 0;
            float min = 0;
            int maxSetCount = 0;
            int minSetCount = 0;

            data.moveToFirst();
            while (!data.isAfterLast()) {
                int position = data.getPosition();
                String creationDate = data.getString(INDEX_COLUMN_CREATED);
                labels[position] = creationDate;
                float value = Float.parseFloat(data.getString(INDEX_COLUMN_BIDPRICE));
                if (value > max) {
                    max = value;
                    maxSetCount++;
                } else if (value < min) {
                    min = value;
                    minSetCount++;
                }
                values[position] = value;
                data.moveToNext();
            }

            if (maxSetCount < 3) {
                max *= 2;
            } else if (minSetCount < 3) {
                min *= 2;
            }

            showData(labels, values, (int) Math.floor(min), (int) Math.ceil(max));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLineChartView.dismiss();
    }

    private void showData(String[] mLabels, float[] values, int min, int max) {
        int line;
        int points;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            line = getColor(R.color.primary);
            points = getColor(R.color.primary_darker);
        } else {
            line = getResources().getColor(R.color.primary);
            points = getResources().getColor(R.color.primary_darker);
        }

        LineSet dataset = new LineSet(mLabels, values);
        dataset.setColor(line)
                .setThickness(Tools.fromDpToPx(2))
                .setSmooth(true);
        for (int i = 0; i < mLabels.length; i += 5) {
            Point point = (Point) dataset.getEntry(i);
            point.setColor(points);
        }
        mLineChartView.addData(dataset);

        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.WHITE);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);
        gridPaint.setStrokeWidth(Tools.fromDpToPx(.75f));

        mLineChartView.setBorderSpacing(Tools.fromDpToPx(0))
                .setLabelsColor(Color.WHITE)
                .setXLabels(AxisRenderer.LabelPosition.NONE)
                .setYLabels(AxisRenderer.LabelPosition.OUTSIDE)
                .setGrid(ChartView.GridType.HORIZONTAL, 7, 1, gridPaint)
                .setAxisBorderValues(min, max);

        mLineChartView.show();
    }
}
