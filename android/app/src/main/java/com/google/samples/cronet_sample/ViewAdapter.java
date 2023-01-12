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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.samples.cronet_sample.data.ImageRepository;

public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ViewHolder> {

    private final MainActivity mainActivity;

    public ViewAdapter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public ViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_layout, null);
        return new ViewHolder(v);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mImageViewCronet;

        public ViewHolder(View v) {
            super(v);
            mImageViewCronet = itemView.findViewById(R.id.cronet_image);
        }

        public ImageView getmImageViewCronet() {
            return mImageViewCronet;
        }
    }

    @RequiresApi(api = 34)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        CronetApplication cronetApplication = mainActivity.getCronetApplication();

        // UrlRequest and UrlRequest.Callback are the core of Cronet operations. UrlRequest is used
        // to issue requests, UrlRequest.Callback specifies how the application reacts to the server
        // responses.

        // Set up a callback which, on a successful read of the entire response, interprets
        // the response body as an image. By default, Cronet reads the body in small parts, having
        // the full body as a byte array is application specific logic. For more details about
        // the callbacks please see implementation of ReadToMemoryCronetCallback.
        ReadToMemoryCronetCallback callback = new ReadToMemoryCronetCallback() {
            @Override
            void onSucceeded(UrlRequest request, UrlResponseInfo info, byte[] bodyBytes,
                             long latencyNanos) {
                // Contribute the request latency
                mainActivity.onCronetImageLoadSuccessful(latencyNanos);

                // Send image to layout
                final Bitmap bimage = BitmapFactory.decodeByteArray(bodyBytes, 0, bodyBytes.length);
                mainActivity.runOnUiThread(() -> {
                    holder.getmImageViewCronet().setImageBitmap(bimage);
                    holder.getmImageViewCronet().getLayoutParams().height = bimage.getHeight();
                    holder.getmImageViewCronet().getLayoutParams().width = bimage.getWidth();
                });
            }
        };

        // The URL request builder allows you to customize the request.
        UrlRequest.Builder builder = cronetApplication.getCronetEngine()
                .newUrlRequestBuilder(
                        ImageRepository.getImage(position),
                        callback,
                        cronetApplication.getCronetCallbackExecutorService())
                // You can set arbitrary headers as needed
                .addHeader("x-my-custom-header", "Hello-from-Cronet")
                // Cronet supports QoS if you specify request priorities
                .setPriority(UrlRequest.Builder.REQUEST_PRIORITY_IDLE);
        // ... and more! Check the UrlRequest.Builder docs.

        // Start the request
        builder.build().start();
    }

    @Override
    public int getItemCount() {
        return Math.min(
                mainActivity.getCronetApplication().imagesToLoadCeiling.get(),
                ImageRepository.numberOfImages());
    }
}
