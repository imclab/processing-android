/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2011-12 Ben Fry and Casey Reas

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.mode.android;

import processing.app.*;
import processing.mode.java.JavaMode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AndroidMode extends JavaMode {
  private AndroidSDK sdk;
  private File coreZipLocation;
  private AndroidRunner runner;

  public static boolean sdkDownloadInProgress = false;

  public AndroidMode(Base base, File folder) {
    super(base, folder);
  }


  @Override
  public Editor createEditor(Base base, String path, EditorState state) {
    try {
      return new AndroidEditor(base, path, state, this);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  @Override
  public String getTitle() {
    return "Android";
  }
  
  
  public File[] getKeywordFiles() {
    return new File[] { 
      Base.getContentFile("modes/java/keywords.txt") 
    };
  }


  public File[] getExampleCategoryFolders() {
    return new File[] {
      new File(examplesFolder, "Basics"),
      new File(examplesFolder, "Topics"),
      new File(examplesFolder, "Demos"),
      new File(examplesFolder, "Sensors")
    };
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /** @return null so that it doesn't try to pass along the desktop version of core.jar */
  public Library getCoreLibrary() {
    return null;
  }


  protected File getCoreZipLocation() {
    if (coreZipLocation == null) {
      /*
      // for debugging only, check to see if this is an svn checkout
      File debugFile = new File("../../../android/core.zip");
      if (!debugFile.exists() && Base.isMacOS()) {
        // current path might be inside Processing.app, so need to go much higher
        debugFile = new File("../../../../../../../android/core.zip");
      }
      if (debugFile.exists()) {
        System.out.println("Using version of core.zip from local SVN checkout.");
//        return debugFile;
        coreZipLocation = debugFile;
      }
      */

      // otherwise do the usual
      //    return new File(base.getSketchbookFolder(), ANDROID_CORE_FILENAME);
      coreZipLocation = getContentFile("android-core.zip");
    }
    return coreZipLocation;
  }


//  public AndroidSDK loadSDK() throws BadSDKException, IOException {
//    if (sdk == null) {
//      sdk = AndroidSDK.load();
//    }
//    return sdk;
//  }

  public void loadSDK() {
    try {
      sdk = AndroidSDK.load();
    } catch (BadSDKException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void checkSDK(Editor parent) {
    if (sdk == null) {
      try {
        sdk = AndroidSDK.load();
        // FIXME REVERT THIS STATEMENT AFTER TESTING (should be ==)
        if (sdk == null) {
          sdk = AndroidSDK.locate(parent, this);
        }
      } catch (BadSDKException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (sdk == null) {
      if (!sdkDownloadInProgress) {
        Base.showWarning("It's gonna be a bad day",
            "The Android SDK could not be loaded.\n" +
                "Use of Android mode will be all but disabled.",
            null);
      }
    }
  }


  public AndroidSDK getSDK() {
    return sdk;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd.HHmm");


  static public String getDateStamp() {
    return dateFormat.format(new Date());
  }


  static public String getDateStamp(long stamp) {
    return dateFormat.format(new Date(stamp));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  public void handleRun(Sketch sketch, RunnerListener listener) throws SketchException {
//    JavaBuild build = new JavaBuild(sketch);
//    String appletClassName = build.build();
//    if (appletClassName != null) {
//      runtime = new Runner(build, listener);
//      runtime.launch(false);
//    }
//  }
  public void handleRunEmulator(Sketch sketch, RunnerListener listener) throws SketchException, IOException {
    listener.startIndeterminate();
    listener.statusNotice("Starting build...");
    AndroidBuild build = new AndroidBuild(sketch, this);

    listener.statusNotice("Building Android project...");
    build.build("debug");

    String abi = Preferences.get(AVD.PREF_KEY_ABI);
    if (abi.equals("")) {
      abi = "x86_64";
    }
    boolean avd = AVD.ensureProperAVD(sdk, abi);
    if (!avd) {
      SketchException se =
        new SketchException("Could not create a virtual device for the emulator.");
      se.hideStackTrace();
      throw se;
    }

    listener.statusNotice("Running sketch on emulator...");
    runner = new AndroidRunner(build, listener);
    runner.launch(Devices.getInstance().getEmulator());
  }


  public void handleRunDevice(Sketch sketch, RunnerListener listener) throws SketchException, IOException {
//    JavaBuild build = new JavaBuild(sketch);
//    String appletClassName = build.build();
//    if (appletClassName != null) {
//      runtime = new Runner(build, listener);
//      runtime.launch(true);
//    }

//    try {
//      runSketchOnDevice(Environment.getInstance().getHardware(), "debug", this);
//    } catch (final MonitorCanceled ok) {
//      sketchStopped();
//      statusNotice("Canceled.");
//    }
    listener.startIndeterminate();
    listener.statusNotice("Starting build...");
    AndroidBuild build = new AndroidBuild(sketch, this);

    listener.statusNotice("Building Android project...");
    build.build("debug");

    listener.statusNotice("Running sketch on device...");
    runner = new AndroidRunner(build, listener);
    runner.launch(Devices.getInstance().getHardware());
  }


  public void handleStop(RunnerListener listener) {
    listener.statusNotice("");
    listener.stopIndeterminate();

//    if (runtime != null) {
//      runtime.close();  // kills the window
//      runtime = null; // will this help?
//    }
    if (runner != null) {
      runner.close();
      runner = null;
    }
  }


//  public void handleExport(Sketch sketch, )


  /*
  protected void buildReleaseForExport(Sketch sketch, String target) throws MonitorCanceled {
//    final IndeterminateProgressMonitor monitor =
//      new IndeterminateProgressMonitor(this,
//                                       "Building and exporting...",
//                                       "Creating project...");
    try {
      AndroidBuild build = new AndroidBuild(sketch, sdk);
      File tempFolder = null;
      try {
        tempFolder = build.createProject(target, getCoreZipLocation());
        if (tempFolder == null) {
          return;
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (SketchException se) {
        se.printStackTrace();
      }
      try {
        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }
        monitor.setNote("Building release version...");
//        if (!build.antBuild("release")) {
//          return;
//        }

        if (monitor.isCanceled()) {
          throw new MonitorCanceled();
        }

        // If things built successfully, copy the contents to the export folder
        File exportFolder = build.createExportFolder();
        if (exportFolder != null) {
          Base.copyDir(tempFolder, exportFolder);
          listener.statusNotice("Done with export.");
          Base.openFolder(exportFolder);
        } else {
          listener.statusError("Could not copy files to export folder.");
        }
      } catch (IOException e) {
        listener.statusError(e);

      } finally {
        build.cleanup();
      }
    } finally {
      monitor.close();
    }
  }


  @SuppressWarnings("serial")
  private static class MonitorCanceled extends Exception {
  }
  */
}