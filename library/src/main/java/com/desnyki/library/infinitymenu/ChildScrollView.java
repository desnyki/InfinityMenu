package com.desnyki.library.infinitymenu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/**
 * Created by MDeszczynski on 08/07/2016.
 */
public class ChildScrollView extends LayoutBase<ScrollView> {

    public ChildScrollView(Context context) {
        this(context, null);
    }

    public ChildScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChildScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected ScrollView createDragableView(Context context, AttributeSet attrs) {
        return new ScrollView(context);
    }

    @Override
    protected boolean isReadyForDragStart() {
        return mDragableView.getScrollY() == 0;
    }

    @Override
    protected boolean isReadyForDragEnd() {
        View scrollViewChild = mDragableView.getChildAt(0);
        return null != scrollViewChild && mDragableView.getScrollY()
                >= (scrollViewChild.getHeight() - getHeight());
    }
}
