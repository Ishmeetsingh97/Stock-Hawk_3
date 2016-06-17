package com.sam_chordas.android.stockhawk.touch_helper;

import android.support.v7.widget.RecyclerView;


public class TouchCallback extends android.support.v7.widget.helper.ItemTouchHelper.Callback{
  private final ItemTouchHelperAdapter mAdapter;
  public static final float ALPHA_FULL = 1.0f;

  public TouchCallback(ItemTouchHelperAdapter adapter){
    mAdapter = adapter;
  }

  @Override
  public boolean isItemViewSwipeEnabled(){
    return true;
  }

  @Override
  public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder){
    final int dragFlags = android.support.v7.widget.helper.ItemTouchHelper.UP | android.support.v7.widget.helper.ItemTouchHelper.DOWN;
    final int swipeFlags = android.support.v7.widget.helper.ItemTouchHelper.START | android.support.v7.widget.helper.ItemTouchHelper.END;
    return makeMovementFlags(dragFlags, swipeFlags);
  }

  @Override
  public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder sourceViewHolder, RecyclerView.ViewHolder targetViewHolder){
    return true;
  }

  @Override
  public void onSwiped(RecyclerView.ViewHolder viewHolder, int i){
    mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
  }


  @Override
  public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState){
    if (actionState != android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_IDLE){
      ItemTouchHelper itemViewHolder = (ItemTouchHelper) viewHolder;
      itemViewHolder.onItemSelected();
    }

    super.onSelectedChanged(viewHolder, actionState);
  }

  @Override
  public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
    super.clearView(recyclerView, viewHolder);

    ItemTouchHelper itemViewHolder = (ItemTouchHelper) viewHolder;
    itemViewHolder.onItemClear();
  }
}
