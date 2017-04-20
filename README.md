# MOBILE APPLICATION ON SCENE RECONSTRUCTION AND VISUALIZATION FROM COMMUNITY PHOTO COLLECTION

### Web & Mobile Application Development
### MSc in Global Software Development
### Winter Semester 2016, Fulda

---
## Chosen Paper: Scene Reconstruction And Visualization From Community Photo Collection
### By Noah Snavely, Ian Simon, Michael Goesele, Richard Szeliski, and Steven M. Seitz

## Introduction

The paper is about accumulating huge number of photographs for a structure or object found in internet and develop a 3D reconstruction of the key object. There are number of steps described such as:
- Finding features and correspondence
- Dense Scene Reconstruction
- Scene Summarization and Segmentation
- Finding Paths From Series of Photos
- Scene Visualization

The project is not necessarily meant for implementing in small or medium handheld devices, however, we took an attempt in doing so. We tried to cover the steps where small computing devices can be used efficiently such as feature detection and finding correspondence, capture images with features and panoramical views. 

## Implementation
### Used Tools, Technologies and Libraries
- Android Studio Version: 2.3
- Android SDK Version: 23, 25
- Android NDK (Native Development Kit)
- OpenCV4Android Version: 3.2
- Gradle Build Tool

### Architecture
The application is developed primarily for Android devices with ARMEABI v7a architecture. However, additional architectures such as ARM64, ARMEABI, mips, x86, x86_64 can be supported easily.

### Features
The purpose of this app is to take photos of key object and detect features. The camera view displays features by red circles found in the camera stream live. The app can switch between both front and rear camera and also change resolution of output image. It can be used to capture photos of key object from different angle and later use the images for further steps such as Dense Scene Reconstruction.

### Structure
Although the app is written using Java, part of it also incorporates C++ codes which calls the OpenCV4android library for feature detection via Java Native Invocation (JNI) calls. The C++ codes are compiled at build time for each architecture.

There are few algorithms for feature detection. SIFT, SURF, FAST and ORB are few among many. In our case, the algorithm used for feature detection is ‘FAST’ - Features from Accelerated Segment Test.


