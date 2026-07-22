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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.desnyki.library.infinitymenu.utils.SmoothScrollRunnable;

/**
 * Created by MDeszczynski on 06/07/2016.
 */
public abstract class LayoutBase<T extends View> extends FrameLayout {

    public static final int LINEAR_PARAMS = 1;
    public static final int RELATIVE_PARAMS = 2;

    public static final String ARROW = "arrow";

    private static final float FRICTION = 2.0f;

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

    // Views/ViewGroups/Params
    private View topView;
    private View bottomView;
    protected T mDragableView;
    private RootScrollView mScrollView;
    private ViewGroup.LayoutParams layoutParams;
    private LinearLayout.LayoutParams linearLayoutParams;
    private RelativeLayout.LayoutParams relativeLayoutParams;
    private int params;
    private int heightRange;
    private int beginBottomMargin;
    private int paddingTop;
    private int paddingBottom;
    private int paddingLeft;
    private int paddingRight;
    private int mHeight = 0;

    // Animators
    private ObjectAnimator mHeightAnimator;
    private ObjectAnimator mScrollYAnimator;
    private AnimatorSet animatorSet = new AnimatorSet();
    private int ANIMDURA = 300;
    private boolean isAnimating = false;
    private boolean isFullscreen = false;

    // Motion
    private int beginScrollY, endScrollY;
    private int prevScrollY = 0;
    private boolean xDrag = false;
    private boolean mIsBeingDragged = false;
    private float mLastMotionX, mLastMotionY;
    private float mInitialMotionY;
    private int iScrollY;
    private int realOffsetY;
    private int prevOffSetY = 0;
    private int dy;

    private Mode mMode = Mode.getDefault();
    private Mode mCurrentMode;
    private int closeDistance = dp2px(60);

    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
    private Runnable showRunnable;
    private Runnable hideRunnable;

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
        animatorSet.setInterpolator(new DecelerateInterpolator());
        mDragableView = createDragableView(context, attrs);

        addDragableView(mDragableView);

        showRunnable = new Runnable() {
            @Override
            public void run() {
                animate().alpha(1.0f).setDuration(100);
                isAnimating = false;
            }
        };

        hideRunnable = new Runnable() {
            @Override
            public void run() {
                setVisibility(View.INVISIBLE);
                ImageView arrow = (ImageView) topView.findViewWithTag(ARROW);

                if (arrow != null) arrow.animate().rotation(0f);

                mHeightAnimator.setIntValues(heightRange, beginBottomMargin);
                mScrollYAnimator.setIntValues(mScrollView.getScrollY(), beginScrollY);

                animatorSet.start();
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((ViewGroup) topView.getParent()).removeView(bottomView);
                        isAnimating = false;

                        //reset padding
                        getChildView().setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

                    }
                }, ANIMDURA + 10);
            }
        };
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {

        if (isOpen() | isAnimating) return;

        final T refreshableView = getDragableView();

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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (isAnimating) return false;

        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            if ((ev.getY() < topView.getMeasuredHeight() || ev.getY() > ((ScrollView) getChildAt(0)).getChildAt(0).getMeasuredHeight()) & !xDrag & !isFullscreen) {
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
            case MotionEvent.ACTION_DOWN: {
                if (isReadyForPull()) {
                    mLastMotionY = mInitialMotionY = event.getY();
                    mLastMotionX = event.getX();
                    return true;
                }
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                xDrag = false;
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;
                    smoothScrollTo(0, 200, 0);
                    prevOffSetY = 0;
                    return true;
                }
        }

        return false;
    }

    private void pullEvent() {
        final int newScrollValue;

        float diffMotionValue = mInitialMotionY - mLastMotionY;
        float maxDiff = Math.max(diffMotionValue, 0);
        float minDiff = Math.min(diffMotionValue, 0);

        switch (mCurrentMode) {
            case PULL_FROM_END:
                newScrollValue = Math.round(maxDiff / FRICTION);
                break;
            case PULL_FROM_START:
            default:
                newScrollValue = Math.round(minDiff / FRICTION);
                break;
        }
        moveContent(newScrollValue);
    }

    private void smoothScrollTo(int newScrollValue, long duration, long delayMillis) {

        if (null != mCurrentSmoothScrollRunnable) mCurrentSmoothScrollRunnable.stop();

        final int oldScrollValue;
        oldScrollValue = getScrollY();

        if (oldScrollValue != newScrollValue) {
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(this, oldScrollValue, newScrollValue, duration);
            if (delayMillis > 0) {
                postDelayed(mCurrentSmoothScrollRunnable, delayMillis);
            } else {
                post(mCurrentSmoothScrollRunnable);
            }
        }

        if (oldScrollValue > Math.abs(closeDistance)) {
            closeWithAnim();
            switch (mCurrentMode) {
                case PULL_FROM_END:
                    mCurrentSmoothScrollRunnable.setShouldRollback(true);
                    break;
                case PULL_FROM_START:
                    mCurrentSmoothScrollRunnable.setShouldRollback(false);
                    break;
            }
        } else {
            mCurrentSmoothScrollRunnable.setShouldRollback(true);
        }
    }

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

    public void openWithAnim(final View topView, final boolean isFullscreen, final boolean showTitle) {

        if (isOpen()) {
            closeWithAnim();
            return;
        }

        this.topView = topView;
        this.isFullscreen = isFullscreen;

        isAnimating = true;
        bottomView = new View(getContext());

        layoutParams = topView.getLayoutParams();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

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

        if (animatorSet.isRunning()) animatorSet.cancel();

        post(new Runnable() {
            @Override
            public void run() {
                setVisibility(View.VISIBLE);
                setAlpha(0.0f);

                if (isFullscreen) {
                    heightRange = mScrollView.getHeight();
                    beginBottomMargin = 0;
                } else {
                    heightRange = getChildView().getMeasuredHeight();
                }

                if (showTitle) {
                    endScrollY = topView.getTop();
                    paddingTop = getChildView().getPaddingTop();
                    paddingBottom = getChildView().getPaddingBottom();
                    paddingLeft = getChildView().getPaddingLeft();
                    paddingRight = getChildView().getPaddingRight();
                    getChildView().setPadding(paddingLeft, paddingTop + topView.getHeight(), paddingRight, paddingBottom);
                } else {
                    ((LayoutParams) getLayoutParams()).topMargin = 0;
                    endScrollY = topView.getBottom();
                }

                mHeightAnimator.setIntValues(beginBottomMargin, heightRange);
                beginScrollY = mScrollView.getScrollY();
                mScrollYAnimator.setIntValues(beginScrollY, endScrollY);
                animatorSet.start();

                ImageView arrow = (ImageView) topView.findViewWithTag(ARROW);
                if (arrow != null) arrow.animate().rotation(90f);

                postDelayed(showRunnable, ANIMDURA);
            }
        });
    }

    public void closeWithAnim() {
        if (!isOpen()) return;
        if (animatorSet.isRunning()) animatorSet.cancel();

        isAnimating = true;

        postDelayed(hideRunnable, 100);
        animate().alpha(0f).setDuration(100);
    }

    private void handleRootViewTouch() {
        if (isOpen()) closeWithAnim();
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

        if (topView != null) topView.setLayoutParams(layoutParams);
    }

    private void scrollYChangeAnim() {
        mScrollView.scrollTo(0, iScrollY);
        mScrollView.invalidate();
    }

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

    public RootScrollView getScrollView() {
        return mScrollView;
    }

    public final T getDragableView() {
        return mDragableView;
    }

    private View getChildView() {
        return ((ScrollView) getChildAt(0)).getChildAt(0);
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

    public void setCloseDistance(int dp) {
        closeDistance = dp2px(dp);
    }

    protected abstract T createDragableView(Context context, AttributeSet attrs);
    protected abstract boolean isReadyForDragStart();
    protected abstract boolean isReadyForDragEnd();
}