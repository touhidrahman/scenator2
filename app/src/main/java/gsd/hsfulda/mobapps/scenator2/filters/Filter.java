package gsd.hsfulda.mobapps.scenator2.filters;


import org.opencv.core.Mat;

public interface Filter {
    public abstract void apply (final Mat src, final Mat dst);
}
