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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends AppCompatActivity {

    private MainActivity current;

    boolean setBackCamera = false;
    boolean flagCanTakePicture = true;
    private static boolean permissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new ScreenReceiver(this), filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        current = this;
    }

    private void checkPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else {
            permissionsGranted = true;
            moveTaskToBack(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            //Check if the permission is granted or not.
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("result ok");
                permissionsGranted = true;
                moveTaskToBack(true);
            } else {
                checkPermissions();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                permissionsGranted = true;
                moveTaskToBack(true);
            } else {
                checkPermissions();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void lockedScreen() {
        if (current == null) return;

        System.out.println("Screen is locked");

        Window window = current.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //takePhoto();
    }

    public void turningOnScreen() {
        if (current == null) return;

        System.out.println("Turning on screen");
        Window window = current.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (permissionsGranted) {
            takePhoto();
        } else {
            checkPermissions();
        }
    }

    private Camera.PictureCallback mPicture = (data, camera) -> {

        Toast.makeText(this, "Image snapshot finished", Toast.LENGTH_SHORT).show();
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            System.out.println("Creating file failed");
            flagCanTakePicture = true;
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://" + pictureFile)));
            flagCanTakePicture = true;
            camera.stopPreview();
            camera.release();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Saving image failed");
        }
    };

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }

    private Camera prepareCameraView(boolean setBackCamera) {
        Camera camera = getCameraInstance(setBackCamera);
        Camera.Parameters cameraParameters = camera.getParameters();
        cameraParameters.setPictureSize(cameraParameters.getSupportedPictureSizes().get(0).width,
                cameraParameters.getSupportedPictureSizes().get(0).height);
        camera.setParameters(cameraParameters);
        return camera;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance(boolean setBackCamera) {
        Camera c = null;
        try {
            c = Camera.open(setBackCamera ? 0 : 1); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; //Returns null if camera is unavailable
    }

    private void takePhoto() {
        if (flagCanTakePicture) {
            flagCanTakePicture = false;
            Toast.makeText(getApplicationContext(), "Image snapshot started", Toast.LENGTH_SHORT).show();
            // here below "this" is activity context.
            Camera mCamera = prepareCameraView(setBackCamera);
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(() -> {
                mCamera.startPreview();
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mCamera.takePicture(null, null, mPicture);
            }).start();
        }
    }

    public void btnClick(View view) {
        takePhoto();
    }
}
