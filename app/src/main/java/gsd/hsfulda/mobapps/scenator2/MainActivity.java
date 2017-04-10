package gsd.hsfulda.mobapps.scenator2;

import android.graphics.Camera;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.List;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // tag for log output
    private static final String TAG = MainActivity.class.getSimpleName();

    // a key to store the index of active camera
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    // and key for active image size
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";

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

        if (savedInstanceState != null)
        {
            mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
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

    public void onSaveInstanceState(Bundle savedInstanceState){
        // save current camera index
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

        // save the current size index
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImageSizeIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    protected void onPause() {
        if (mCameraView != null)
        {
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
        if (mCameraView != null){
            mCameraView.disableView();
        }
        super.onDestroy();
    }

//    @Override
//    public boolean onCreateOptionsMenu(final Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
//    };

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
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return null;
    }
}
