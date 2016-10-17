package com.desnyki.infinitymenuexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.desnyki.library.infinitymenu.ChildScrollView;
import com.desnyki.library.infinitymenu.RootScrollView;

public class MainActivity extends AppCompatActivity {

    RelativeLayout bar1;
    RelativeLayout bar2;
    RelativeLayout bar3;
    RelativeLayout bar4;
    ChildScrollView childScrollView;
    RootScrollView rootScrollView;
    final LinearLayout[] childContainer = new LinearLayout[5]; //used to cache and access child views, no children have been hurt in this container

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_relative);

        rootScrollView = (RootScrollView) findViewById(R.id.menu_scroll_view);
        childScrollView = (ChildScrollView) findViewById(R.id.child_scroll_view);

        childScrollView.setBackgroundScrollView(rootScrollView);
        childScrollView.setCloseDistance(50);

        bar1 = (RelativeLayout) findViewById(R.id.bar1);
        childContainer[0]=(LinearLayout) getLayoutInflater().inflate(R.layout.my_menu_item, null);
        bar1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                childScrollView.addView(childContainer[0], 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                childScrollView.openWithAnim(bar1,true,false);
            }
        });
        bar2 = (RelativeLayout) findViewById(R.id.bar2);
        childContainer[1] = (LinearLayout) getLayoutInflater().inflate(R.layout.my_menu_item, null);
        bar2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                childScrollView.addView(childContainer[1], 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                childScrollView.openWithAnim(bar2, false, true);
            }
        });
        bar3 = (RelativeLayout) findViewById(R.id.bar3);
        childContainer[2] = (LinearLayout) getLayoutInflater().inflate(R.layout.my_menu_item, null);
        bar3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    childScrollView.addView(childContainer[2], 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    childScrollView.openWithAnim(bar3, false, true);
                }
//            }
        });
        bar4 = (RelativeLayout) findViewById(R.id.bar4);
        childContainer[3] = (LinearLayout) getLayoutInflater().inflate(R.layout.my_menu_item, null);
        bar4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    childScrollView.addView(childContainer[3], 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    childScrollView.openWithAnim(bar4, false, true);
            }
        });



    }
}
