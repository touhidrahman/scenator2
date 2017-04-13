package gsd.hsfulda.mobapps.scenator2;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.List;

import gsd.hsfulda.mobapps.scenator2.filters.BlankFilter;
import gsd.hsfulda.mobapps.scenator2.filters.Filter;
import gsd.hsfulda.mobapps.scenator2.filters.ImageDetectionFilter;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // tag for log output
    private static final String TAG = MainActivity.class.getSimpleName();

    // a key to store the index of active camera
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    // and key for active image size
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";

    // Keys for storing the indices of the active filters
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX =
            "imageDetectionFilterIndex";

    // filter
    private Filter[] mImageDetectionFilters;

    // index of active filter
    private int mImageDetectionFilterIndex;

    // id for image size submenu
    private static final int MENU_GROUP_ID_SIZE = 2;

    // index of the active camera
    private int mCameraIndex;

    // index of active image size
    private int mImageSizeIndex;

    // if the camerra is front facing (then we need to mirror image)
    private boolean isCameraFrontFacing;

    // available cameras in the device
    private int numCameras;

    // camera view
    private CameraBridgeViewBase mCameraView;

    // image sizes supported b the device
    private List<android.hardware.Camera.Size> mSupportedImageSizes;

    // Whether the next camera frame should be saved as a photo.
    private boolean mIsPhotoPending;

    // A matrix that is used when saving photos.
    private Mat mBgr;

    // Whether an asynchronous menu action is in progress.
    // If so, menu interaction should be disabled.
    private boolean mIsMenuLocked;

    // The OpenCV loader callback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded succefully");
                    mCameraView.enableView();
                    mBgr = new Mat();

                    final Filter starryNight;
                    try {
                        starryNight = new ImageDetectionFilter(
                                MainActivity.this, R.drawable.starry_night
                        );
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load image from drawable: " + "starry_night");
                        e.printStackTrace();
                        break;
                    }

                    final Filter akbarHunting;
                    try {
                        akbarHunting = new ImageDetectionFilter(
                                MainActivity.this, R.drawable.akbar_hunting_with_cheetahs
                        );
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load image from drawable: " + "akbar_hunting_with_cheetahs");
                        e.printStackTrace();
                        break;
                    }

                    mImageDetectionFilters = new Filter[]{
                            new BlankFilter(),
                            starryNight,
                            akbarHunting
                    };

                    break;

                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
            mImageDetectionFilterIndex = savedInstanceState.getInt(STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mImageDetectionFilterIndex = 0;
        }

        final android.hardware.Camera camera;

        //TODO: in case of probs, check here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(mCameraIndex, cameraInfo);
            isCameraFrontFacing = (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
            numCameras = android.hardware.Camera.getNumberOfCameras();
            camera = android.hardware.Camera.open(mCameraIndex);
        } else {
            // for older version phones, assume only rear camera
            isCameraFrontFacing = false;
            numCameras = 1;
            camera = android.hardware.Camera.open();
        }

        final android.hardware.Camera.Parameters parameters = camera.getParameters();
        camera.release();
        mSupportedImageSizes = parameters.getSupportedPreviewSizes();
        final android.hardware.Camera.Size size = mSupportedImageSizes.get(mImageSizeIndex);

        mCameraView = new JavaCameraView(this, mCameraIndex);
        mCameraView.setMaxFrameSize(size.width, size.height);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        // save current camera index
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

        // save the current size index
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImageSizeIndex);

        // save current filter index
        savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mImageDetectionFilterIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    @SuppressLint("NewApi")
    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    protected void onPause() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //TODO: in case of probs, check here with the version num
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        mIsMenuLocked = false;
    }

    @Override
    protected void onDestroy() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.acitivity_main, menu);
        if (numCameras < 2) {
            menu.removeItem(R.id.menu_next_camera);
        }
        int numSupportedImageSizes = mSupportedImageSizes.size();
        if (numSupportedImageSizes > 1) {
            final SubMenu sizeSubMenu = menu.addSubMenu(R.string.menu_image_size);
            for (int i = 0; i < numSupportedImageSizes; i++) {
                final android.hardware.Camera.Size size = mSupportedImageSizes.get(i);
                sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE, String.format("%dx%d", size.width, size.height));
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mIsMenuLocked) {
            return true;
        }
        if (item.getGroupId() == MENU_GROUP_ID_SIZE) {
            mImageSizeIndex = item.getItemId();
            recreate();
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_next_camera:
                mIsMenuLocked = true;
                // if another camera index, then recreate the activity
                mCameraIndex++;
                if (mCameraIndex == numCameras) {
                    mCameraIndex = 0;
                }
//                mImageSizeIndex = 0;
                recreate();
                return true;

            case R.id.menu_take_photo:
                mIsMenuLocked = true;
                mIsPhotoPending = true;
                return true;

            case R.id.menu_next_image_detection_filter:
                mImageDetectionFilterIndex++;
                if(mImageDetectionFilterIndex == mImageDetectionFilters.length)
                {
                    mImageDetectionFilterIndex = 0;
                }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();

        // Apply the active filters.
        if (mImageDetectionFilters != null) {
            mImageDetectionFilters[mImageDetectionFilterIndex].apply(
                    rgba, rgba);
        }

        if (mIsPhotoPending) {
            mIsPhotoPending = false;
            capturePhoto(rgba);
        }

        if (isCameraFrontFacing) {
            Core.flip(rgba, rgba, 1); // flip to same variable
        }

        return rgba;
    }

    private void capturePhoto(final Mat rgba) {
        final long currTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
        ).toString();
        final String albumPath = galleryPath + File.separator + appName;
        final String photoPath = albumPath + File.separator + currTimeMillis + AfterCaptureActivity.PHOTO_FILE_EXTENSION;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE, AfterCaptureActivity.PHOTO_MIME_TYPE);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currTimeMillis);

        // check album dir exists
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album dir at " + albumPath);
            onCapturePhotoFailed();
            return;
        }

        // create photo
        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPath, mBgr)) {
            Log.e(TAG, "Failed to save photo at " + photoPath);
            onCapturePhotoFailed();
        }
        Log.d(TAG, "Photo saved to " + photoPath);

        // insert photo on media store
        Uri uri = null;
        try {
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo in media store");
            e.printStackTrace();
            // delete photo
            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete uninserted photo");
                onCapturePhotoFailed();
                return;
            }
        }
        // show a toast
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Photo captured", Toast.LENGTH_SHORT).show();
            }
        });

        // open the photo in AfterCaptureActivity
        final Intent intent = new Intent(this, AfterCaptureActivity.class);
        intent.putExtra(AfterCaptureActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(AfterCaptureActivity.EXTRA_PHOTO_DATA_PATH, photoPath);
        startActivity(intent);
    }

    private void onCapturePhotoFailed() {
        mIsMenuLocked = false;
        final String errMsg = getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, errMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
