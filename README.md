# InfinityMenu

InfinityMenu is an Android Library implementing an accordion style menu. You can place any view with any size in the menu. To close the menu you can drag to close or tap outside the menu. You can have only one menu open at a time.

![Demo][1]

How to use:

You can look at the sample app to see how to use this library in detail.

In your layout create a FrameLayout which will hold the RootScrollView that contains the menu title bars. As well as the ChildScrollView that holds the menu item. It is important to set fillViewport for RootScrollView, and to set ChildScrollView as invisible.

```
 <FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <infinitymenu.RootScrollView
        android:id="@+id/menu_scroll_view"
        android:layout_height="match_parent"
        android:layout_width="match_parent"
        android:fillViewport="true"
        >
	</infinitymenu.RootScrollView>
	<infinitymenu.ChildScrollView
        android:id="@+id/child_scroll_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="invisible"
        >
    </infinitymenu.ChildScrollView>
    </FrameLayout>
```

You can style and place menu title bars as children of the RootScrollView. If you want to animate the arrow like in the gif, you will need to have an element with tag "arrow" which will then spin 90 degrees based on state.

``` 
<RelativeLayout android:id="@+id/bar1" android:background="@drawable/bar"
    android:layout_width="match_parent" android:layout_height="64dp" android:orientation="horizontal">           
    <ImageView
        android:scaleType="fitCenter"
        android:layout_width="16dp"
        android:layout_height="16dp"
        android:layout_centerVertical="true"
        android:src="@drawable/arrow_right"
        android:layout_alignParentEnd="true"
        android:tag="arrow"
        android:layout_marginRight="8dp"
        />
</RelativeLayout>

In your main Activity you will have to get a reference to both the ChildScrollView and RootScrollView as well as the bar views.

```


childScrollView.setBackgroundScrollView(rootScrollView);
childScrollView.setCloseDistance(50);

RelativeLayout bar1 = (RelativeLayout) findViewById(R.id.bar1);
        bar1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                childScrollView.addView((LinearLayout) getLayoutInflater().inflate(R.layout.my_menu_item, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                childScrollView.openWithAnim(bar1,true,false);
            }
        });
```

.openWithAnim(param 1: title bar object, param 2: is child full screen, param 3: do you want to animate the arrow) is the call that opens your accordion. 

[1]: ./art/demo.gif