package io.flutter.plugins.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.util.Size;
import io.flutter.plugins.camera.Camera.ResolutionPreset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Provides various utilities for camera. */
public final class CameraUtils {

  private CameraUtils() {}

  static Size computeBestPreviewSize(String cameraName, ResolutionPreset preset) {
    if (preset.ordinal() > ResolutionPreset.high.ordinal()) {
      preset = ResolutionPreset.high;
    }

    CamcorderProfile profile =
        getBestAvailableCamcorderProfileForResolutionPreset(cameraName, preset);
    return new Size(profile.videoFrameWidth, profile.videoFrameHeight);
  }

  static Size computeBestCaptureSize(StreamConfigurationMap streamConfigurationMap) {
    // For still image captures, we use the largest available size.
    return Collections.max(
        Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
        new CompareSizesByArea());
  }

  public static List<Map<String, Object>> getAvailableCameras(Activity activity)
      throws CameraAccessException {
    CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    String[] cameraNames = cameraManager.getCameraIdList();
    List<Map<String, Object>> cameras = new ArrayList<>();
    for (String cameraName : cameraNames) {
      HashMap<String, Object> details = new HashMap<>();
      CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraName);
      details.put("name", cameraName);
      int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
      details.put("sensorOrientation", sensorOrientation);

      int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
      switch (lensFacing) {
        case CameraMetadata.LENS_FACING_FRONT:
          details.put("lensFacing", "front");
          break;
        case CameraMetadata.LENS_FACING_BACK:
          details.put("lensFacing", "back");
          break;
        case CameraMetadata.LENS_FACING_EXTERNAL:
          details.put("lensFacing", "external");
          break;
      }
      cameras.add(details);
    }
    return cameras;
  }

  static CamcorderProfile getBestAvailableCamcorderProfileForResolutionPreset(
      String cameraName, ResolutionPreset preset) {
    int cameraId = Integer.parseInt(cameraName);
    switch (preset) {
        // All of these cases deliberately fall through to get the best available profile.
      case max:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }
      case ultraHigh:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_2160P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_2160P);
        }
      case veryHigh:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
        }
      case high:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
        }
      case medium:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        }
      case low:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QVGA);
        }
      default:
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW)) {
          return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
        } else {
          throw new IllegalArgumentException(
              "No capture session available for current capture session.");
        }
    }
  }

  private static class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow.
      return Long.signum(
          (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
  }

  public static Size getBestCaptureSize(CameraCharacteristics characteristics, ResolutionPreset preset, double targetAspectRatio) {
    final android.graphics.Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
    if (activeArraySize == null) {
      return new Size(0, 0);
    }
    final int maxWidth = activeArraySize.width();
    final int maxHeight = activeArraySize.height();

    final int presetWidth = getDimensionsForPreset(preset).getWidth();
    final int presetHeight = 16 * (int)(presetWidth / (16 * targetAspectRatio));
    if (presetWidth <= maxWidth && presetHeight <= maxHeight) {
      return new Size(presetWidth, presetHeight);
    }
    final int scaledWidth = 16 * ((int)(maxHeight * targetAspectRatio)/ 16);
    if (scaledWidth <= maxWidth) {
      return new Size(scaledWidth, maxHeight);
    }
    final int scaledHeight = 16 * (int)(maxWidth / (16 * targetAspectRatio));
    return new Size(maxWidth, scaledHeight);
  }

  private static Size getDimensionsForPreset(ResolutionPreset preset) {
    switch (preset) {
      case low:
        return new Size(320, 240);
      case medium:
        return new Size(720, 480);
      case high:
        return new Size(1280, 720);
      case veryHigh:
        return new Size(1920, 1080);
      case ultraHigh:
        return new Size(3840, 2160);
      case max:
        return new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    return new Size(0, 0);
  }
}
