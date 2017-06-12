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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.samples.cronet_sample.data.ImageRepository;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ViewHolder> {

    private static final String TAG = "ViewAdapter";
    private static CronetEngine cronetEngine;
    private Context context;

    public ViewAdapter(Context context) {
        this.context = context;
        getCronetEngine(context);
        startNetLog();
    }

    @Override
    public ViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_layout, null);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageViewCronet;

        public ViewHolder(View v) {
            super(v);
            mImageViewCronet = (ImageView) itemView.findViewById(R.id.cronet_image);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // Create an executor to execute the request
        Executor executor = Executors.newSingleThreadExecutor();
        UrlRequest.Callback callback = new SimpleUrlRequestCallback(holder.mImageViewCronet,
                this.context);
        UrlRequest.Builder builder = cronetEngine.newUrlRequestBuilder(
                ImageRepository.getImage(position), callback, executor);
        // Measure the start time of the request so that
        // we can measure latency of the entire request cycle
        ((SimpleUrlRequestCallback) callback).start = System.nanoTime();
        // Start the request
        builder.build().start();

    }

    /**
     * Use this class for create a request and receive a callback once the request is finished.
     */
    class SimpleUrlRequestCallback extends UrlRequest.Callback {

        private ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream();
        private WritableByteChannel receiveChannel = Channels.newChannel(bytesReceived);
        private ImageView imageView;
        public long start;
        private long stop;
        private Activity mainActivity;

        SimpleUrlRequestCallback(ImageView imageView, Context context) {
            this.imageView = imageView;
            this.mainActivity = (Activity) context;
        }

        @Override
        public void onRedirectReceived(
                UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            android.util.Log.i(TAG, "****** onRedirectReceived ******");
            request.followRedirect();
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            android.util.Log.i(TAG, "****** Response Started ******");
            android.util.Log.i(TAG, "*** Headers Are *** " + info.getAllHeaders());

            request.read(ByteBuffer.allocateDirect(32 * 1024));
        }

        @Override
        public void onReadCompleted(
                UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            android.util.Log.i(TAG, "****** onReadCompleted ******" + byteBuffer);
            byteBuffer.flip();
            try {
                receiveChannel.write(byteBuffer);
            } catch (IOException e) {
                android.util.Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
            }
            byteBuffer.clear();
            request.read(byteBuffer);
        }

        @Override
        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {

            stop = System.nanoTime();

            android.util.Log.i(TAG,
                    "****** Cronet Request Completed, the latency is " + (stop - start));

            android.util.Log.i(TAG,
                    "****** Cronet Request Completed, status code is " + info.getHttpStatusCode()
                            + ", total received bytes is " + info.getReceivedByteCount());
            // Set the latency
            ((MainActivity) context).addCronetLatency(stop - start);

            byte[] byteArray = bytesReceived.toByteArray();
            final Bitmap bimage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    imageView.setImageBitmap(bimage);
                    imageView.getLayoutParams().height = bimage.getHeight();
                    imageView.getLayoutParams().width = bimage.getWidth();
                }
            });
        }

        @Override
        public void onFailed(UrlRequest var1, UrlResponseInfo var2, CronetException var3) {
            android.util.Log.i(TAG, "****** onFailed, error is: " + var3.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return ImageRepository.numberOfImages();
    }

    private static synchronized CronetEngine getCronetEngine(Context context) {
        // Lazily create the Cronet engine.
        if (cronetEngine == null) {
            CronetEngine.Builder myBuilder = new CronetEngine.Builder(context);
            // Enable caching of HTTP data and
            // other information like QUIC server information, HTTP/2 protocol and QUIC protocol.
            cronetEngine = myBuilder
                    .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 100 * 1024)
                    .enableHttp2(true)
                    .enableQuic(true)
                    .build();
        }
        return cronetEngine;
    }

    /**
     * Method to start NetLog to log Cronet events
     */
    private void startNetLog() {
        File outputFile;
        try {
            outputFile = File.createTempFile("cronet", "log",
                    Environment.getExternalStorageDirectory());
            cronetEngine.startNetLogToFile(outputFile.toString(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
