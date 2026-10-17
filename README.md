# MoodLens — Facial Expression Recognition Android App

**An Android application that uses deep learning to recognise facial expressions in real time.**

Developed for the **HIT401 Capstone Project | Semester 2, 2026 | Charles Darwin University**


### Team Members

- **Fabiha Sultana Anushe** — 372205
- **Hoang Lan Dao** — 375910
- **Akshata Bhusal** — 377164
- **Jeremy Immanuel Situmeang** — 374594


## About MoodLens

MoodLens is an Android application that uses computer vision and deep learning to recognise facial expressions through a phone's camera or photographs.

We developed this project to explore how facial expression recognition models can be trained, improved, and used in a real mobile application.

Our work started with building a custom Convolutional Neural Network (CNN) using the FER-2013 dataset. After evaluating its performance, we explored transfer learning with ConvNeXt-Tiny to improve classification accuracy.

The final model achieved **71.06% test accuracy on FER-2013**, compared with 56.42% from our initial CNN.

We then integrated the trained model into an Android application that supports live detection, photo analysis, target-expression selection, detection history, and analytics.

One of our main goals was to keep the application accessible and privacy-conscious. Facial expression inference runs directly on the Android device rather than requiring images to be uploaded to an external server.

## Features

### Live Facial Expression Detection

MoodLens uses the device's camera to detect faces and classify their expressions in real time.

The application displays a bounding box around a detected face, together with the predicted expression and its confidence score.

It also supports switching between the front and rear cameras.

To make predictions more stable, the live detection implementation averages recent prediction scores rather than displaying every individual result independently.

### Photo Analysis

Users can analyse photographs in two ways:

- Capture a new photograph using the device's camera.
- Select an existing photograph from the gallery.

The application identifies the largest detected face and displays the predicted expression along with confidence scores for all seven categories.

This feature is useful for examining how the model responds to individual images.

### Target Expression Detection

MoodLens allows users to select one or more facial expressions they want to monitor.

For example, users can select Happy and Surprise and then start Target Camera mode.

The app highlights a detection when the highest-scoring predicted expression matches one of the selected categories and passes the required confidence threshold.

The model continues to evaluate all seven expressions internally. Target selection affects which results are highlighted, not how the neural network classifies images.

### Detection History

The app maintains a local history of eligible facial expression predictions.

Each saved record contains:

- Predicted facial expression
- Model confidence score
- Date and time
- Detection source
- Target-match status

Users can filter their history by live camera, captured photo, or gallery, and can clear saved records when needed.

Room Database is used to manage these records on the device.

### Analytics Dashboard

MoodLens includes an analytics screen to help users explore their recorded predictions.

The dashboard displays the total number of detections, most frequently predicted expression, average model confidence score, expression distribution, and detection activity over time.

Users can view all-time, seven-day, and 30-day summaries.

These statistics represent the model's recorded predictions rather than a measurement of a person's emotional state.

---

## Facial Expressions Recognised

MoodLens classifies seven facial expression categories:

| Expression | Category |
|---|---|
| Happy | Happiness |
| Sad | Sadness |
| Angry | Anger |
| Fear | Fear |
| Surprise | Surprise |
| Disgust | Disgust |
| Neutral | Neutral |

**Note:** Facial expression recognition cannot determine a person's actual feelings or psychological condition. The application's outputs are predictions of visual facial-expression patterns.

---

## Machine Learning Model

### Dataset

We used the **FER-2013 dataset**, which contains 35,887 grayscale facial images representing seven facial expression categories.

The final experiment used the following dataset split:

| Dataset | Images |
|---|---:|
| Training | 22,967 |
| Validation | 5,742 |
| Testing | 7,178 |

### Model Development

Our initial approach used a custom CNN trained to classify facial expressions from FER-2013 images.

The baseline model provided a working recognition pipeline, but its performance also showed the challenges of distinguishing visually similar expressions.

To improve the results, we experimented with **ConvNeXt-Tiny**, a convolutional neural network architecture.

Instead of training the entire feature extractor from scratch, we used pretrained ImageNet weights and fine-tuned the model for seven-class facial expression recognition.

The training process included:

- Image resizing and normalisation
- Training-data augmentation
- Transfer learning with ConvNeXt-Tiny
- Class-weighted cross-entropy
- AdamW optimisation
- Learning-rate scheduling
- Early stopping
- Model validation and evaluation

Model development and training were carried out using Python and PyTorch.

### Model Performance

| Evaluation Metric | Result |
|---|---:|
| Custom CNN test accuracy | 56.42% |
| ConvNeXt-Tiny test accuracy | **71.06%** |
| Accuracy improvement | **+14.64 percentage points** |
| Best validation accuracy | 71.25% |
| Test macro-F1 | 68.77% |
| Balanced test accuracy | 69.85% |

The improved model performed better than our original CNN on the FER-2013 test dataset.

However, dataset accuracy does not guarantee the same performance in real-world camera conditions. Factors such as lighting, head position, image quality, and individual differences can affect recognition results.

The final trained model was prepared for Android deployment using TensorFlow Lite.

---

## How the Application Works

The recognition process follows this pipeline:

**Camera / Gallery → Face Detection → Face Preprocessing → ConvNeXt-Tiny Model → Expression Prediction → Results**

**1. Image Input**

The application receives camera frames through CameraX or loads a photograph selected by the user.

**2. Face Detection**

Google ML Kit identifies the facial region in the image. The detected face is cropped before classification.

**3. Image Preprocessing**

The face image is processed into a grayscale representation and resized to match the model's 224 × 224 input requirements. ImageNet-style normalisation is applied.

**4. Expression Classification**

The trained ConvNeXt-Tiny model runs locally through TensorFlow Lite / LiteRT and generates scores for the seven expression categories.

**5. Display and Storage**

The predicted expression and confidence score are shown to the user. Eligible predictions are saved to the local Room database for history and analytics.

---

## Technologies Used

| Component | Technology |
|---|---|
| Android development | Kotlin, Java, Android Studio |
| Model training | Python, PyTorch, torchvision |
| Deep learning architecture | ConvNeXt-Tiny |
| Dataset | FER-2013 |
| Face detection | Google ML Kit |
| Camera | CameraX |
| Model inference | TensorFlow Lite / LiteRT |
| Database | Android Room |
| Preferences | SharedPreferences |
| Image processing | Android Bitmap APIs, ExifInterface |
| Build system | Gradle |
| Version control | Git, GitHub, Git LFS |

---

## Project Structure

The repository contains both the facial expression recognition research and the Android application.

```text
Moodlens_2026/
│
├── FER2013-Emotion-Recognition/
│   └── Notebook/
│       └── Model training and experiments
│
├── MoodLens/
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/
│   │   │   │   │   └── com/example/moodlens/
│   │   │   │   ├── res/
│   │   │   │   ├── assets/
│   │   │   │   │   ├── emotion_model.tflite
│   │   │   │   │   └── labels.txt
│   │   │   │   └── AndroidManifest.xml
│   │   │   └── test/
│   │   └── build.gradle.kts
│   │
│   ├── gradle/
│   ├── docs/
│   ├── tools/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradlew
│   └── gradlew.bat
│
├── .gitignore
├── .gitattributes
├── LICENSE
└── README.md
```

The main Android application source is located in:

`MoodLens/app/src/main/java/com/example/moodlens/`

---

## Installation and Setup

### Requirements

To run the project, you will need:

- Android Studio
- Android SDK
- Git and Git LFS
- An Android device or compatible emulator

The application is configured with a minimum Android SDK level of 26.

### 1. Clone the Repository

```bash
git lfs install
git clone https://github.com/fabihasultanaanushe72-hub/Moodlens_2026.git
cd Moodlens_2026
git lfs pull
```

Git LFS is required because the trained TensorFlow Lite model is larger than GitHub's regular Git file-size limit.

### 2. Open the Android Project

Open Android Studio and select the `MoodLens` folder inside the cloned repository.

Allow Gradle to download the necessary dependencies and complete project synchronisation.

### 3. Run the Application

Connect an Android device or launch a compatible emulator, select the `app` configuration, and click **Run**.

Camera access is required for live detection and taking photographs.

### 4. Explore the Features

Once the application starts, users can access Live Detection, Photo Analysis, Target Emotions, History, and Analytics from the main dashboard.

**Important:** Make sure `emotion_model.tflite` and `labels.txt` are available inside `app/src/main/assets/` before building the application.

---

## Team Contributions

MoodLens was developed by four students as part of the HIT401 Capstone Project at Charles Darwin University.

The work was divided across model development, Android application architecture, inference integration, camera processing, database management, and analytics.

### Fabiha Sultana Anushe
**Project Lead | AI/ML Research and Model Training | Core Android Development**

Fabiha took a leading role in the research and technical development of MoodLens, with major contributions to the machine learning model and the foundation of the Android application.

Her work included developing and evaluating the initial CNN baseline, fine-tuning ConvNeXt-Tiny, conducting model experiments, and evaluating the final trained model.

Alongside the research component, her allocated Android development responsibilities covered the application foundation, main dashboard, navigation, target-expression selection, project configuration, and integration.

**Main code files and components:**

- `MainActivity.kt` — Main dashboard and feature navigation
- `MoodLensActivity.kt` — Shared activity structure and bottom navigation
- `TargetEmotionsActivity.kt` — Target-expression selection and preferences
- `activity_main.xml` — Main dashboard interface
- `activity_target_emotions.xml` — Target selection interface
- `AndroidManifest.xml` — Application configuration and permissions
- `settings.gradle.kts` — Project configuration
- `build.gradle.kts` — Gradle build configuration
- Shared application resources and project setup
- Python model-training notebooks and experiments

**Research and model development:**

- FER-2013 dataset preparation and preprocessing
- CNN baseline model development
- ConvNeXt-Tiny fine-tuning
- Training configuration and optimisation
- Validation and performance evaluation
- Preparation of the trained model for mobile deployment

Fabiha also contributed to overall technical coordination, integration, troubleshooting, and project finalisation.

### Hoang Lan Dao
**Machine Learning Inference Integration**

Hoang's allocated contributions focused on bringing the trained recognition model into the Android environment and supporting the inference pipeline.

**Main code files and components:**

- `EmotionClassifier.kt` — Model loading, image preprocessing, inference and classification results
- `EmotionEngine.kt` — Background model execution and callbacks
- `EmotionMath.kt` — Prediction softmax and averaging
- `FaceDetectorHelper.kt` — ML Kit face detection and cropping
- `EmotionMathTest.kt` — Unit tests for classification calculations
- `labels.txt` — Model expression labels
- `emotion_model.tflite` — Integration of the trained model into Android

These components connect the trained model to the application's recognition features.

### Akshata Bhusal
**Camera and Image Processing Development**

Akshata's allocated contributions focused on the live camera and photo-analysis functionality.

**Main code files and components:**

- `LiveDetectionActivity.kt` — Live camera processing and expression detection
- `PhotoAnalysisActivity.kt` — Gallery and captured-photo analysis
- `CameraFrameUtils.kt` — Camera frame conversion and orientation handling
- `BitmapUtils.kt` — Image decoding and bitmap processing
- `FaceOverlayView.kt` — Facial bounding-box and prediction overlays
- `activity_live_detection.xml` — Live detection interface
- `activity_photo_analysis.xml` — Photo analysis interface

These components allow users to interact with the recognition model through the phone's camera and photographs.

### Jeremy Immanuel Situmeang
**Database, History and Analytics Development**

Jeremy's allocated contributions focused on local detection storage and presenting saved results through history and analytics.

**Main code files and components:**

- `DetectionEntity.java` — Detection record data model
- `DetectionDao.java` — Database operations
- `MoodLensDatabase.java` — Room database configuration
- `DetectionStore.kt` — Saving and retrieving detection results
- `HistoryActivity.kt` — Detection history and filtering
- `AnalyticsActivity.kt` — Analytics dashboard and summary statistics
- `EmotionPieChartView.kt` — Expression-distribution chart
- `WeeklyBarChartView.kt` — Daily detection trend chart
- `activity_history.xml` — History interface
- `activity_analytics.xml` — Analytics interface

These components provide local data storage, detection history and visual summaries of recorded predictions.

### Collaboration

Although different members were responsible for different parts of the project, building MoodLens required integrating the trained model with the Android inference pipeline, camera functionality, user interface, and local database.

The team worked together throughout the capstone to develop, integrate, evaluate and present the final application.

---

## Limitations and Future Work

MoodLens was developed as an academic research and software engineering project, and there are several areas where the system could be improved.

Some facial expressions share similar visual features, making them difficult to distinguish. Recognition can also be affected by lighting, camera angle, occlusion, image quality, and differences between individuals.

Our FER-2013 results provide a useful benchmark, but further testing on diverse real-world Android camera images would be needed to establish deployment performance.

Future improvements could include:

- Testing the model on more diverse facial expression datasets
- Improving prediction consistency in real-world environments
- Optimising model inference speed and resource usage
- Evaluating performance across different Android devices
- Exploring additional approaches to improve classification robustness

---

## Academic Information

**Project:** MoodLens — On-Device Facial Expression Recognition  
**Course:** HIT401 Capstone Project  
**University:** Charles Darwin University  
**Semester:** Semester 2, 2026

**Team Members:**
- Fabiha Sultana Anushe
- Hoang Lan Dao
- Akshata Bhusal
- Jeremy Immanuel Situmeang

**Academic Supervisor:** Thuseethan Selvarajah

---

## License

This repository is provided under the MIT License. See the [LICENSE](LICENSE) file for details.

Third-party datasets, pretrained models, and software dependencies remain subject to their own applicable licenses and terms.

---

## Final Note

MoodLens has been a learning experience that brought together machine learning research, mobile application development, and practical software engineering.

From training our first CNN to integrating an improved ConvNeXt-Tiny model into Android, the project helped us understand both the possibilities and the limitations of facial expression recognition.

While there is still room for improvement, MoodLens represents our effort to turn a research idea into a working mobile application.

**MoodLens — Exploring facial expression recognition through deep learning and Android development.**
