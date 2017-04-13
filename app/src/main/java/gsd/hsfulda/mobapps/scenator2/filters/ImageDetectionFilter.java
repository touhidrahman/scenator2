package gsd.hsfulda.mobapps.scenator2.filters;

import android.content.Context;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public void apply(final Mat src, final Mat dst) {
        // convert scene to grayscale
        Imgproc.cvtColor(src, mGraySrc, Imgproc.COLOR_RGBA2GRAY);

        // detect scene features, compute descriptors and match with ref
        mFD.detect(mGraySrc, mSceneKeyPoints);
        mDE.compute(mGraySrc, mSceneKeyPoints, mSceneDescriptors);
        mDM.match(mSceneDescriptors, mRefDescriptors, mMatches);

        // try to find target img's corners in the scene img
        findSceneCorners();

        // if corners found, draw outline in target img
        draw(src, dst);
    }


    private void findSceneCorners() {
        final List<DMatch> matchesList = mMatches.toList();
        if (matchesList.size() < 4)
        {
            // too few matches
            return;
        }

        final List<KeyPoint> refKeyPointList = mRefKeyPoints.toList();
        final List<KeyPoint> sceneKeyPointList = mSceneKeyPoints.toList();

        // calculate max & min dist between keypoints
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        for (final DMatch match : matchesList)
        {
            final double dist = match.distance;
            if (dist < minDist)
            {
                minDist = dist;
            }
            if (dist > maxDist)
            {
                maxDist = dist;
            }
        }
        double minThreshold = 50.0;
        double acceptableThreshold = 25.0;
        if (minDist > minThreshold)
        {
            // target is lost, discard previously found corners
            mSceneCorners.create(0, 0, mSceneCorners.type());
            return;
        } else if (minDist > acceptableThreshold)
        {
            // target is almost lost, but still keep corners
            return;
        }

        // identify good keypoints based on match distance
        final ArrayList<Point> goodRefPointsList = new ArrayList<Point>();
        final ArrayList<Point> goodScenePointsList = new ArrayList<Point>();
        final double maxGoodMatchDist = 1.75 * minDist;
        for (final DMatch match : matchesList)
        {
            if (match.distance < maxGoodMatchDist)
            {
                goodRefPointsList.add(refKeyPointList.get(match.trainIdx).pt);
                goodScenePointsList.add(sceneKeyPointList.get(match.queryIdx).pt);
            }
        }

        if (goodRefPointsList.size() < 4 || goodScenePointsList.size() < 4)
        {
            // too few good points to find the homography
            return;
        }

        // past this line, we have enough good points to find homography
        // convert the matched points to MatOfPoint2f format, as
        // required by the Calib3d.findHomography function.
        final MatOfPoint2f goodRefPoints = new MatOfPoint2f();
        final MatOfPoint2f goodScenePoints = new MatOfPoint2f();
        goodRefPoints.fromList(goodRefPointsList);
        goodScenePoints.fromList(goodScenePointsList);

        // find homography
        final Mat homography = Calib3d.findHomography(goodRefPoints, goodScenePoints);

        // use the homography to project ref corner coords into scene coords
        Core.perspectiveTransform(mRefCorners, mCandidateSceneCorners, homography);

        // convert scene corners to int as reqd by Imgproc.isContourConvex func
        mCandidateSceneCorners.convertTo(mIntSceneCorners, CvType.CV_32S);

        // check the corners form a convex polygon
        // if not then detection is false
        if (Imgproc.isContourConvex(mIntSceneCorners))
        {
            // valid
            mCandidateSceneCorners.copyTo(mSceneCorners);
        }
    }


    private void draw(Mat src, Mat dst) {
        if (dst != src)
        {
            src.copyTo(dst);
        }

        if (mSceneCorners.height() < 4)
        {
            // target not found, give user a que
            int ht = mRefImage.height();
            int wd = mRefImage.width();
            final int maxDimen = Math.min(dst.width(), dst.height()) / 2;
            final double aspectRatio = wd / (double) ht;
            if (ht > wd) {
                ht = maxDimen;
                wd = (int) (ht * aspectRatio);
            } else {
                wd = maxDimen;
                ht = (int) (wd / aspectRatio);
            }

            // select the region of interest
            final Mat dstRegion = dst.submat(0, ht, 0, wd);

            // copy a resized ref image into the region
            Imgproc.resize(mRefImage, dstRegion, dstRegion.size(), 0.0, 0.0, Imgproc.INTER_AREA);
            return;
        }

        // outline the box
        Imgproc.line(dst, new Point(mSceneCorners.get(0, 0)), new Point(mSceneCorners.get(1, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(1, 0)), new Point(mSceneCorners.get(2, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(2, 0)), new Point(mSceneCorners.get(3, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(3, 0)), new Point(mSceneCorners.get(0, 0)), mLineColor, 4);
    }
}
