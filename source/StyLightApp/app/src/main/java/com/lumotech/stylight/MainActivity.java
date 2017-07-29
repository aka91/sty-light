package com.lumotech.stylight;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import java.net.UnknownHostException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.os.Bundle;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.AsyncTask;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.core.Size;
import org.opencv.core.Core;

    public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 100;
    private static final int port = 59900;
    private static final String IP = "127.0.0.1";
    private static String STORE_DIRECTORY;
    private static int IMAGES_PRODUCED;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection;

    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;
    Socket sock;
    ServerSocket server;


    private org.opencv.core.Point sTopLeft;
    private org.opencv.core.Point sTopRight;
    private org.opencv.core.Point sBottomLeft;
    private org.opencv.core.Point sBottomRight;

    private org.opencv.core.Point dTopLeft;
    private org.opencv.core.Point dTopRight;
    private org.opencv.core.Point dBottomLeft;
    private org.opencv.core.Point dBottomRight;

    private final int mDistortion = 100;

    private Mat mPerspectiveMatrix;


    private static final String TAG = "MainActivity";
    
    static{
        if(!OpenCVLoader.initDebug())
            Log.d(TAG, "OpenCV not loaded");
        else
            Log.d(TAG, "OpenCV loaded");
    }
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    Mat findPerspectiveTransform(){

        setDistortionPoints();

        List<org.opencv.core.Point> source = new ArrayList<>();
        source.add(sTopLeft);
        source.add(sTopRight);
        source.add(sBottomLeft);
        source.add(sBottomRight);
        Mat startM = Converters.vector_Point2f_to_Mat(source);


        List<org.opencv.core.Point> dest = new ArrayList<>();
        dest.add(dTopLeft);
        dest.add(dTopRight);
        dest.add(dBottomLeft);
        dest.add(dBottomRight);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Log.d(TAG, "findPerspectiveTransform: found pt");
        return Imgproc.getPerspectiveTransform(startM, endM);
    }

    void setDistortionPoints(){
        sTopLeft = new org.opencv.core.Point(0, 0);
        sTopRight = new org.opencv.core.Point(mWidth, 0);
        sBottomLeft = new org.opencv.core.Point(0, mHeight);
        sBottomRight = new org.opencv.core.Point(mWidth, mHeight);

        dTopLeft = new org.opencv.core.Point(0, 0);
        dTopRight = new org.opencv.core.Point(mWidth, 0);
        dBottomLeft = new org.opencv.core.Point(0 + mDistortion, mHeight);
        dBottomRight = new org.opencv.core.Point(mWidth - mDistortion, mHeight);
    }


    Bitmap warp(Bitmap image) {

        Mat inputMat = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(image, inputMat);
        Mat outputMat = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC1);

        Mat perspectiveTransform = findPerspectiveTransform();

        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(image.getWidth(), image.getHeight()));
        Core.flip(outputMat, outputMat, -1);
        Bitmap output = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outputMat, output);
        return output;
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    IMAGES_PRODUCED = IMAGES_PRODUCED % 10;
                    // write bitmap to a file
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap = warp(bitmap);
                    bitmap.compress(CompressFormat.JPEG, 20, stream);


                    IMAGES_PRODUCED++;
                    Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
                    try {

                        sock = server.accept();
                        System.out.println("Connecting...");


                        // sendfile
                        byte [] mybytearray  = stream.toByteArray();
                        OutputStream os = sock.getOutputStream();
                        System.out.println("Sending...");
                        os.write(mybytearray,0,mybytearray.length);
                        os.flush();

                        sock.close();
                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap != null) {
                    bitmap.recycle();
                }

                if (image != null) {
                    image.close();
                }
            }
        }
    }

    private class OrientationChangeCallback extends OrientationEventListener {

        OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e("ScreenCapture", "stopping projection.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    private void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
    }

    /****************************************** Factoring Virtual Display creation ****************/
    private void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x/2;
        mHeight = size.y/2;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Ahmadreza Ended


        // call for the projection manager
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        // start projection
        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    server = new ServerSocket(59900);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startProjection();
            }
        });

        // stop projection
        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                stopProjection();
            }
        });

        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();

                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (sMediaProjection != null) {
                File externalFilesDir = getExternalFilesDir(null);
                if (externalFilesDir != null) {
                    STORE_DIRECTORY = externalFilesDir.getAbsolutePath() + "/screenshots/";
                    File storeDirectory = new File(STORE_DIRECTORY);
                    if (!storeDirectory.exists()) {
                        boolean success = storeDirectory.mkdirs();
                        if (!success) {
                            Log.e(TAG, "failed to create file storage directory.");
                            return;
                        }
                    }
                } else {
                    Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.");
                    return;
                }

                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();

                // create virtual display depending on device width / height
                createVirtualDisplay();
                mPerspectiveMatrix = findPerspectiveTransform();


                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    //mOrientationChangeCallback.enable();
                }

                // register media projection stop callback
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
