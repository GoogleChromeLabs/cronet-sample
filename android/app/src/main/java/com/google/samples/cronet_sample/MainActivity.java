/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.cronet_sample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.samples.cronet_sample.data.ImageRepository;
import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private AtomicLong cronetLatency = new AtomicLong();
    private long totalLatency;
    private long numberOfImages;
    private ViewAdapter viewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(
            new ExceptionHandler());
        setContentView(R.layout.images_activity);
        setUpToolbar();
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.images_activity_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> loadItems());
        loadItems();
    }

    private void loadItems() {
        numberOfImages = 0;

        RecyclerView cronetView = (RecyclerView) findViewById(R.id.images_view);

        viewAdapter = new ViewAdapter(this);
        CustomGridLayoutManager gridLayoutManager =
                new CustomGridLayoutManager(this, 2, viewAdapter);

        // In order to enable Netlog, a Cronet logging system, enable write permissions.
        // Find more info about Netlog here:
        // https://www.chromium.org/developers/design-documents/network-stack/netlog
        enableWritingPermissionForLogging();

        cronetView.setLayoutManager(gridLayoutManager);
        cronetView.setAdapter(viewAdapter);
        cronetView.setItemAnimator(new DefaultItemAnimator());
        onItemsLoadComplete();

    }

    private static class CustomGridLayoutManager extends GridLayoutManager {
        private ViewAdapter viewAdapter;

        public CustomGridLayoutManager(Context context, int spanCount, ViewAdapter viewAdapter) {
            super(context, spanCount);
            this.viewAdapter = viewAdapter;
        }

        // stop network logging oce our requests are done
        public void onLayoutCompleted(RecyclerView.State state) {
            super.onLayoutCompleted(state);
            viewAdapter.stopNetLog();
        }
    }

    private void enableWritingPermissionForLogging() {
        int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void onItemsLoadComplete() {
        swipeRefreshLayout.setRefreshing(false);
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ((TextView) toolbar.findViewById(R.id.title)).setText(R.string.toolbar_title);
    }

    /**
     * This calculates and sets on the UI the latency of loading images with Cronet.
     * @param cronetLatency
     */
    public void addCronetLatency(final long cronetLatency) {

        totalLatency += cronetLatency;
        numberOfImages++;

        if (numberOfImages == ImageRepository.numberOfImages()) {
            final long averageLatency = totalLatency / numberOfImages;
            android.util.Log.i(TAG,
                    "All Cronet Requests Complete, the average latency is " + averageLatency);
            final TextView cronetTime = (TextView) findViewById(R.id.cronet_time_label);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cronetTime.setText(String.format(getResources()
                            .getString(R.string.images_loaded), averageLatency));
                }
            });
            this.cronetLatency.set(averageLatency);
            // viewAdapter.stopNetLog();
        }
    }

    // properly end network logging when app crashes
    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread thread, Throwable exception) {
            android.util.Log.e(TAG, exception.getLocalizedMessage());
            exception.printStackTrace();
            viewAdapter.stopNetLog();
        }
    }

}
