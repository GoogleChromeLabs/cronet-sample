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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.net.CronetProviderInstaller;
import com.google.android.gms.tasks.Task;

public class WelcomeActivity extends AppCompatActivity{

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.welcome_layout);
        setUpToolbar();

        final Button imagesButton = (Button) findViewById(R.id.images_button);
        ((TextView) findViewById(R.id.welcome_introduction))
                .setText(R.string.welcome_introduction_text);
        ((TextView) findViewById(R.id.cronet_load_images))
                .setText(R.string.cronet_load_images_text);

    }
    public void openImages(View view) {
        Task<Void> installTask = CronetProviderInstaller.installProvider(this);
        installTask.addOnCompleteListener(
            task -> {
                if (task.isSuccessful()) {
                    Intent mpdIntent = new Intent(this, MainActivity.class);
                    startActivity(mpdIntent);
                } else if (task.getException() != null) {
                    Exception cause = task.getException();
                    if (cause instanceof GooglePlayServicesNotAvailableException) {
                        Toast.makeText(this, "Google Play services not available.",
                                Toast.LENGTH_SHORT).show();
                    } else if (cause instanceof GooglePlayServicesRepairableException) {
                        Toast.makeText(this, "Google Play services update is required.",
                                Toast.LENGTH_SHORT).show();
                        startActivity(((GooglePlayServicesRepairableException) cause).getIntent());
                    } else {
                        Toast.makeText(this, "Unexpected error: " + cause,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Unable to load Google Play services.",
                            Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.welcome_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        ((TextView) toolbar.findViewById(R.id.welcome_title)).setText(R.string.welcome_activity);
    }

}
