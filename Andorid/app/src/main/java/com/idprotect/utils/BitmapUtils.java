package com.idprotect.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;

/**
 * Created by XTER on 2016/7/8.
 */
public class BitmapUtils {

    public static Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);// 使用后乘
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return newBM;
    }

    /**
     * 兼容处理
     *
     * @param context
     *            上下文
     * @param bitmap
     *            模糊位图
     * @param view
     *            模糊区域
     * @param radius
     *            模糊半径
     */
    public static void blur(Context context, Bitmap bitmap, View view, int radius) {
        if (Build.VERSION.SDK_INT > 17) {
            blurByRender(context, bitmap, view, radius);
        } else {
            blurByGauss(context, bitmap, view, radius);
        }
    }

    /**
     * 兼容处理
     *
     * @param context
     *            上下文
     * @param bitmap
     *            模糊位图
     * @param radius
     *            模糊半径
     * @return bitmap
     */
    public static Bitmap blur(Context context, Bitmap bitmap, int radius) {
        if (Build.VERSION.SDK_INT > 17) {
            return blurBitmapByRender(context, bitmap, radius);
        } else {
            return blurByGauss(bitmap, radius);
        }
    }

    /**
     * 高斯模糊
     *
     * @param srcBitmap
     *            源位图
     * @param radius
     *            模糊半径
     * @return bitmap
     */
    public static Bitmap blurByGauss(Bitmap srcBitmap, int radius) {

        Bitmap bitmap = srcBitmap.copy(srcBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int temp = 256 * divsum;
        int dv[] = new int[temp];
        for (i = 0; i < temp; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }

    /**
     * 高斯局部模糊
     *
     * @param context
     *            上下文
     * @param bitmap
     *            模糊位图
     * @param view
     *            模糊区域
     * @param radius
     *            模糊半径
     */
    public static void blurByGauss(Context context, Bitmap bitmap, View view, float radius) {
        // 得到要处理的区域
        Bitmap dstArea = getDstArea(bitmap, view);

        // 作模糊处理
        dstArea = blurByGauss(zoomImage(dstArea, 0.8f), (int) radius);

        // 设置背景
        view.setBackground(new BitmapDrawable(context.getResources(), dstArea));

        bitmap.recycle();
    }

    /**
     * RenderScript模糊
     *
     * @param context
     *            上下文
     * @param bitmap
     *            源位图
     * @param radius
     *            模糊半径
     * @return bitmap
     */
    @SuppressLint("NewApi")
    public static Bitmap blurBitmapByRender(Context context, Bitmap bitmap, float radius) {
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
        blurScript.setRadius(radius);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(outBitmap);

        bitmap.recycle();
        rs.destroy();

        return outBitmap;
    }

    /**
     * RenderScript局部模糊
     *
     * @param context
     *            上下文
     * @param bitmap
     *            模糊位图
     * @param view
     *            模糊区域
     * @param radius
     *            模糊半径
     */
    @SuppressLint("NewApi")
    public static void blurByRender(Context context, Bitmap bitmap, View view, float radius) {
        // 得到要处理的区域
        Bitmap dstArea = getDstArea(bitmap, view);
        dstArea = zoomImage(dstArea, 0.8f);

        // 作模糊处理
        RenderScript rs = RenderScript.create(context);
        Allocation overlayAlloc = Allocation.createFromBitmap(rs, dstArea);
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());
        blur.setInput(overlayAlloc);
        blur.setRadius(radius);
        blur.forEach(overlayAlloc);
        overlayAlloc.copyTo(dstArea);

        // 设置背景
        view.setBackground(new BitmapDrawable(context.getResources(), dstArea));

        bitmap.recycle();
        rs.destroy();
    }

    /**
     * 得到待处理的位图
     *
     * @param bitmap
     *            模糊位图
     * @param view
     *            模糊区域
     * @return bitmap
     */
    public static Bitmap getDstArea(Bitmap bitmap, View view) {
        Bitmap dstArea = Bitmap.createBitmap((int) (view.getMeasuredWidth()), (int) (view.getMeasuredHeight()),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dstArea);
        canvas.translate(-view.getLeft(), -view.getTop());
        canvas.drawBitmap(bitmap, 0, 0, null);
        return dstArea;
    }


    public static Bitmap getDstArea(Bitmap bitmap, Rect rect) {
        Bitmap dstArea = Bitmap.createBitmap((int) (rect.width()), (int) (rect.height()),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dstArea);
        canvas.translate(-rect.left, -rect.top);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return dstArea;
    }

    public static Bitmap zoomImage(Bitmap srcBitmap, float scale) {
        // 获取这个图片的宽和高
        float width = srcBitmap.getWidth();
        float height = srcBitmap.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 缩放图片动作
        matrix.postScale(scale, scale);
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap, 0, 0, (int) width, (int) height, matrix, true);
        return bitmap;
    }

}