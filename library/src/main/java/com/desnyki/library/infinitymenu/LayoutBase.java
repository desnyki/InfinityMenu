package com.desnyki.library.infinitymenu;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

/**
 * Created by MDeszczynski on 06/07/2016.
 */
public abstract class LayoutBase<T extends View> extends FrameLayout {

    public final static int LINEAR_PARAMS = 1;
    public final static int RELATIVE_PARAMS = 2;

    public static final String ARROW = "arrow";

    private View topView;
    private RootScrollView mScrollView;
    private ViewGroup.LayoutParams layoutParams;
    private LinearLayout.LayoutParams linearLayoutParams;
    private RelativeLayout.LayoutParams relativeLayoutParams;
    private View bottomView;
    private int params = 0;
    private int heightRange;
    private int beginBottomMargin;
    private int beginScrollY, endScrollY;
    private ObjectAnimator mHeightAnimator;
    private ObjectAnimator mScrollYAnimator;
    private int ANIMDURA = 300;
    private AnimatorSet animatorSet = new AnimatorSet();
    boolean isAnimating = false;
    boolean fullScreen = false;
    private int paddingTop;
    private int paddingBottom;
    private int paddingLeft;
    private int paddingRight;
    private DisplayMetrics displayMetrics;

    private Runnable showRunnable = new Runnable() {
        @Override
        public void run() {
            animate().alpha(1.0f).setDuration(100);

            isAnimating = false;
        }
    };

    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            setVisibility(View.INVISIBLE);
            mHeightAnimator.setIntValues(heightRange, beginBottomMargin);
            mScrollYAnimator.setIntValues(mScrollView.getScrollY(), beginScrollY);
            ImageView arrow = (ImageView) topView.findViewWithTag(ARROW);
            if (arrow != null)
                arrow.animate().rotation(0f);
            animatorSet.start();
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((ViewGroup) topView.getParent()).removeView(bottomView);
                    isAnimating = false;
                    ((ScrollView) getChildAt(0)).getChildAt(0).setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom); //reset padding
                }
            }, ANIMDURA + 10);
        }
    };

    private Interpolator mInterpolator = new DecelerateInterpolator();
    private int mHeight = 0;
    private int iScrollY;
    protected T mDragableView;

    public LayoutBase(Context context) {
        this(context, null);
    }

    public LayoutBase(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LayoutBase(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        mHeightAnimator = ObjectAnimator.ofInt(this, aHeight, 0, 0);
        mScrollYAnimator = ObjectAnimator.ofInt(this, aScrollY, 0, 0);
        mHeightAnimator.setDuration(ANIMDURA);
        mScrollYAnimator.setDuration(ANIMDURA);
        animatorSet.playTogether(mHeightAnimator, mScrollYAnimator);
        animatorSet.setInterpolator(mInterpolator);
        mDragableView = createDragableView(context, attrs);

        addDragableView(mDragableView);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (isOpen() | isAnimating)
            return;

        final T refreshableView = getDragableView();
        displayMetrics = getResources().getDisplayMetrics();

        if (child == refreshableView) {
            super.addView(child, index, params);
            return;
        }

        if (refreshableView instanceof ViewGroup) {
            ((ViewGroup) refreshableView).removeAllViews(); // will break if view isn t scrollview
            ((ViewGroup) refreshableView).addView(child, index, params);
        } else {
            throw new UnsupportedOperationException("Draggable View is not a ViewGroup");
        }
    }

    public void setBackgroundScrollView(RootScrollView scrollView) {
        mScrollView = scrollView;
        mScrollView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                final int action = motionEvent.getAction();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        handleRootViewTouch();
                }
                return true;
            }
        });
    }

    private boolean mIsBeingDragged = false;
    private float mLastMotionX, mLastMotionY;
    private float mInitialMotionY;

    private enum Mode {
        PULL_FROM_START(0x1),
        PULL_FROM_END(0x2),
        BOTH(0x3);
        private int mIntValue;

        Mode(int modeInt) {
            mIntValue = modeInt;
        }

        static Mode getDefault() {
            return BOTH;
        }
    }

    private Mode mMode = Mode.getDefault();
    private Mode mCurrentMode;
    static final float FRICTION = 2.0f;
    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
    private int closeDistance = dp2px(60);
    private boolean shouldRollback;
    private Interpolator mScrollAnimationInterpolator = new DecelerateInterpolator();
    private boolean xDrag = false;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isAnimating) {
            return false;
        }

        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            if ((ev.getY() < topView.getMeasuredHeight() || ev.getY() > ((ScrollView) getChildAt(0)).getChildAt(0).getMeasuredHeight()) & !xDrag & !fullScreen) {
                closeWithAnim();
            }
            xDrag = false;
            mIsBeingDragged = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (isReadyForPull()) {
                    mLastMotionY = mInitialMotionY = ev.getY();
                    mLastMotionX = ev.getX();
                    mIsBeingDragged = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isReadyForPull()) {
                    final float y = ev.getY(), x = ev.getX();
                    final float diff, oppositeDiff, absDiff;

                    diff = y - mLastMotionY;
                    oppositeDiff = x - mLastMotionX;
                    absDiff = Math.abs(diff);
                    if (Math.abs(oppositeDiff) > 3) {
                        xDrag = true;
                    }
                    if ((absDiff > Math.abs(oppositeDiff))) {
                        if ((diff >= 4f) && isReadyForDragStart()) {
                            mLastMotionY = y;
                            mLastMotionX = x;
                            mIsBeingDragged = true;
                            if (mMode == Mode.BOTH) {
                                mCurrentMode = Mode.PULL_FROM_START;
                            }
                        } else if ((diff <= -4f) && isReadyForDragEnd()) {
                            mLastMotionY = y;
                            mLastMotionX = x;
                            mIsBeingDragged = true;
                            if (mMode == Mode.BOTH) {
                                mCurrentMode = Mode.PULL_FROM_END;
                            }
                        }
                    }
                }
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (mIsBeingDragged) {
                    mLastMotionY = event.getY();
                    mLastMotionX = event.getX();
                    pullEvent();
                    return true;
                }
                break;
            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {
                    mLastMotionY = mInitialMotionY = event.getY();
                    mLastMotionX = event.getX();
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                xDrag = false;
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;
                    smoothScrollTo(0, 200, 0);
                    prevOffSetY = 0;

                    return true;
                }
                break;
            }
        }
        return false;
    }

    private void pullEvent() {
        final int newScrollValue;
        final float initialMotionValue, lastMotionValue;
        initialMotionValue = mInitialMotionY;
        lastMotionValue = mLastMotionY;

        switch (mCurrentMode) {
            case PULL_FROM_END:
                newScrollValue = Math.round(Math.max(initialMotionValue - lastMotionValue, 0) / FRICTION);
                break;
            case PULL_FROM_START:
            default:
                newScrollValue = Math.round(Math.min(initialMotionValue - lastMotionValue, 0) / FRICTION);
                break;
        }
        moveContent(newScrollValue);
    }

    private void smoothScrollTo(int newScrollValue, long duration, long delayMillis) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }
        final int oldScrollValue;
        oldScrollValue = getScrollY();
        if (oldScrollValue < -closeDistance || oldScrollValue > closeDistance) {
            closeWithAnim();
            delayMillis = 100;
            switch (mCurrentMode) {
                case PULL_FROM_END:
                    shouldRollback = true;
                    break;
                case PULL_FROM_START:
                    shouldRollback = false;
                default:
                    break;
            }
        } else {
            shouldRollback = true;
        }

        if (oldScrollValue != newScrollValue) {
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration);
            if (delayMillis > 0) {
                postDelayed(mCurrentSmoothScrollRunnable, delayMillis);
            } else {
                post(mCurrentSmoothScrollRunnable);
            }
        }
    }

    private int realOffsetY;
    private int prevOffSetY = 0;
    private int dy;
    int prevScrollY = 0;

    private int moveContent(int offsetY) {

        realOffsetY = (int) (offsetY / 1.4f);
        dy = prevOffSetY - realOffsetY;

        if (mScrollView.getScrollY() != 0) {
            scrollTo(0, realOffsetY);
            prevOffSetY = realOffsetY;

        }
        prevScrollY = mScrollView.getScrollY();
        mScrollView.scrollBy(0, -dy);
        mScrollView.invalidate();
        return realOffsetY;
    }

    private boolean isReadyForPull() {
        switch (mMode) {
            case PULL_FROM_START:
                return isReadyForDragStart();
            case PULL_FROM_END:
                return isReadyForDragEnd();
            case BOTH:
                return isReadyForDragEnd() || isReadyForDragStart();
            default:
                return false;
        }
    }

    final class SmoothScrollRunnable implements Runnable {
        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;
        private final long mDuration;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;
        private int PrevY = 0;
        private int offsetY = 0;

        public SmoothScrollRunnable(int fromY, int toY, long duration) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mInterpolator = mScrollAnimationInterpolator;
            mDuration = duration;
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

                if (PrevY == 0) { /*the PrevY will be 0 at first time */
                    PrevY = mScrollFromY;
                }

                offsetY = PrevY - mCurrentY;
                PrevY = mCurrentY;

                scrollTo(0, mCurrentY);

                if (shouldRollback) {
                    mScrollView.scrollBy(0, -offsetY);
                }
            }

            if (mContinueRunning && mScrollToY != mCurrentY) {
                LayoutBase.this.postDelayed(this, 17);
            }
        }

        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }

    public void setCloseDistance(int dp) {
        closeDistance = dp2px(dp);
    }

    public void openWithAnim(final View topView, final boolean fullScreen, final boolean showTitle) {

        if (isOpen()) {
            closeWithAnim();
        }

        if (isAnimating)
            return;

        this.topView = topView;
        this.fullScreen = fullScreen;

        isAnimating = true;

        bottomView = new View(getContext());

        layoutParams = topView.getLayoutParams();

        if (layoutParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams myParams = new LinearLayout.LayoutParams(0, 0);
            myParams.setMargins(0, displayMetrics.heightPixels, 0, 0);
            bottomView.setLayoutParams(myParams);
            params = LINEAR_PARAMS;
            linearLayoutParams = (LinearLayout.LayoutParams) layoutParams;
            beginBottomMargin = linearLayoutParams.bottomMargin;
            ((LinearLayout) topView.getParent()).addView(bottomView);
        } else if (layoutParams instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams myParams = new RelativeLayout.LayoutParams(0, 0);
            myParams.setMargins(0, displayMetrics.heightPixels * 2, 0, 0);
            bottomView.setLayoutParams(myParams);
            params = RELATIVE_PARAMS;
            relativeLayoutParams = (RelativeLayout.LayoutParams) layoutParams;
            beginBottomMargin = relativeLayoutParams.bottomMargin;
            ((RelativeLayout) topView.getParent()).addView(bottomView);
        } else {
            Log.e("error", "topView's parent should be linearlayout or relativelayout");
            return;
        }

        if (animatorSet.isRunning()) {
            animatorSet.cancel();
        }

        post(new Runnable() {
            @Override
            public void run() {
                setVisibility(View.VISIBLE);
                setAlpha(0.0f);

                if (fullScreen) {
                    heightRange = mScrollView.getHeight();
                    beginBottomMargin = 0;
                } else {
                    heightRange = ((ScrollView) getChildAt(0)).getChildAt(0).getMeasuredHeight();
                }

                if (showTitle) {
                    endScrollY = topView.getTop();
                    paddingTop = ((ScrollView) getChildAt(0)).getChildAt(0).getPaddingTop();
                    paddingBottom = ((ScrollView) getChildAt(0)).getChildAt(0).getPaddingBottom();
                    paddingLeft = ((ScrollView) getChildAt(0)).getChildAt(0).getPaddingLeft();
                    paddingRight = ((ScrollView) getChildAt(0)).getChildAt(0).getPaddingRight();
                    ((ScrollView) getChildAt(0)).getChildAt(0).setPadding(paddingLeft, paddingTop + topView.getHeight(), paddingRight, paddingBottom);
                } else {
                    ((LayoutParams) getLayoutParams()).topMargin = 0;
                    endScrollY = topView.getBottom();
                }

                mHeightAnimator.setIntValues(beginBottomMargin, heightRange);
                beginScrollY = mScrollView.getScrollY();
                mScrollYAnimator.setIntValues(beginScrollY, endScrollY);
                animatorSet.start();
                ImageView arrow = (ImageView) topView.findViewWithTag(ARROW);
                if (arrow != null)
                    arrow.animate().rotation(90f);
                postDelayed(showRunnable, ANIMDURA);
            }
        });
    }

    public void closeWithAnim() {
        if (!isOpen())
            return;

        isAnimating = true;

        if (animatorSet.isRunning()) {
            animatorSet.cancel();
        }

        postDelayed(hideRunnable, 100);

        animate().alpha(0f).setDuration(100);
    }

    private void handleRootViewTouch() {
        if (isOpen()) {
            closeWithAnim();
        }
    }

    private Property<LayoutBase, Integer> aHeight = new Property<LayoutBase, Integer>(Integer.class, "mHeight") {
        @Override
        public Integer get(LayoutBase object) {
            return object.mHeight;
        }

        @Override
        public void set(LayoutBase object, Integer value) {
            object.mHeight = value;
            heightChangeAnim();
        }
    };
    private Property<LayoutBase, Integer> aScrollY = new Property<LayoutBase, Integer>(Integer.class, "iScrollY") {
        @Override
        public Integer get(LayoutBase object) {
            return object.iScrollY;
        }

        @Override
        public void set(LayoutBase object, Integer value) {
            object.iScrollY = value;
            scrollYChangeAnim();
        }
    };

    private void heightChangeAnim() {
        switch (params) {
            case LINEAR_PARAMS:
                linearLayoutParams.bottomMargin = mHeight;
                break;
            case RELATIVE_PARAMS:
                relativeLayoutParams.bottomMargin = mHeight;
        }

        if (topView != null)
            topView.setLayoutParams(layoutParams);
    }

    private void scrollYChangeAnim() {
        mScrollView.scrollTo(0, iScrollY);
        mScrollView.invalidate();
    }

    public final T getDragableView() {
        return mDragableView;
    }

    protected abstract T createDragableView(Context context, AttributeSet attrs);

    protected abstract boolean isReadyForDragStart();

    protected abstract boolean isReadyForDragEnd();

    private void addDragableView(T DragableView) {
        addView(DragableView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp2px(float dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    public boolean isOpen() {
        return getVisibility() == View.VISIBLE;
    }

    public boolean isAnimating(){
        return isAnimating;
    }
}