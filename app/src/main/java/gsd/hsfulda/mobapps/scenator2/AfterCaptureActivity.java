package gsd.hsfulda.mobapps.scenator2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

public class AfterCaptureActivity extends AppCompatActivity {

    public static final String PHOTO_FILE_EXTENSION = ".png";
    public static final String PHOTO_MIME_TYPE = "image/png";
    public static final String EXTRA_PHOTO_URI =
            "cgsd.hsfulda.mobapps.scenator2.AfterCaptureActivity.extra.PHOTO_URI";
    public static final String EXTRA_PHOTO_DATA_PATH =
            "cgsd.hsfulda.mobapps.scenator2.AfterCaptureActivity.extra.PHOTO_DATA_PATH";

    private Uri mUri;
    private String mDataPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mUri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
        mDataPath = intent.getStringExtra(EXTRA_PHOTO_DATA_PATH);

        final ImageView imageView = new ImageView(this);
        imageView.setImageURI(mUri);

        setContentView(imageView);
    }

}
