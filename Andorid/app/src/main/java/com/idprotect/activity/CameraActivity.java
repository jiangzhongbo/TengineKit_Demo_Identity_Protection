/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.idprotect.activity;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.idprotect.R;
import com.idprotect.currencyview.LegacyCameraConnectionFragment;

public abstract class CameraActivity extends AppCompatActivity implements
        Camera.PreviewCallback, LegacyCameraConnectionFragment.CameraReadyListener {
    private static final String TAG = "CameraActivity";

    public static int CameraId = 0;

    private boolean isProcessingFrame = false;
    public static boolean is_front_camera = true;

    private Handler handler;
    private HandlerThread handlerThread;

    private boolean isInit = false;

    private int previewWidth;
    private int previewHeight;
    private int outputWidth;
    private int outputHeight;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        setFragment();

    }

    @Override
    public void onCameraReady(int previewWidth, int previewHeight, int outputWidth, int outputHeight) {
        try {
            if (!isInit) {
                isInit = true;
                this.previewWidth = previewWidth;
                this.previewHeight = previewHeight;
                this.outputWidth = outputWidth;
                this.outputHeight = outputHeight;

                onInit(previewWidth, previewHeight, outputWidth, outputHeight);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            return;
        }
        isProcessingFrame = true;
        processImage(bytes, previewWidth, previewHeight, outputWidth, outputHeight);
        camera.addCallbackBuffer(bytes);
        isProcessingFrame = false;
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        handlerThread = new HandlerThread("sdk");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
    }


    protected void setFragment() {
        LegacyCameraConnectionFragment fragment = new LegacyCameraConnectionFragment(
                this,
                this,
                getLayoutId(), getDesiredPreviewFrameSize());
        CameraId = fragment.getCameraId();
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    protected abstract void processImage(byte[] data, int previewWidth, int previewHeight, int outputWidth, int outputHeight);

    protected abstract void onInit(int previewWidth, int previewHeight, int outputWidth, int outputHeight);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();


}
