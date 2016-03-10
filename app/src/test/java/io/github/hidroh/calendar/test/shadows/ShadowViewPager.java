package io.github.hidroh.calendar.test.shadows;

import android.support.v4.view.ViewPager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowViewGroup;

@Implements(value = ViewPager.class, inheritImplementationMethods = true)
public class ShadowViewPager extends ShadowViewGroup {

    @RealObject ViewPager realObject;
    private ViewPager.OnPageChangeListener listener;

    @Implementation
    public void addOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        this.listener = listener;
    }

    @Implementation
    public void setCurrentItem(int item, boolean smoothScroll) {
        realObject.setCurrentItem(item);
        notifyListener(false);
    }

    /**
     * Simulate a swipe gesture, which should update current item and trigger page change listener
     */
    public void swipeLeft() {
        realObject.setCurrentItem(realObject.getCurrentItem() + 1);
        notifyListener(true);
    }

    /**
     * Simulate a swipe gesture, which should update current item and trigger page change listener
     */
    public void swipeRight() {
        realObject.setCurrentItem(realObject.getCurrentItem() - 1);
        notifyListener(true);
    }

    private void notifyListener(boolean dragging) {
        if (dragging) {
            listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING);
        }
        listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_SETTLING);
        listener.onPageSelected(realObject.getCurrentItem());
        listener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
    }
}
