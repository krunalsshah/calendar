package io.github.hidroh.calendar.test.shadows;

import android.support.v7.widget.RecyclerView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(RecyclerView.ViewHolder.class)
public class ShadowViewHolder {
    public int adapterPosition;

    @Implementation
    public int getAdapterPosition() {
        return adapterPosition;
    }
}
