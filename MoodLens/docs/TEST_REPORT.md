# Verification report

## MoodLens 1.2 follow-up checks

The existing offline resource/model/SQL checks passed again. Additional checks confirmed:

- The home logo PNG is byte-for-byte identical to the approved coral-and-gold generated image, has RGBA transparency, uses fit-center display, and has no layout tint.
- Exactly one Home hero reference uses the new drawable; the old hero-face drawable and references are absent.
- The screenshot's specific footer is absent from app XML/source.
- The model remains byte-for-byte identical to the attachment.
- The Gradle version code/name and README agree on version 1.2.
- The new ZIP includes every project file, preserves the executable wrapper, and passed CRC/content checks.

Android compilation, native inference, and on-device UI remain unverified for the reasons below.

## Checks executed successfully

`python tools/validate_project.py` passed:

- All Android resource XML parsed.
- Local layout, drawable, color, style, string, and ID references in XML/source resolved.
- Explicit Activity `findViewById<T>` casts matched their XML elements.
- Manifest activities and custom view classes existed.
- TFLite schema/version, declared input/output shapes and types, tensor/operator references, and seven-class label order matched the app's expected contract. The graph declares built-in operators, with no custom operator or embedded softmax in the operator table.
- Gradle configuration explicitly leaves `.tflite` assets uncompressed for memory mapping.
- The exact DAO SQL ran against an in-memory SQLite fixture: all/live/camera/gallery filtering, compatibility with legacy camera source strings, counts, deterministic ordering, page offsets, and clearing.

The packaged model's SHA-256 was compared with the attached model. ZIP integrity was checked. These checks are structural and database-query tests, not a full Android validation.

## Build attempt

Running `./gradlew --version` failed before Gradle could start:

```
java: error while loading shared libraries: libjli.so: cannot open shared object file: No such file or directory
```

No Android SDK was found in the available environment. Kotlin/Java compilation, AAPT resource compilation, Gradle dependency resolution, Android Lint, JVM JUnit tests, instrumentation tests, and APK assembly were not executed successfully. No installable APK is included.

## Device/model tests still required

The supplied TFLite model was inspected but not invoked; runtime operator support, prediction outputs, exported-model parity with PyTorch, camera performance, and FER accuracy are unverified. No metric is claimed. The new XML screens were not rendered on Android here.

Before treating the app as device-tested:

1. Build with JDK 17 and SDK 35 using `./gradlew testDebugUnitTest lintDebug assembleDebug`.
2. Test on a real device: grant/deny camera permission, recover via Settings, and run with front and rear cameras (including a single-camera device).
3. Switch cameras repeatedly during analysis; background/resume the app; close screens while model loading, detecting a face, or inferring; check Logcat for crashes and resource errors.
4. Use gallery photos with EXIF rotation and mirrored orientations, a large image, a damaged file, no face, and multiple faces. Confirm only the largest face is analyzed and the overlay aligns.
5. Select/clear/apply targets and reopen the screen. Verify target camera highlights the top predicted class only when selected and sufficiently confident; filters do not suppress other model classes.
6. Check real history after photo/live runs, every source filter, more than 50 entries, confirmed clearing, and the seven-/thirty-day analytics windows.
7. Check the PPT-derived UI at small widths, landscape, large font settings, TalkBack, and Android 15 system bars. Chart/overlay typography at very large font settings needs device review.
8. Evaluate prediction quality with labeled data and measure latency on the target phone. Validate preprocessing/export parity before drawing accuracy conclusions.

## Added tests awaiting a working Android build

`EmotionMathTest.kt`: extreme logits, equal-logit normalization, rejection of non-finite model output, and label-based score smoothing when score ranks change. Existing sample tests are retained.
