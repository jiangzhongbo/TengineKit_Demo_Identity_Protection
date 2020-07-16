package com.idprotect.activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

import com.idprotect.R;
import com.idprotect.camera.CameraEngine;
import com.idprotect.currencyview.OverlayView;
import com.idprotect.utils.BitmapUtils;
import com.idprotect.utils.SensorEventUtil;
import com.tenginekit.AndroidConfig;
import com.tenginekit.Face;
import com.tenginekit.model.FaceDetectInfo;
import com.tenginekit.model.FaceLandmarkInfo;

import java.util.ArrayList;
import java.util.List;


public class ClassifierActivity extends CameraActivity {
    private static final String TAG = "ClassifierActivity";

    private OverlayView trackingOverlay;
    List<FaceLandmarkInfo> faceLandmarks;
    Bitmap testBitmap;
    List<Bitmap> testFaceBitmaps = new ArrayList<>();
    private final Paint circlePaint = new Paint();

    private SensorEventUtil sensorEventUtil;


    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected Size getDesiredPreviewFrameSize() {
        return new Size(1280, 960);
    }

    @Override
    public void onInit(int previewWidth, int previewHeight, int outputWidth, int outputHeight) {

        com.tenginekit.Face.init(getBaseContext(),
                AndroidConfig.create()
                        .setCameraMode()
                        .openFunc(AndroidConfig.Func.Detect)
                        .openFunc(AndroidConfig.Func.Landmark)
                        .setInputImageFormat(AndroidConfig.ImageFormat.YUV_NV21)
                        .setInputImageSize(previewWidth, previewHeight)
                        .setOutputImageSize(outputWidth, outputHeight)
        );

        com.tenginekit.Face.Camera.switchCamera(false);

        sensorEventUtil = new SensorEventUtil(this);

        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStrokeWidth((float) 20.0);
        circlePaint.setStyle(Paint.Style.STROKE);

        trackingOverlay = findViewById(R.id.facing_overlay);
        trackingOverlay.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(final Canvas canvas) {
                if(testBitmap != null){
                    canvas.drawBitmap(testBitmap, 0,0, circlePaint);
                }
                if(faceLandmarks != null){
                    for (int i = 0; i < faceLandmarks.size(); i++) {
                        Rect r = faceLandmarks.get(i).getBoundingBox();
                        canvas.drawRect(r, circlePaint);
                        canvas.drawBitmap(testFaceBitmaps.get(i), r.left, r.top, circlePaint);
//                        for (int j = 0; j < faceLandmarks.get(i).landmarks.size() ; j++) {
//                            float x = 0;
//                            float y = 0;
//                            x = faceLandmarks.get(i).landmarks.get(j).X;
//                            y = faceLandmarks.get(i).landmarks.get(j).Y;
//                            canvas.drawCircle(x, y, 2, circlePaint);
//                        }
                    }
                }
            }
        });


    }

    @Override
    protected void processImage(byte[] data, int previewWidth, int previewHeight, int outputWidth, int outputHeight) {

        int degree = CameraEngine.getInstance().getCameraOrientation(sensorEventUtil.orientation);

        com.tenginekit.Face.Camera.setRotation(degree - 90, false,
                outputWidth, outputHeight);

        com.tenginekit.Face.FaceDetect faceDetect = Face.detect(data);
        faceLandmarks = null;
        if(faceDetect.getFaceCount() > 0){
            faceLandmarks = faceDetect.landmark2d();
        }
        if(testBitmap != null){
            testBitmap.recycle();
        }
        testBitmap = Face.Image.convertCameraYUVData(
                data,
                previewWidth, previewHeight,
                outputWidth, outputHeight,
                - 90,
                true);


        for(Bitmap bitmap : testFaceBitmaps){
            bitmap.recycle();
        }
        testFaceBitmaps.clear();
        if(testBitmap != null && faceDetect.getFaceCount() > 0){
            if(faceLandmarks != null){
                for (int i = 0; i < faceLandmarks.size(); i++) {
                     Bitmap face = BitmapUtils.getDstArea(testBitmap, faceLandmarks.get(i).getBoundingBox());
                     face = BitmapUtils.blurByGauss(face, 50);
                     testFaceBitmaps.add(face);
                }
            }
        }

        runInBackground(new Runnable() {
            @Override
            public void run() {
                trackingOverlay.postInvalidate();
            }
        });
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        com.tenginekit.Face.release();
    }

}