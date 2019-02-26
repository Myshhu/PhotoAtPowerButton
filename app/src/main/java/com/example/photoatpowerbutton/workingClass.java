package com.example.photoatpowerbutton;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class workingClass extends AppCompatActivity {

    private MainActivity current;

    boolean setBackCamera = false;
    boolean flagCanTakePicture = true;
    private Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        //prepareCameraView(setBackCamera);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //registerReceiver(new ScreenReceiver(this), filter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //current = this;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
    }

    public void lockedScreen() {
        if (current == null) return;

        System.out.println("Screen is locked");

        Window window = current.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        takePhoto();
    }

    public void turningOnScreen() {
        if (current == null) return;

        System.out.println("Turning on screen");
        Window window = current.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //takePhoto();
        takeSnapShots();
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                System.out.println("Creating file failed");
                flagCanTakePicture = true;
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://"+ pictureFile)));
                flagCanTakePicture = true;
            } catch (Exception e){
                e.printStackTrace();
                System.out.println("Saving image failed");
            }
        }
    };

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    private void prepareCameraView(boolean setBackCamera){
        mCamera = getCameraInstance(setBackCamera);
        Camera.Parameters cameraParameters = mCamera.getParameters();
        cameraParameters.setPictureSize(cameraParameters.getSupportedPictureSizes().get(0).width,
                cameraParameters.getSupportedPictureSizes().get(0).height);
        mCamera.setParameters(cameraParameters);
        /*CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.removeAllViews();
        preview.addView(mPreview);*/
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(boolean setBackCamera){
        Camera c = null;
        try {
            c = Camera.open(setBackCamera ? 0 : 1); // attempt to get a Camera instance
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return c; //Returns null if camera is unavailable
    }

    private void takePhoto() {
        try {
            if (flagCanTakePicture) {
                flagCanTakePicture = false;
                mCamera.startPreview();

                mCamera.takePicture(null, null, mPicture);
                mCamera.stopPreview();
                mCamera.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takeSnapShots()
    {
        Toast.makeText(getApplicationContext(), "Image snapshot   Started", Toast.LENGTH_SHORT).show();
        // here below "this" is activity context.
        prepareCameraView(false);
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new Thread(()->{
            mCamera.startPreview();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mCamera.takePicture(null,null, mPicture);
        }).start();
    }

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera)
        {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(pictureFile);
                outStream.write(data);
                outStream.close();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://"+ pictureFile)));
                //Log.d(, "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally
            {
                camera.stopPreview();
                camera.release();
                Toast.makeText(getApplicationContext(), "Image snapshot Done",Toast.LENGTH_LONG).show();
            }
        }
    };

    public void btnClick(View view) {
        takeSnapShots();
    }
}
