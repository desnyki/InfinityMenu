package com.desnyki.library.infinitymenu.utils;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.desnyki.library.infinitymenu.LayoutBase;

/**
 * Created by Rudolf on 10/19/2016.
 */

public class SmoothScrollRunnable implements Runnable {

    private LayoutBase parent;
    private final int mScrollToY;
    private final int mScrollFromY;
    private final long mDuration;
    private final Interpolator mInterpolator;

    private boolean isRunning = true;
    private long mStartTime = -1;
    private int mCurrentY = -1;
    private int prevY = 0;
    private int offsetY = 0;
    private boolean shouldRollback;

    public SmoothScrollRunnable(LayoutBase parent, int fromY, int toY, long duration) {
        this.parent = parent;
        mScrollFromY = fromY;
        mScrollToY = toY;
        mDuration = duration;
        mInterpolator = new DecelerateInterpolator();
    }

    @Override
    public void run() {
        if (mStartTime == -1) {
            mStartTime = System.currentTimeMillis();
        } else {
            long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
            normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

            final int deltaY = Math.round((mScrollFromY - mScrollToY)
                    * mInterpolator.getInterpolation(normalizedTime / 1000f));
            mCurrentY = mScrollFromY - deltaY;

            // prevY will be 0 at first time
            if (prevY == 0) prevY = mScrollFromY;

            offsetY = prevY - mCurrentY;
            prevY = mCurrentY;

            parent.scrollTo(0, mCurrentY);

            if (shouldRollback) {
                parent.getScrollView().scrollBy(0, -offsetY);
            }
        }

        if (isRunning && mScrollToY != mCurrentY) {
            parent.postDelayed(this, 17);
        }
    }

    public void stop() {
        isRunning = false;
        parent.removeCallbacks(this);
    }

    public void setShouldRollback(boolean shouldRollback) {
        this.shouldRollback = shouldRollback;
    }
}