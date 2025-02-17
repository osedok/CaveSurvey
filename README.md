CaveSurvey
==========

Cave surveying application for Android devices


The Story
=========

Preparing a cave map is a tough activity. For example if you want to map a gallery you usually pick reference points for the main polyline going trough that gallery. Between every two stations distance, angle and inclination are measured. On each station (and at any other specific gallery place) the distances to the top, bottom and both sides are measured. Usually this is written to a sheet of paper, later transferred into Excel spreadsheet and processed with a variety of existing tools.
Well, in real life this process is much harder and error prone than it sounds. CaveSurvey targets the process of collecting measurements and tries to improve it. See examples in the [User Guide](https://github.com/lz1asl/CaveSurvey/wiki/User-Guide).


Goals
=====

CaveSurvey is a tool for mapping caves using (at least) an Android device and (optionally) a laser distance meter and/or other measuring devices by:
 - keeping the measurements sheet in digital format during the survey (thus eliminating typo errors)
 - making it easy to enter measurements (because not all caves are a pleasant place to stay)
 - allowing you to export the data in Excel format for further processing (and not being a tool for the creation of a final map, such tools already exist and are awesome)
 - drawing a simple map of the main line (allowing the detection of big measurement erros on site)
 - allowing the creation of simple sketches
 - aiding measurement collection (reducing the number of measuring instruments needed and/or the need to manually type the value):
  - by using built-in sensors if available (as compass and clinometer) and tools (take pictures, save the GPS locations of the entrances, type notes, etc)
  - by using Bluetooth to integrate external laser distance meters (to read distance and in some cases compass and clinometer measurements), see the list of supported devices below


Features
========

 Currently with CaveSurvey you can:
  - make multiple cave surveys
  - split a survey into galleries
  - add stations and interim points
  - save notes, pictures, drawings, GPS coordinates and vectors at any station
  - input measurements:
   - manually
   - from the built-in sensors
   - from [Bluetooth](https://github.com/lz1asl/CaveSurvey/wiki/Measurement-Devices)
  - preview a 2D plan/section map of the current survey
  - export to Excel

  See the current [issues](https://github.com/lz1asl/CaveSurvey/issues) to see what we are working on.
  The supported languages so far are English and Bulgarian.
  

Notes
=====

One of the possible target configurations is to read the distance and inclination from Bluetooth and use the built-in compass of the device. In this case both a waterproof Android 4.x device and CEM iLDM-150 cost about $250.

We have tested from Android 2.1(cheap and small) up to 5.0 (better hardware and protection). The APK should also work on BlackBerry Q5 and probably others. Smartphones are recommended to have water protection (to survive), a compass (to read from) and a SPP Bluetooth profile (to connect instruments).
See the list of current [Measurement Devices](https://github.com/lz1asl/CaveSurvey/wiki/Measurement-Devices)

Feel free to join our team and help adding other devices/functionalities
  
In most caves it is not possible to create the whole map underground (as some other apps offer). I'm not confident in drawing stuff with muddy fingers, so get the data you can quick and get safe home to enjoy it.

Precision
=========

Having precise instruments is important to do a proper work. Anyway in most caves centimeter precision is never possible.
For the Android build-in sensors - you have to consult your device specifications and test within CaveSurvey, but if good enough and you use short legs there should be no problem (error will distribute and compensate anyway).
If you are paranoic you can still use CaveSurvey in manual mode only - type manually the proper values from the existing tools you use. It will save you the Excel homework later.


About
=====

Developed by members of caving club [Paldin](http://sk-paldin.eu/) Plovdiv, Bulgaria.

![Picture](res/drawable-mdpi/paldin.jpg)

You can use the official version at [Google Play](https://play.google.com/store/apps/details?id=com.astoev.cave.survey) or the [latest beta build](https://razhodki.ci.cloudbees.com/job/CaveSurvey/lastSuccessfulBuild/artifact/build/outputs/apk/CaveSurvey-defaultFlavor-release.apk) for devces without Google Play, [history](https://github.com/lz1asl/CaveSurvey/wiki/Releases).

![CloudBees](http://www.cloudbees.com/sites/default/files/Button-Built-on-CB-1.png)


This software is free to use and modify. We provide no guarantee in any kind but are open for ideas and collaborations. Contact us at cave.survey.project@gmail.com.


Related Projects
================

[CaveSurveyBTEmulator](https://github.com/lz1asl/CaveSurveyBTEmulator) is utility project that helps us to emulate the communication with Bluetooth measurement devices that we don't currently have while developing.
