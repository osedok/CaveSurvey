package com.astoev.cave.survey.activity.main;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.astoev.cave.survey.Constants;
import com.astoev.cave.survey.R;
import com.astoev.cave.survey.activity.MainMenuActivity;
import com.astoev.cave.survey.activity.UIUtilities;
import com.astoev.cave.survey.activity.dialog.ConfirmDeleteDialog;
import com.astoev.cave.survey.activity.dialog.ConfirmationDialog;
import com.astoev.cave.survey.activity.dialog.ConfirmationOperation;
import com.astoev.cave.survey.activity.dialog.DeleteHandler;
import com.astoev.cave.survey.activity.dialog.VectorDialog;
import com.astoev.cave.survey.activity.draw.DrawingActivity;
import com.astoev.cave.survey.activity.map.MapUtilities;
import com.astoev.cave.survey.fragment.LocationFragment;
import com.astoev.cave.survey.model.Gallery;
import com.astoev.cave.survey.model.Leg;
import com.astoev.cave.survey.model.Note;
import com.astoev.cave.survey.model.Photo;
import com.astoev.cave.survey.model.Point;
import com.astoev.cave.survey.model.Vector;
import com.astoev.cave.survey.service.bluetooth.BTMeasureResultReceiver;
import com.astoev.cave.survey.service.bluetooth.BTResultAware;
import com.astoev.cave.survey.service.bluetooth.util.MeasurementsUtil;
import com.astoev.cave.survey.service.orientation.AzimuthChangedListener;
import com.astoev.cave.survey.service.orientation.SlopeChangedListener;
import com.astoev.cave.survey.util.DaoUtil;
import com.astoev.cave.survey.util.FileStorageUtil;
import com.astoev.cave.survey.util.PointUtil;
import com.astoev.cave.survey.util.StringUtils;
import com.j256.ormlite.misc.TransactionManager;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by IntelliJ IDEA.
 * User: astoev
 * Date: 2/17/12
 * Time: 1:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class PointActivity extends MainMenuActivity implements AzimuthChangedListener, SlopeChangedListener, BTResultAware, View.OnTouchListener, DeleteHandler {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQIEST_EDIT_NOTE = 2;

    private static final String VECTOR_DIALOG = "vector_dialog";

    private String mNewNote = null;

    private String mCurrentPhotoPath;

    /**
     * Current leg to work with
     */
    private Leg mCurrentLeg = null;

    private BTMeasureResultReceiver mReceiver = new BTMeasureResultReceiver(this);

    // swipe detection variables
    private float x1, x2;
    private static final int MIN_SWIPE_DISTANCE = 150;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.point);
        mNewNote = null;

        // initialize the view with leg data only if the activity is new
        if (savedInstanceState == null) {
            loadPointData();
        }

        // handle double click for reading built-in azimuth
        EditText azimuth = (EditText) findViewById(R.id.point_azimuth);
        MeasurementsUtil.bindAzimuthAwareField(azimuth, getSupportFragmentManager());

        // handle double click for reading built-in slope
        EditText slope = (EditText) findViewById(R.id.point_slope);
        MeasurementsUtil.bindSlopeAwareField(slope, getSupportFragmentManager());

        Leg legEdited = getCurrentLeg();
        if (legEdited != null) {
            GPSActivity.initSavedLocationContainer(legEdited.getFromPoint(), this, savedInstanceState);
        }

        loadLegVectors(legEdited);

        // make swipe work
        View view = findViewById(R.id.point_main_view);
        view.setOnTouchListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mReceiver.resetMeasureExpectations();

        //check if location is added and returned back to this activity
        Fragment fragment = this.getSupportFragmentManager().findFragmentById(R.id.saved_location_container);
        if (!(fragment instanceof LocationFragment)) {
            Leg legEdited = getCurrentLeg();
            GPSActivity.initSavedLocationContainer(legEdited.getFromPoint(), this, null);
        }

        loadLegVectors(getCurrentLeg());
    }

    /**
     * @see android.support.v4.app.FragmentActivity#onPause()
     */
    @Override
    protected void onPause() {
        MeasurementsUtil.closeDialogs();
        super.onPause();
    }

    /**
     * Shows the current leg as activity title
     *
     * @see com.astoev.cave.survey.activity.BaseActivity#getScreenTitle()
     */
    @Override
    protected String getScreenTitle() {
        try {
            StringBuilder builder = new StringBuilder(getString(R.string.leg));
            builder.append(getCurrentLeg().buildLegDescription(true));

            return builder.toString();
        } catch (SQLException e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to create activity's name", e);
        }
        return null;
    }

    private void loadPointData() {
        Log.i(Constants.LOG_TAG_UI, "Loading point data");

        try {

            Leg legEdited = getCurrentLeg();

            // up
            EditText up = (EditText) findViewById(R.id.point_up);
            StringUtils.setNotNull(up, legEdited.getTop());
            mReceiver.bindBTMeasures(up, Constants.Measures.up, false, null);

            // down
            EditText down = (EditText) findViewById(R.id.point_down);
            StringUtils.setNotNull(down, legEdited.getDown());
            mReceiver.bindBTMeasures(down, Constants.Measures.down, false, null);

            // left
            EditText left = (EditText) findViewById(R.id.point_left);
            StringUtils.setNotNull(left, legEdited.getLeft());
            mReceiver.bindBTMeasures(left, Constants.Measures.left, false, null);

            // right
            EditText right = (EditText) findViewById(R.id.point_right);
            StringUtils.setNotNull(right, legEdited.getRight());
            mReceiver.bindBTMeasures(right, Constants.Measures.right, false, null);

            // distance
            EditText distance = (EditText) findViewById(R.id.point_distance);
            StringUtils.setNotNull(distance, legEdited.getDistance());
            mReceiver.bindBTMeasures(distance, Constants.Measures.distance, false, new Constants.Measures[]{Constants.Measures.angle, Constants.Measures.slope});
            disableIfMiddle(legEdited, distance);

            // azimuth
            EditText azimuth = (EditText) findViewById(R.id.point_azimuth);
            StringUtils.setNotNull(azimuth, legEdited.getAzimuth());
            mReceiver.bindBTMeasures(azimuth, Constants.Measures.angle, false, new Constants.Measures[]{Constants.Measures.distance, Constants.Measures.slope});
            disableIfMiddle(legEdited, azimuth);

            // slope
            EditText slope = (EditText) findViewById(R.id.point_slope);
            slope.setText("0");
            StringUtils.setNotNull(slope, legEdited.getSlope());
            mReceiver.bindBTMeasures(slope, Constants.Measures.slope, false, new Constants.Measures[]{Constants.Measures.angle, Constants.Measures.distance});
            disableIfMiddle(legEdited, slope);

            if (!legEdited.isMiddle()) {
                // fill note_text with its value
                Note note = DaoUtil.getActiveLegNote(legEdited);
                TextView textView = (TextView) findViewById(R.id.point_note_text);
                if (note != null && note.getText() != null) {
                    textView.setText(note.getText());
                    textView.setClickable(true);
                } else if (mNewNote != null) {
                    textView.setText(mNewNote);
                    textView.setClickable(true);
                }
            }

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to render point", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    private void disableIfMiddle(Leg aCurrentLeg, EditText anEditText) {
        anEditText.setEnabled(!aCurrentLeg.isMiddle());
    }

    private boolean saveLeg() {
        try {

            // start validation
            boolean valid = true;
            final EditText distance = (EditText) findViewById(R.id.point_distance);
            valid = valid && UIUtilities.validateNumber(distance, true);

            final EditText azimuth = (EditText) findViewById(R.id.point_azimuth);
            valid = valid && UIUtilities.validateNumber(azimuth, true) && UIUtilities.checkAzimuth(azimuth);

            final EditText slope = (EditText) findViewById(R.id.point_slope);
            valid = valid && UIUtilities.validateNumber(slope, false) && UIUtilities.checkSlope(slope);

            final EditText up = (EditText) findViewById(R.id.point_up);
            valid = valid && UIUtilities.validateNumber(up, false);

            final EditText down = (EditText) findViewById(R.id.point_down);
            valid = valid && UIUtilities.validateNumber(down, false);

            final EditText left = (EditText) findViewById(R.id.point_left);
            valid = valid && UIUtilities.validateNumber(left, false);

            final EditText right = (EditText) findViewById(R.id.point_right);
            valid = valid && UIUtilities.validateNumber(right, false);

            if (!valid) {
                return false;
            }

            Log.i(Constants.LOG_TAG_UI, "Saving leg");

            TransactionManager.callInTransaction(getWorkspace().getDBHelper().getConnectionSource(),
                    new Callable<Integer>() {
                        public Integer call() throws Exception {

                            Leg legEdited = getCurrentLeg();

                            if (getIntent().getBooleanExtra(Constants.GALLERY_NEW, false)) {
                                Gallery newGallery = DaoUtil.createGallery(false);
                                legEdited.setGalleryId(newGallery.getId());
                            }

                            if (legEdited.isNew()) {
                                getWorkspace().getDBHelper().getPointDao().create(legEdited.getToPoint());
                                getWorkspace().getDBHelper().getLegDao().create(legEdited);
                            }

                            // update model
                            legEdited.setDistance(StringUtils.getFromEditTextNotNull(distance));
                            legEdited.setAzimuth(StringUtils.getFromEditTextNotNull(azimuth));
                            legEdited.setSlope(StringUtils.getFromEditTextNotNull(slope));
                            legEdited.setTop(StringUtils.getFromEditTextNotNull(up));
                            legEdited.setDown(StringUtils.getFromEditTextNotNull(down));
                            legEdited.setLeft(StringUtils.getFromEditTextNotNull(left));
                            legEdited.setRight(StringUtils.getFromEditTextNotNull(right));

                            // save leg
                            getWorkspace().getDBHelper().getLegDao().update(legEdited);

                            if (mNewNote != null) {
                                // create new note
                                Note note = new Note(mNewNote);
                                note.setPoint(legEdited.getFromPoint());
                                note.setGalleryId(legEdited.getGalleryId());
                                getWorkspace().getDBHelper().getNoteDao().create(note);
                            }

                            if (legEdited.isMiddle()) {
                                getWorkspace().setActiveLeg(DaoUtil.getLegByToPointId(legEdited.getToPoint().getId()));
                            } else {
                                getWorkspace().setActiveLeg(legEdited);
                            }

                            Log.i(Constants.LOG_TAG_UI, "Saved");
                            UIUtilities.showNotification(R.string.action_saved);
                            return 0;
                        }
                    }
            );
            return true;
        } catch (Exception e) {
            UIUtilities.showNotification(R.string.error);
            Log.e(Constants.LOG_TAG_UI, "Leg not saved", e);
        }
        return false;
    }

    public void noteButton(View aView) {
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra(Constants.LEG_SELECTED, getCurrentLeg().getId());
        intent.putExtra(Constants.LEG_NOTE, mNewNote);
        startActivityForResult(intent, REQIEST_EDIT_NOTE);
    }

    public void saveButton() {
        if (saveLeg()) {
            finish();
        }
    }

    public void drawingButton() {
        Intent intent = new Intent(this, DrawingActivity.class);
        startActivity(intent);
    }

    public void gpsButton() {
        Point parentPoint = getCurrentLeg().getFromPoint();
        Intent intent = new Intent(this, GPSActivity.class);
        intent.putExtra(GPSActivity.POINT, parentPoint);
        startActivity(intent);
    }

    private void vectorButton() {
        VectorDialog dialog = new VectorDialog(getSupportFragmentManager());
        Bundle bundle = new Bundle();
        Leg leg = getCurrentLeg();
        bundle.putSerializable(VectorDialog.LEG, getCurrentLeg());
        dialog.setCancelable(true);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), VECTOR_DIALOG);
    }

    public void deleteButton() {
        try {
            Leg legEdited = getCurrentLeg();
            boolean deleted = DaoUtil.deleteLeg(legEdited);
            if (deleted) {
                UIUtilities.showNotification(R.string.action_deleted);

                // ensure active leg present
                getWorkspace().setActiveLeg(getWorkspace().getLastLeg());

                onBackPressed();
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to delete point", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    public void photoButton() {
        // picture http://www.tutorialforandroid.com/2010/10/take-picture-in-android-with.html
        // https://developer.android.com/training/camera/photobasics.html

        File photoFile;
        try {
            String projectName = getWorkspace().getActiveProject().getName();
            Leg workingLeg = getCurrentLeg();
            Point pointFrom = workingLeg.getFromPoint();
            DaoUtil.refreshPoint(pointFrom);

            // create file where to capture the image
            String galleryName = PointUtil.getGalleryNameForFromPoint(pointFrom, workingLeg.getGalleryId());
            String filePrefix = FileStorageUtil.getFilePrefixForPicture(pointFrom, galleryName);
            photoFile = FileStorageUtil.createPictureFile(this, projectName, filePrefix, FileStorageUtil.JPG_FILE_EXTENSION, true);

        } catch (SQLException e) {
            UIUtilities.showNotification(R.string.error);
            return;
        } catch (Exception e) {
            UIUtilities.showNotification(R.string.export_io_error);
            return;
        }

        // call capture image
        if (photoFile != null) {

            mCurrentPhotoPath = photoFile.getAbsolutePath();

            Log.i(Constants.LOG_TAG_SERVICE, "Going to capture image in: " + photoFile.getAbsolutePath());
            final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    // photo is captured
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent aData) {

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE: {
                    Log.i(Constants.LOG_TAG_SERVICE, "Got image");
                    try {

                        // check if the file really exists
                        if (!FileStorageUtil.isFileExists(mCurrentPhotoPath)) {
                            UIUtilities.showNotification(R.string.export_io_error);
                            break;
                        }

                        File pictureFile = new File(mCurrentPhotoPath);

                        // broadcast that the file is added
                        FileStorageUtil.notifyPictureAddedToGalery(this, pictureFile);

                        Log.i(Constants.LOG_TAG_SERVICE, "Image captured in: " + mCurrentPhotoPath);
                        Photo photo = new Photo();
                        photo.setFSPath(mCurrentPhotoPath);

                        Leg legEdited = getCurrentLeg();
                        Point currPoint = DaoUtil.getPoint(legEdited.getFromPoint().getId());
                        photo.setPoint(currPoint);
                        photo.setGalleryId(legEdited.getGalleryId());

                        getWorkspace().getDBHelper().getPhotoDao().create(photo);
                        Log.i(Constants.LOG_TAG_SERVICE, "Image stored");

                    } catch (SQLException e) {
                        Log.e(Constants.LOG_TAG_UI, "Picture object not saved", e);
                        UIUtilities.showNotification(R.string.error);
                    }
                }
                break;
                case REQIEST_EDIT_NOTE:
                    mNewNote = aData.getStringExtra("note");
                    TextView textView = (TextView) findViewById(R.id.point_note_text);
                    textView.setText(mNewNote);
                    textView.setClickable(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Leg currLeg = getCurrentLeg();
        if (!currLeg.isNew() && currLeg.isMiddle()) {
            try {
                getWorkspace().setActiveLeg(DaoUtil.getLegByToPointId(currLeg.getToPoint().getId()));
            } catch (SQLException e) {
                Log.e(Constants.LOG_TAG_UI, "Failed to locate parent leg", e);
                UIUtilities.showNotification(R.string.error);
            }
        }
        finish();
    }

    /**
     * @see com.astoev.cave.survey.activity.MainMenuActivity#getChildsOptionsMenu()
     */
    @Override
    protected int getChildsOptionsMenu() {
        return R.menu.pointmenu;
    }

    /**
     * @see com.astoev.cave.survey.activity.MainMenuActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(Constants.LOG_TAG_UI, "Point activity's menu selected - " + item.toString());

        switch (item.getItemId()) {
            case R.id.point_action_save:
                saveButton();
                return true;
            case R.id.point_action_note:
                noteButton(null);
                return true;
            case R.id.point_action_draw:
                drawingButton();
                return true;
            case R.id.point_action_gps:
                gpsButton();
                return true;
            case R.id.point_action_photo:
                photoButton();
                return true;
            case R.id.point_action_add_vector:
                vectorButton();
                return true;
            case R.id.point_action_delete:
                deleteButton();
                return true;
            case R.id.point_action_reverse:
                confirmReverseLeg();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // need to call super to prepare menu
        boolean flag = super.onPrepareOptionsMenu(menu);

        Leg currLeg = getCurrentLeg();

        if (currLeg.isMiddle()) {
            MenuItem noteMenuItem = menu.findItem(R.id.point_action_note);
            noteMenuItem.setVisible(false);

            MenuItem drawingMenuItem = menu.findItem(R.id.point_action_draw);
            drawingMenuItem.setVisible(false);

            MenuItem gpsMenuItem = menu.findItem(R.id.point_action_gps);
            gpsMenuItem.setVisible(false);

            MenuItem vectorsMenuItem = menu.findItem(R.id.point_action_add_vector);
            vectorsMenuItem.setVisible(false);

            MenuItem reverseMenuItem = menu.findItem(R.id.point_action_reverse);
            reverseMenuItem.setVisible(false);
        }

        // check if the device has a camera
        PackageManager packageManager = getPackageManager();
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) || currLeg.isMiddle()) {
            // if there is no camera remove the photo button
            MenuItem photoMenuItem = menu.findItem(R.id.point_action_photo);
            photoMenuItem.setVisible(false);
        }

        // allow vectors for saved legs
        if (mCurrentLeg != null && !mCurrentLeg.isNew() && !currLeg.isMiddle()) {
            MenuItem photoMenuItem = menu.findItem(R.id.point_action_add_vector);
            photoMenuItem.setVisible(true);
        }

        try {
            if (Leg.canDelete(getCurrentLeg())) {
                MenuItem deleteMenuOption = menu.findItem(R.id.point_action_delete);
                deleteMenuOption.setVisible(true);
                return flag;
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to update menu", e);
            UIUtilities.showNotification(R.string.error);
        }

        // delete disabled by default
        MenuItem deleteMenuOption = menu.findItem(R.id.point_action_delete);
        deleteMenuOption.setEnabled(false);
        return flag;
    }

    /**
     * Helper method to build the current leg. If the leg is new will create from and to points. The id of the
     * new leg will always be null. If the leg is currently edited it is obtained from the workspace.
     *
     * @return Leg instance
     */
    private Leg getCurrentLeg() {
        if (mCurrentLeg == null) {
            Bundle extras = getIntent().getExtras();
            try {
                if (extras != null) {
                    int currentLegSelectedId = extras.getInt(Constants.LEG_SELECTED);

                    if (currentLegSelectedId > 0) {
                        mCurrentLeg = DaoUtil.getLeg(currentLegSelectedId);
                        Log.i(Constants.LOG_TAG_UI, "PointView for leg with id: " + currentLegSelectedId);
                        return mCurrentLeg;
                    }
                }

                Log.i(Constants.LOG_TAG_UI, "Create new leg");
                Integer currGalleryId = getWorkspace().getActiveGalleryId();

                // another leg, starting from the latest in the gallery
                boolean newGalleryFlag = extras != null && extras.getBoolean(Constants.GALLERY_NEW, false);
                Point newFrom, newTo;
                if (newGalleryFlag) {
                    newFrom = getWorkspace().getActiveLeg().getFromPoint();
                    newTo = PointUtil.createSecondPoint();
                    currGalleryId = null;
                } else {
                    newFrom = getWorkspace().getLastGalleryPoint(currGalleryId);
                    newTo = PointUtil.generateNextPoint(currGalleryId);
                }

                Log.i(Constants.LOG_TAG_UI, "PointView for new point");
                mCurrentLeg = new Leg(newFrom, newTo, getWorkspace().getActiveProject(), currGalleryId);
            } catch (SQLException sqle) {
                throw new RuntimeException(sqle);
            }
        }
        return mCurrentLeg;
    }

    /**
     * @see com.astoev.cave.survey.service.orientation.AzimuthChangedListener#onAzimuthChanged(float)
     */
    @Override
    public void onAzimuthChanged(float newValueArg) {
        final EditText azimuth = (EditText) findViewById(R.id.point_azimuth);
        azimuth.setText(String.valueOf(newValueArg));
    }

    /**
     * @see com.astoev.cave.survey.service.orientation.SlopeChangedListener#onSlopeChanged(float)
     */
    @Override
    public void onSlopeChanged(float newValueArg) {
        final EditText slope = (EditText) findViewById(R.id.point_slope);
        slope.setText(String.valueOf(newValueArg));
    }

    public void loadLegVectors(Leg aLegEdited) {

        if (aLegEdited.isNew()) {
            // no vectors anyway
            return;
        }

        if (aLegEdited.isMiddle()) {
            // no need to proceed
            return;
        }

        try {
            TableLayout vectorsTable = (TableLayout) findViewById(R.id.point_vectors_table);

            // data
            List<Vector> vectorsList = DaoUtil.getLegVectors(aLegEdited);
            if (vectorsList != null && vectorsList.size() > 0) {

                // remove old data
                vectorsTable.removeAllViews();

                // set headers
                TableRow header = new TableRow(this);
                TextView counterHeader = new TextView(this);
                counterHeader.setText(getString(R.string.point_vectors_counter));
                header.addView(counterHeader);
                TextView distanceHeader = new TextView(this);
                distanceHeader.setText(getString(R.string.distance));
                header.addView(distanceHeader);
                TextView azimuthHeader = new TextView(this);
                azimuthHeader.setText(getString(R.string.azimuth));
                header.addView(azimuthHeader);
                TextView slopeHeader = new TextView(this);
                slopeHeader.setText(getString(R.string.slope));
                header.addView(slopeHeader);
                vectorsTable.addView(header);

                // populate data
                int index = 1;
                for (final Vector v : vectorsList) {
                    TableRow row = new TableRow(this);
                    TextView id = new TextView(this);
                    id.setText(String.valueOf(index));
                    id.setGravity(Gravity.CENTER);
                    row.addView(id);

                    TextView distance = new TextView(this);
                    distance.setText(StringUtils.floatToLabel(v.getDistance()));
                    distance.setGravity(Gravity.CENTER);
                    row.addView(distance);

                    TextView azimuth = new TextView(this);
                    azimuth.setText(StringUtils.floatToLabel(v.getAzimuth()));
                    azimuth.setGravity(Gravity.CENTER);
                    row.addView(azimuth);

                    TextView angle = new TextView(this);
                    angle.setText(StringUtils.floatToLabel(v.getSlope()));
                    angle.setGravity(Gravity.CENTER);
                    row.addView(angle);

                    final int finalIndex = index;
                    final Vector finalVector = v;

                    row.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {

                            // instantiate delete dialog and pass the vector
                            String message = getString(R.string.point_vectors_delete, finalIndex);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(ConfirmDeleteDialog.ELEMENT, finalVector);
                            bundle.putString(ConfirmDeleteDialog.MESSAGE, message);

                            ConfirmDeleteDialog deleteVecotrDialog = new ConfirmDeleteDialog();
                            deleteVecotrDialog.setArguments(bundle);
                            deleteVecotrDialog.show(getSupportFragmentManager(), ConfirmDeleteDialog.DELETE_VECTOR_DIALOG);
                            return true;
                        }
                    });

                    vectorsTable.addView(row);
                    index++;
                }

                vectorsTable.setVisibility(View.VISIBLE);
            } else {
                vectorsTable.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to load vectors", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    @Override
    public void onReceiveMeasures(Constants.Measures aMeasureTarget, float aMeasureValue) {
        switch (aMeasureTarget) {
            case distance:
                Log.i(Constants.LOG_TAG_UI, "Got distance " + aMeasureValue);
                populateMeasure(aMeasureValue, R.id.point_distance);
                break;

            case angle:
                Log.i(Constants.LOG_TAG_UI, "Got angle " + aMeasureValue);
                populateMeasure(aMeasureValue, R.id.point_azimuth);
                break;

            case slope:
                Log.i(Constants.LOG_TAG_UI, "Got slope " + aMeasureValue);
                populateMeasure(aMeasureValue, R.id.point_slope);
                break;

            case up:
                Log.i(Constants.LOG_TAG_UI, "Got up " + aMeasureValue);
                populateMeasure(aMeasureValue, R.id.point_up);
                break;

            case down:
                Log.i(Constants.LOG_TAG_UI, "Got down " + aMeasureValue);
                populateMeasure(aMeasureValue, R.id.point_down);
                break;

            case left:
                Log.i(Constants.LOG_TAG_UI, "Got left " + aMeasureValue);
                populateMeasure(aMeasureValue, R.id.point_left);
                break;

            case right:
                Log.i(Constants.LOG_TAG_UI, "Got right " + aMeasureValue);
                populateMeasure(aMeasureValue, R.id.point_right);
                break;

            default:
                Log.i(Constants.LOG_TAG_UI, "Ignore type " + aMeasureTarget);
        }
    }

    private void populateMeasure(float aMeasure, int anEditTextId) {
        EditText field = (EditText) findViewById(anEditTextId);
        StringUtils.setNotNull(field, aMeasure);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // detect swipes to switch the current point
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_SWIPE_DISTANCE) {
                    try {
                        Leg currLeg = getCurrentLeg();
                        if (!currLeg.isNew()) {
                            Leg nextPoint;
                            if (deltaX > 0) {
                                Log.i(Constants.LOG_TAG_UI, "swipe right");
                                nextPoint = DaoUtil.getGalleryPrevLeg(currLeg);
                            } else {
                                Log.i(Constants.LOG_TAG_UI, "swipe left");
                                nextPoint = DaoUtil.getGalleryNextLeg(getCurrentLeg());
                            }
                            swipeTo(nextPoint);
                        }
                    } catch (Exception e) {
                        Log.e(Constants.LOG_TAG_UI, "Failed to swipe", e);
                        UIUtilities.showNotification(R.string.error);
                    }
                }
                break;
        }

        // propagate back to allow scrolls etc
        return super.onTouchEvent(event);
    }

    private void swipeTo(Leg aNextLeg) {
        if (aNextLeg != null) {
            Intent intent = new Intent(PointActivity.this, PointActivity.class);
            intent.putExtra(Constants.LEG_SELECTED, aNextLeg.getId());
            getWorkspace().setActiveLeg(aNextLeg);
            startActivity(intent);
            finish();
        } else {
            UIUtilities.showNotification(R.string.point_swipe_not_possible);
        }
    }

    /**
     * Executed when menu button for reverse leg is selected. It shows confirmation dialog to
     * confirm the operation of reversing.
     */
    private void confirmReverseLeg() {
        try {

            // validate
            final EditText azimuth = (EditText) findViewById(R.id.point_azimuth);
            final EditText slope = (EditText) findViewById(R.id.point_slope);

            if (UIUtilities.validateNumber(azimuth, true) && UIUtilities.checkAzimuth(azimuth)
                    && UIUtilities.validateNumber(slope, false) && UIUtilities.checkSlope(slope) ) {

                // build and show confirmation dialog
                String message = getString(R.string.main_reverse_message, mCurrentLeg.buildLegDescription());
                Bundle bundle = new Bundle();
                bundle.putSerializable(ConfirmationDialog.OPERATION, ConfirmationOperation.REVERSE_LEG);
                bundle.putString(ConfirmationDialog.MESSAGE, message);
                bundle.putString(ConfirmationDialog.TITLE, getString(R.string.title_warning));

                ConfirmationDialog confirmationDialog = new ConfirmationDialog();
                confirmationDialog.setArguments(bundle);
                confirmationDialog.show(getSupportFragmentManager(), ConfirmationDialog.CONFIRM_DIALOG);

            } else {
                // TODO Alexander, what if not valid? Never handled before
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to build reverse dialog", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    @Override
    public boolean confirmOperation(ConfirmationOperation operationArg) {
        if (ConfirmationOperation.REVERSE_LEG.equals(operationArg)){
            reverseLeg();
            return true;
        } else {
            return super.confirmOperation(operationArg);
        }
    }

    /**
     * Reverses the leg after confirmation from the confirmation dialog
     */
    private void reverseLeg() {
        Log.i(Constants.LOG_TAG_UI, "Reverse leg");
        // validate
        final EditText azimuth = (EditText) findViewById(R.id.point_azimuth);
        final EditText slope = (EditText) findViewById(R.id.point_slope);

        // if values are present update them in the UI only, they will be persisted on "save"
//        try {
            Float currAzimuth = MapUtilities.getAzimuthInDegrees(StringUtils.getFromEditTextNotNull(azimuth));
            if (currAzimuth != null) {
                Float reversedAzimuth = MapUtilities.add90Degrees(MapUtilities.add90Degrees(currAzimuth));
                populateMeasure(reversedAzimuth, R.id.point_azimuth);
            }
            Float currSlope = StringUtils.getFromEditTextNotNull(slope);
            if (currSlope != null && currSlope != 0) {
                populateMeasure(-currSlope, R.id.point_slope);
            }
//        } catch (Exception e) {
//            Log.e(Constants.LOG_TAG_UI, "Failed to reverse leg", e);
//            UIUtilities.showNotification(R.string.error);
//        }
    }


    @Override
    public void delete(Serializable vectorArg) {
        Log.i(Constants.LOG_TAG_UI, "Delete vector");
        try {
            if (vectorArg != null && vectorArg instanceof Vector ) {
                DaoUtil.deleteVector((Vector) vectorArg);
                UIUtilities.showNotification(R.string.action_deleted);
                loadLegVectors(getCurrentLeg());
            } else {
                String vectorClass = vectorArg != null ? vectorArg.getClass().getName() : null;
                Log.e(Constants.LOG_TAG_UI, "Failed to delete vector. Passed instance not a Vector but:" + vectorClass);
                UIUtilities.showNotification(R.string.error);
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to delete vector", e);
            UIUtilities.showNotification(R.string.error);
        }
    }
}
