# Default optimized rules come from proguard-android-optimize.txt.
# Room, Coil, WorkManager, Firebase and Google Mobile Ads ship their own consumer rules.
-keepattributes *Annotation*
-keep class com.infinity.wallpapers.wallpaper.DownloadWorker { *; }
