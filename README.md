# InfinityMenu

[![GitHub license](https://img.shields.io/github/license/dcendents/android-maven-gradle-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![](https://jitpack.io/v/desnyki/InfinityMenu.svg)](https://jitpack.io/#desnyki/InfinityMenu)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Infinity%20Menu-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/4510)

InfinityMenu is an Android Library implementing an accordion style menu. You can place any view with any size in the menu. To close the menu you can drag to close or tap outside the menu. You can have only one menu open at a time.

Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    compile 'com.github.desnyki:InfinityMenu:1.0.3'
}
```

![Demo][1]

<a href="https://play.google.com/store/apps/details?id=com.desnyki.infinitymenu"><img alt="Android app on Google Play" src="https://developer.android.com/images/brand/en_app_rgb_wo_45.png" />
</a>

How to use:

You can look at the sample app to see how to use this library in detail.

In your layout create a FrameLayout which will hold the RootScrollView that contains the menu title bars. As well as the ChildScrollView that holds the menu item. It is important to set fillViewport for RootScrollView, and to set ChildScrollView as invisible.

```xml
 <FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <com.desnyki.library.infinitymenu.RootScrollView
        android:id="@+id/menu_scroll_view"
        android:layout_height="match_parent"
        android:layout_width="match_parent"
        android:fillViewport="true"
        >
	</com.desnyki.library.infinitymenu.RootScrollView>
	<com.desnyki.library.infinitymenu.ChildScrollView
        android:id="@+id/child_scroll_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="invisible"
        >
    </com.desnyki.library.infinitymenu.ChildScrollView>
    </FrameLayout>
```

You can style and place menu title bars as children of the RootScrollView. If you want to animate the arrow like in the gif, you will need to have an element with tag "arrow" which will then spin 90 degrees based on state.

``` xml
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

```
In your main Activity you will have to get a reference to both the ChildScrollView and RootScrollView as well as the bar views.

```java
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

.openWithAnim(param 1: title bar object, param 2: is child full screen, param 3: do you want to show the title bar if full screen) is the call that opens your accordion. 


License
-------

    Copyright 2016 Marcin Deszczynski

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: ./art/demo.gif