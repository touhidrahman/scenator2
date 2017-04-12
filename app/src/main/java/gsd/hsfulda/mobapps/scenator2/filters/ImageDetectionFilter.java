package gsd.hsfulda.mobapps.scenator2.filters;

import android.content.Context;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class ImageDetectionFilter implements Filter {

    // ref image
    private final Mat mRefImage;

    // features of ref image
    private final MatOfKeyPoint mRefKeyPoints = new MatOfKeyPoint();

    // descriptors of ref image's features
    private final Mat mRefDescriptors = new Mat();

    // boundary coordinates of ref image (px)
    // CvType = color depth, # of ch, ch layout
    private final Mat mRefCorners = new Mat(4, 1, CvType.CV_32FC2);

    // features of the scene
    private final MatOfKeyPoint mSceneKeyPoints = new MatOfKeyPoint();

    // descriptors of the scene's features
    private final Mat mSceneDescriptors = new Mat();

    // possible scene corners
    private final Mat mCandidateSceneCorners = new Mat(4, 1, CvType.CV_32FC2);

    // good corner coords detected in the scene
    private final Mat mSceneCorners = new Mat(0, 0, CvType.CV_32FC2);

    // good detected corner coords, px as int
    private final MatOfPoint mIntSceneCorners = new MatOfPoint();

    // grayscale version of image
    private final Mat mGraySrc = new Mat();

    // possible matches of scene features and ref features
    private final MatOfDMatch mMatches = new MatOfDMatch();

    // feature detector
    private final FeatureDetector mFD = FeatureDetector.create(FeatureDetector.ORB);

    // descriptor extractor
    private final DescriptorExtractor mDE = DescriptorExtractor.create(DescriptorExtractor.ORB);

    // descriptor matcher
    private final DescriptorMatcher mDM = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

    // outline color of the box around the detected image
    private final Scalar mLineColor = new Scalar(0, 255, 0);

    public ImageDetectionFilter(final Context context, final int refImageResourceID) throws IOException{
        // load from res folder (as BGR mode, default)
        mRefImage = Utils.loadResource(context, refImageResourceID, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        // create grayscale & RGBA of ref image
        final Mat refImageGray = new Mat();
        Imgproc.cvtColor(mRefImage, refImageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mRefImage, refImageGray, Imgproc.COLOR_BGR2RGBA);

        // store ref image's coords (px)
        mRefCorners.put(0, 0, new double[]{0.0, 0.0});
        mRefCorners.put(1, 0, new double[]{refImageGray.cols(), 0.0});
        mRefCorners.put(2, 0, new double[]{refImageGray.cols(), refImageGray.rows()});
        mRefCorners.put(2, 0, new double[]{0.0, refImageGray.rows()});

        // detect ref features and find out descriptors
        mFD.detect(refImageGray, mRefKeyPoints);
        mDE.compute(refImageGray, mRefKeyPoints, mRefDescriptors);
    }


    @Override
    public void apply(Mat src, Mat dst) {

    }
}
