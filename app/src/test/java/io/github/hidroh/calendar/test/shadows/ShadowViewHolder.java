package io.github.hidroh.calendar.test.shadows;

import android.support.v7.widget.RecyclerView;

import org.robolectric.annotation.Implements;

@Implements(RecyclerView.ViewHolder.class)
public class ShadowViewHolder {
    int adapterPosition;

    public int getAdapterPosition() {
        return adapterPosition;
    }

    public void setAdapterPosition(int adapterPosition) {
        this.adapterPosition = adapterPosition;
    }
}
