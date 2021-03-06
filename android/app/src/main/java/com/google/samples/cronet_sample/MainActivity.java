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

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.samples.cronet_sample.data.ImageRepository;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.chromium.net.CronetEngine;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private final AtomicLong cronetLatency = new AtomicLong();
    private long totalLatency;
    private long numberOfImages;
    private CronetEngine cronetEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up cronet engine and it's logging mechanism
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        setCronetEngine();
        startNetLog();

        setContentView(R.layout.images_activity);
        setUpToolbar();
        swipeRefreshLayout = findViewById(R.id.images_activity_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> loadItems());
        loadItems();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ensure logging is stopped properly when activity is ended, either by system or
        // by clicking the back button
        stopNetLog();
    }

    private void loadItems() {
        numberOfImages = 0;

        RecyclerView cronetView = findViewById(R.id.images_view);

        GridLayoutManager gridLayoutManager =
                new GridLayoutManager(this, 2);

        cronetView.setLayoutManager(gridLayoutManager);
        cronetView.setAdapter(new ViewAdapter(this));
        cronetView.setItemAnimator(new DefaultItemAnimator());
        onItemsLoadComplete();

    }

    private void onItemsLoadComplete() {
        swipeRefreshLayout.setRefreshing(false);
    }

    private void setUpToolbar() {
        Toolbar toolbar =  findViewById(R.id.toolbar);
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
            final TextView cronetTime = findViewById(R.id.cronet_time_label);
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

    private void setCronetEngine() {
        // create the Cronet engine, enable caching of HTTP data and
        // other information like QUIC server information, HTTP/2 protocol and QUIC protocol.
        cronetEngine = new CronetEngine.Builder(this)
            .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 100 * 1024)
            .enableHttp2(true)
            .enableQuic(true)
            .build();

    }

    public CronetEngine getCronetEngine() { return cronetEngine; }

    /**
     * Method to start NetLog to log Cronet events.
     * Find more info about Netlog here:
     * https://www.chromium.org/developers/design-documents/network-stack/netlog
     */
    private void startNetLog() {
        File outputFile;
        try {
            outputFile = File.createTempFile("cronet", "log",
                this.getExternalFilesDir(null));
            cronetEngine.startNetLogToFile(outputFile.toString(), false);
        } catch (IOException e) {
            android.util.Log.e(TAG, e.toString());
        }
    }

    /**
     * Method to properly stop NetLog
     */
    private void stopNetLog() {
        cronetEngine.stopNetLog();
    }

    // properly end network logging when app crashes
    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread thread, Throwable exception) {
            android.util.Log.e(TAG, exception.toString());
            stopNetLog();
        }
    }
}
