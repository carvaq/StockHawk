package com.sam_chordas.android.stockhawk.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Caro Vaquero
 * Date: 25.10.2016
 * Project: StockHawk
 */
public class ChartEntry implements Parcelable{
    private long mTimestamp;
    private double mClose;

    public ChartEntry(Long timestamp, Double close) {
        this.mTimestamp = timestamp;
        this.mClose = close;
    }

    protected ChartEntry(Parcel in) {
        mTimestamp = in.readLong();
        mClose = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTimestamp);
        dest.writeDouble(mClose);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChartEntry> CREATOR = new Creator<ChartEntry>() {
        @Override
        public ChartEntry createFromParcel(Parcel in) {
            return new ChartEntry(in);
        }

        @Override
        public ChartEntry[] newArray(int size) {
            return new ChartEntry[size];
        }
    };

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        this.mTimestamp = timestamp;
    }

    public double getClose() {
        return mClose;
    }

    public void setClose(double close) {
        this.mClose = close;
    }
}
