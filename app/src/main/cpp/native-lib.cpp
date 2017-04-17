#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

extern "C" {


void
Java_msgsd_mobapps_scenator5_MainActivity_objDetection(JNIEnv *env, jclass type, jlong addrGray,
                                                       jlong addrRgba) {
    Mat &mGr = *(Mat *) addrGray;
    Mat &mRgb = *(Mat *) addrRgba;
    vector<KeyPoint> v;

    Ptr<FeatureDetector> detector = FastFeatureDetector::create(50);
    detector->detect(mGr, v);
    for (unsigned int i = 0; i < v.size(); i++) {
        const KeyPoint &kp = v[i];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255, 0, 0, 255));
    }


}

}
