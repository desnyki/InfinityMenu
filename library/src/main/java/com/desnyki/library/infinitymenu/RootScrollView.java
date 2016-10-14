package com.desnyki.library.infinitymenu;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by MDeszczynski on 06/07/2016.
 */
public class RootScrollView extends ScrollView {
<<<<<<< HEAD
    private final static String TAG = "RootScrollView";
    private boolean mTouchable = true;
=======
>>>>>>> 3ee72ec429934117c31ccf920f0f1f2a8fd62cb9

    public RootScrollView(Context context) {
        this(context,null);
    }

    public RootScrollView(Context context, AttributeSet attrs) {
        this(context,attrs,0);
    }

    public RootScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }
}
