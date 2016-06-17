package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

public class WidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetRemoteViews(this.getApplicationContext(), intent);
    }
}

class WidgetRemoteViews implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Cursor mCursor = null;

    public WidgetRemoteViews(Context context, Intent intent) {
        mContext = context;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        final long identityToken = Binder.clearCallingIdentity();

        mCursor = mContext.getContentResolver().query(
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);

        Binder.restoreCallingIdentity(identityToken);

    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {

        if (position == AdapterView.INVALID_POSITION ||
                mCursor == null || !mCursor.moveToPosition(position)) {
            return null;
        }

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_collection_item);

        String symbol = mCursor.getString(1);
        String value = mCursor.getString(4);

        if (mCursor.getInt(mCursor.getColumnIndex("is_up")) == 1){
            rv.setInt(R.id.change, "setBackgroundResource", R.drawable.change_green);
        } else {
            rv.setInt(R.id.change, "setBackgroundResource", R.drawable.change_red);
        }

        rv.setTextViewText(R.id.stock_symbol, symbol);
        rv.setContentDescription(R.id.stock_symbol, mContext.getString(R.string.Talkback) + symbol);

        rv.setTextViewText(R.id.change, value);
        rv.setContentDescription(R.id.change, mContext.getString(R.string.TalkbackChange) + value);

        Bundle extras = new Bundle();
        extras.putString("SYMBOL", symbol);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(mContext.getPackageName(), R.layout.widget_collection_item);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor.moveToPosition(position)) {
            return mCursor.getLong(0);
        }
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
