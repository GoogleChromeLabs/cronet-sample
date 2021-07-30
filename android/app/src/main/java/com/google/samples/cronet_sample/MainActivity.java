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
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.net.CronetEngine;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private final AtomicReference<CronetMetrics> metrics = new AtomicReference<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When debugging, the net log (https://www.chromium
        // .org/developers/design-documents/network-stack/netlog)
        // is an extremely useful tool to figure out what's going on in the network stack. However,
        // because it's a JSON file, it's quite sensitive to correct formatting,so we must ensure
        // that it's always closed properly.
        startNetLog();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            stopNetLog();
        });

        setContentView(R.layout.images_activity);
        setUpToolbar();
        swipeRefreshLayout = findViewById(R.id.images_activity_layout);
        swipeRefreshLayout.setOnRefreshListener(this::loadItems);
        loadItems();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetLog();
    }

    private void loadItems() {
        metrics.set(new CronetMetrics(0, 0));
        getCronetApplication().imagesToLoadCeiling.incrementAndGet();

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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ((TextView) toolbar.findViewById(R.id.title)).setText(R.string.toolbar_title);
    }

    /**
     * This calculates and sets on the UI the latency of loading images with Cronet.
     *
     * <p>This method must be thread safe as it can be called from multiple Cronet callbacks
     * in parallel.
     */
    public void onCronetImageLoadSuccessful(long requestLatencyNanos) {
        CronetMetrics delta = new CronetMetrics(requestLatencyNanos, 1);
        CronetMetrics newMetrics = metrics.accumulateAndGet(
                delta,
                (left, right) -> new CronetMetrics(
                        left.totalLatencyNanos + right.totalLatencyNanos,
                        left.numberOfLoadedImages + right.numberOfLoadedImages));


        if (newMetrics.numberOfLoadedImages == Math.min(
                getCronetApplication().imagesToLoadCeiling.get(),
                ImageRepository.numberOfImages())) {
            long averageLatencyNanos =
                    newMetrics.totalLatencyNanos / newMetrics.numberOfLoadedImages;
            android.util.Log.i(TAG,
                    "All Cronet Requests Complete, the average latency is " + averageLatencyNanos
                            + " nanos.");
            final TextView cronetTime = findViewById(R.id.cronet_time_label);
            runOnUiThread(() -> cronetTime.setText(String.format(getResources()
                    .getString(R.string.images_loaded), averageLatencyNanos)));
        }
    }

    CronetApplication getCronetApplication() {
        return ((CronetApplication) getApplication());
    }

    private CronetEngine getCronetEngine() {
        return getCronetApplication().getCronetEngine();
    }

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
            getCronetEngine().startNetLogToFile(outputFile.toString(), false);
        } catch (IOException e) {
            android.util.Log.e(TAG, e.toString());
        }
    }

    /**
     * Method to properly stop NetLog
     */
    private void stopNetLog() {
        getCronetEngine().stopNetLog();
    }

    /**
     * Holder of multiple metrics that can be atomically updated.
     */
    private static class CronetMetrics {
        final long totalLatencyNanos;
        final int numberOfLoadedImages;

        CronetMetrics(long totalLatencyNanos, int numberOfLoadedImages) {
            this.totalLatencyNanos = totalLatencyNanos;
            this.numberOfLoadedImages = numberOfLoadedImages;
        }
    }
}
