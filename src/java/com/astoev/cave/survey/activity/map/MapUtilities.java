package com.astoev.cave.survey.activity.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import com.astoev.cave.survey.Constants;
import com.astoev.cave.survey.model.Option;
import com.astoev.cave.survey.service.Options;

/**
 * Created by astoev on 12/31/13.
 */
public class MapUtilities {
    private static final int[] COLORS = new int[]{Color.YELLOW, Color.RED, Color.GRAY, Color.GREEN, Color.BLUE};

    public static int getNextGalleryColor(int currentCountArg) {
        // assure predictable colors for the galleries, start repeating colors if too many galleries
        int colorIndex = currentCountArg % COLORS.length;
        return COLORS[colorIndex];
    }

    public static Float getMiddleAngle(Float aFirstAzimuth, Float aSecondAzimuth) {
        if (aFirstAzimuth.equals(aSecondAzimuth)) {
            return aFirstAzimuth;
        } else if (Math.abs(aFirstAzimuth - aSecondAzimuth) > Option.MAX_VALUE_AZIMUTH_DEGREES/2) {
            // average, then flip the direction if sum goes above 360
            return addDegrees((aFirstAzimuth + aSecondAzimuth)/2, Option.MAX_VALUE_AZIMUTH_DEGREES/2);
        } else {
            // average for small angles
            return (aFirstAzimuth + aSecondAzimuth)/2;
        }
    }

    public static Float getAzimuthInDegrees(Float anAzimuth) {
        if (null == anAzimuth) {
            return null;
        }

        if (Option.UNIT_DEGREES.equals(Options.getOptionValue(Option.CODE_AZIMUTH_UNITS))) {
            return anAzimuth;
        } else {
            // convert from grads to degrees
            return anAzimuth * Constants.GRAD_TO_DEC;
        }
    }

    public static Float getAzimuthInDegrees(Float anAzimuth, String currUnits) {
        if (null == anAzimuth) {
            return null;
        }

        if (Option.UNIT_DEGREES.equals(currUnits)) {
            return anAzimuth;
        } else {
            // convert from grads to degrees
            return anAzimuth * Constants.GRAD_TO_DEC;
        }
    }

    public static Float getSlopeInDegrees(Float aSlope) {
        if (null == aSlope) {
            return null;
        }

        if (Option.UNIT_DEGREES.equals(Options.getOptionValue(Option.CODE_SLOPE_UNITS))) {
            return aSlope;
        } else {
            // convert from grads to degrees
            return aSlope * Constants.GRAD_TO_DEC;
        }
    }

    public static Float getSlopeInDegrees(Float aSlope, String currUnits) {
        if (null == aSlope) {
            return null;
        }

        if (Option.UNIT_DEGREES.equals(currUnits)) {
            return aSlope;
        } else {
            // convert from grads to degrees
            return aSlope * Constants.GRAD_TO_DEC;
        }
    }

    public static Float applySlopeToDistance(Float aDistance, Float aSlope) {
        if (aSlope == null) {
            return aDistance;
        }
        return Double.valueOf(aDistance * Math.cos(Math.toRadians(aSlope))).floatValue();
    }

    public static Float add90Degrees(Float anAzimuth) {
        return addDegrees(anAzimuth, 90);
    }

    private static Float addDegrees(Float anAzimuth, int numDegrees) {
        float newAngle = anAzimuth + numDegrees;

        if (newAngle < Option.MAX_VALUE_AZIMUTH_DEGREES) {
            return newAngle;
        } else if (newAngle == Option.MAX_VALUE_AZIMUTH_DEGREES) {
            return Float.valueOf(Option.MIN_VALUE_AZIMUTH);
        } else {
            return newAngle - Option.MAX_VALUE_AZIMUTH_DEGREES;
        }
    }

    public static Float minus90Degrees(Float anAzimuth) {

        if (anAzimuth >= 90) {
            return anAzimuth - 90;
        } else {
            return Option.MAX_VALUE_AZIMUTH_DEGREES + anAzimuth - 90;
        }
    }

    public static boolean isSlopeValid(Float aSlope) {
        String currSlopeMeasure = Options.getOptionValue(Option.CODE_SLOPE_UNITS);
        if (Option.UNIT_DEGREES.equals(currSlopeMeasure)) {
            return isSlopeInDegreesValid(aSlope);
        } else { // Option.UNIT_GRADS
            return isSlopeInGradsValid(aSlope);
        }
    }

    public static boolean isAzimuthValid(Float anAzimuth) {
        String currAzimuthMeasure = Options.getOptionValue(Option.CODE_AZIMUTH_UNITS);
        if (Option.UNIT_DEGREES.equals(currAzimuthMeasure)) {
            return isAzimuthInDegreesValid
                    (anAzimuth);
        } else { // Option.UNIT_GRADS
            return isAzimuthInGradsValid(anAzimuth);
        }
    }

    public static float getSlopeOrHorizontallyIfMissing(Float aSlope) {
        if (aSlope == null) {
            return 0f;
        } else {
            return aSlope;
        }
    }

    public static boolean isAzimuthInDegreesValid(Float anAzimuth) {
        return anAzimuth != null && anAzimuth >= 0 && anAzimuth < Option.MAX_VALUE_AZIMUTH_DEGREES;
    }

    public static boolean isAzimuthInGradsValid(Float anAzimuth) {
        return anAzimuth != null && anAzimuth >= 0 && anAzimuth < Option.MAX_VALUE_AZIMUTH_GRADS;
    }

    public static boolean isSlopeInDegreesValid(Float aSlope) {
        return aSlope != null && aSlope >= Option.MIN_VALUE_SLOPE_DEGREES && aSlope <= Option.MAX_VALUE_SLOPE_DEGREES;
    }

    public static boolean isSlopeInGradsValid(Float aSlope) {
        return aSlope != null && aSlope >= Option.MIN_VALUE_SLOPE_GRADS && aSlope <= Option.MAX_VALUE_SLOPE_GRADS;
    }
}
