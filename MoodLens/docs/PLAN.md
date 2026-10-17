# Plan → Organize → Execute

## 1. Inspect and plan

Inspected both submitted Android ZIPs, all five screenshot images in the PPT, the training notebook's source and class-order outputs, and the supplied TFLite graph metadata. The optimized project was chosen as the base because it already included temporal score smoothing and real chart views. The older archive's generated build files were not treated as proof that the updated app builds.

Preserve the PPT's light purple/teal card theme and five core workflows. Address missing model integration and lifecycle issues before visual polish. Do not invent detections, calibrated scores, improved accuracy, successful builds, or device test results.

## 2. Organize

Create a clean `MoodLens/` root. Remove IDE state, Gradle caches, generated build outputs, local machine SDK paths, automatic JDK download configuration, and obsolete setup notes. Keep existing package/application identity and the Room database version, and copy in the actual provided model.

Separate shared navigation/insets (`MoodLensActivity`), background model ownership (`EmotionEngine`), pure score math (`EmotionMath`), emotion presentation (`EmotionStyle`), and database access (`DetectionStore`). Keep CameraX, Kotlin + Java, XML, ML Kit, LiteRT, and Room as in the proposal.

## 3. Execute

Implement theme-aligned screen layouts and icons; connect navigation, source/time filters, select-all/clear-all actions, photo bars and overlay; guard stale camera callbacks and screen-close disposal; sample and orient photos off the UI thread; fix unused crop/flipped-image cleanup; memory-map the model; remove unsupported per-class calibration.

Validate resource XML, resource references, explicit view casts, registered screen/view classes, model tensor/operator metadata, label order, uncompressed packaging configuration, and exact DAO SQL. Add JVM score tests for later Android Studio execution. Package the clean source ZIP and explain verification limits.

## Follow-up: approved logo and footer removal

Use the exact approved generated coral-and-gold logo as the Home hero PNG. Remove only the footer identified in the supplied screenshot from Photo Analysis. Keep the purple/teal card theme. Remove the obsolete hero-face drawable, align version and documentation, rerun existing consistency checks, verify the logo's transparency and byte identity, and package the full project with the unchanged model.
