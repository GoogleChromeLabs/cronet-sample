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

import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private final AtomicReference<CronetMetrics> metrics = new AtomicReference<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.images_activity);
        setUpToolbar();
        swipeRefreshLayout = findViewById(R.id.images_activity_layout);
        swipeRefreshLayout.setOnRefreshListener(this::loadItems);
        loadItems();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
