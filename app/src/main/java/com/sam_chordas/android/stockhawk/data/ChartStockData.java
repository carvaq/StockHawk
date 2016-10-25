
package com.sam_chordas.android.stockhawk.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Caro Vaquero
 * Date: 25.10.2016
 * Project: StockHawk
 */
public class ChartStockData implements Parcelable {

    private String mCompanyName;
    private Double mPreviousClose;
    private Double mMin;
    private Double mMax;
    private ArrayList<ChartEntry> mEntries;

    protected ChartStockData(Parcel in) {
        mCompanyName = in.readString();
        mPreviousClose = in.readDouble();
        mMin = in.readDouble();
        mMax = in.readDouble();
        mEntries = in.createTypedArrayList(ChartEntry.CREATOR);
    }

    public ChartStockData() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCompanyName);
        dest.writeDouble(mPreviousClose);
        dest.writeDouble(mMin);
        dest.writeDouble(mMax);
        dest.writeTypedList(mEntries);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChartStockData> CREATOR = new Creator<ChartStockData>() {
        @Override
        public ChartStockData createFromParcel(Parcel in) {
            return new ChartStockData(in);
        }

        @Override
        public ChartStockData[] newArray(int size) {
            return new ChartStockData[size];
        }
    };

    public String getCompanyName() {
        return mCompanyName;
    }

    public void setCompanyName(String companyName) {
        mCompanyName = companyName;
    }

    public Double getPreviousClose() {
        return mPreviousClose;
    }

    public void setPreviousClose(Double previousClose) {
        mPreviousClose = previousClose;
    }

    public Double getMin() {
        return mMin;
    }

    public void setMin(Double min) {
        mMin = min;
    }

    public Double getMax() {
        return mMax;
    }

    public void setMax(Double max) {
        mMax = max;
    }

    public ArrayList<ChartEntry> getEntries() {
        return mEntries;
    }

    public void setEntries(ArrayList<ChartEntry> entries) {
        mEntries = entries;
    }
}