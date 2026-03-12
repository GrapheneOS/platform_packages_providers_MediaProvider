/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.providers.media.util;

import static com.android.providers.media.util.TestUtils.QUERY_TYPE;
import static com.android.providers.media.util.TestUtils.RESULT_KEY_EXCEPTION_CLASS_NAME;
import static com.android.providers.media.util.TestUtils.RESULT_KEY_EXCEPTION_MESSAGE;
import static com.android.providers.media.util.TestUtils.RESULT_KEY_OPEN_SUCCEEDED;
import static com.android.providers.media.util.TestUtils.OPEN_MIC_SPOOFING_SOURCE;
import static com.android.providers.media.util.TestUtils.RUN_INFINITE_ACTIVITY;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import java.io.IOException;


public class TestAppActivity extends Activity {
    private static final Uri MIC_SPOOFING_OPEN_SOURCE_URI =
            Uri.parse("content://media/mic_spoofing_source");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String queryType = getIntent().getStringExtra(QUERY_TYPE);
        queryType = queryType == null ? "null" : queryType;

        switch (queryType) {
            case RUN_INFINITE_ACTIVITY:
                while (true) {
                }
            case OPEN_MIC_SPOOFING_SOURCE:
                openMicSpoofingSourceForResult();
                return;
            default:
                throw new IllegalStateException(
                        "Unknown query received from launcher app: " + queryType);
        }
    }

    private void openMicSpoofingSourceForResult() {
        Intent result = new Intent();
        try (ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(
                MIC_SPOOFING_OPEN_SOURCE_URI, "r")) {
            result.putExtra(RESULT_KEY_OPEN_SUCCEEDED, parcelFileDescriptor != null);
        } catch (IOException | SecurityException e) {
            result.putExtra(RESULT_KEY_OPEN_SUCCEEDED, false);
            result.putExtra(RESULT_KEY_EXCEPTION_CLASS_NAME, e.getClass().getName());
            result.putExtra(RESULT_KEY_EXCEPTION_MESSAGE, e.getMessage());
        }

        setResult(Activity.RESULT_OK, result);
        finish();
    }
}
