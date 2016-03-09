package io.github.hidroh.calendar.test.shadows;

import android.support.v7.widget.RecyclerView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowViewGroup;
import org.robolectric.util.ReflectionHelpers;

import static org.robolectric.internal.Shadow.directlyOn;

@Implements(value = RecyclerView.class, inheritImplementationMethods = true)
public class ShadowRecyclerView extends ShadowViewGroup {
    @RealObject RecyclerView realObject;

    @Implementation
    public void smoothScrollToPosition(int position) {
        realObject.getLayoutManager().scrollToPosition(position);
        directlyOn(realObject, RecyclerView.class, "dispatchOnScrolled",
                ReflectionHelpers.ClassParameter.from(int.class, 0),
                ReflectionHelpers.ClassParameter.from(int.class, 0));
    }
}
