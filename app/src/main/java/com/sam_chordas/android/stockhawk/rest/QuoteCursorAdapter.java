package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

/**
 * Created by sam_chordas on 10/6/15.
 * Credit to skyfishjy gist:
 * https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private static Typeface robotoLight;
    private Context mContext;
    private boolean mIsPercent;
    private View mEmptyView;
    private OnItemClickListener mOnItemClickListener;
    private static final String TRANSITION_RUNNER = "transition_symbol_%s";
    private static final String CONTENT_DESCRIPTION = "%s: %s";
    private String CD_CHANGE;
    private String CD_CHANGE_PERCENT;
    private String CD_BID;
    private String CD_SYMBOL;

    public QuoteCursorAdapter(Context context, View emptyView, boolean isPercent, OnItemClickListener listener) {
        super(context, null);
        mContext = context;
        this.mEmptyView = emptyView;
        this.mIsPercent = isPercent;
        this.mOnItemClickListener = listener;
        robotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");
        CD_CHANGE = context.getString(R.string.label_change);
        CD_CHANGE_PERCENT = context.getString(R.string.label_change_in_percent);
        CD_BID = context.getString(R.string.label_bid_price);
        CD_SYMBOL = context.getString(R.string.label_symbol);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_quote, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));
        viewHolder.symbol.setText(symbol);
        viewHolder.symbol.setContentDescription(getContentDescription(CD_SYMBOL, symbol));
        String bidPrice = cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE));
        viewHolder.bidPrice.setText(bidPrice);
        viewHolder.bidPrice.setContentDescription(getContentDescription(CD_BID, bidPrice));


        if (cursor.getInt(cursor.getColumnIndex(QuoteColumns.ISUP)) == 1) {
            viewHolder.change.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            viewHolder.change.setBackgroundResource(R.drawable.percent_change_pill_red);
        }
        if (mIsPercent) {
            String changeInPercent = cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
            viewHolder.change.setText(changeInPercent);
            viewHolder.change.setContentDescription(getContentDescription(CD_CHANGE_PERCENT, changeInPercent));
            viewHolder.change.setContentDescription(getContentDescription(CD_CHANGE_PERCENT, changeInPercent));
        } else {
            String change = cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE));
            viewHolder.change.setText(change);
            viewHolder.change.setContentDescription(getContentDescription(CD_CHANGE, change));
        }
        ViewCompat.setTransitionName(viewHolder.symbol, String.format(TRANSITION_RUNNER, cursor.getPosition()));
    }

    private String getContentDescription(String caption, String value) {
        return String.format(CONTENT_DESCRIPTION, caption, value);
    }

    @Override
    public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        mEmptyView.setVisibility(newCursor != null && newCursor.getCount() > 0 ? View.GONE : View.VISIBLE);
        return super.swapCursor(newCursor);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener {
        public final TextView symbol;
        public final TextView bidPrice;
        public final TextView change;

        public ViewHolder(View itemView) {
            super(itemView);
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            symbol.setTypeface(robotoLight);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }

        @Override
        public void onClick(View v) {
            mOnItemClickListener.onItemClicked(this, getAdapterPosition());
        }
    }

    public void toggleUnit() {
        mIsPercent = !mIsPercent;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClicked(ViewHolder vh, int position);
    }
}
