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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

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
        setContentView(R.layout.images_activity);
        setUpToolbar();
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.images_activity_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadItems();
            }
        });
        loadItems();
    }

    private void loadItems() {
        numberOfImages = 0;

        RecyclerView cronetView = (RecyclerView) findViewById(R.id.images_view);
        GridLayoutManager gridLayoutManager =
                new GridLayoutManager(this, 2);

        viewAdapter = new ViewAdapter(this);
        // In order to enable Netlog, a Cronet logging system, enable write permissions.
        // Find more info about Netlog here:
        // https://www.chromium.org/developers/design-documents/network-stack/netlog
        enableWritingPermissionForLogging();

        cronetView.setLayoutManager(gridLayoutManager);
        cronetView.setAdapter(viewAdapter);
        cronetView.setItemAnimator(new DefaultItemAnimator());
        onItemsLoadComplete();

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
        }
    }
}
