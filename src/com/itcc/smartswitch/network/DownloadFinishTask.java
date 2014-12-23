
package com.itcc.smartswitch.network;


import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.itcc.smartswitch.UpdateManager;
import com.itcc.smartswitch.utils.Constant;
//import android.content.pm.PackageParser;
//import android.content.pm.PackageParser;


public class DownloadFinishTask implements Runnable {

    private int result, type;
    private String path;
    private Context context;

    public DownloadFinishTask(Context context, int result, int type, String path) {
        this.result = result;
        this.type = type;
        this.path = path;
        this.context = context;
    }

    @Override
    public void run() {
        if (type == Constant.NOTIFY_TYPE_NEW_VERSION_UPDATE_APK) {
            if (result == Constant.RESULT_SUCCESS) {
                UpdateManager.getInastance(context).mUpdateing = false;
                installApp(context, path);
            }
        }
    }
    public static void installApp(Context context, String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(path)),
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    public synchronized static void installAppInSilent(Context context, String path) {
/*//  	Toast.makeText(context, R.string.toast_update_start, Toast.LENGTH_SHORT).show();
  	PackageManager pm = context.getPackageManager();
  	int installFlags = 0;
  	final File sourceFile = new File(path);
  	PackageParser.Package mPkgInfo = getPackageInfo(sourceFile);
  	ApplicationInfo mAppInfo = mPkgInfo.applicationInfo;
//  	PackageInstallObserver observer = new PackageInstallObserver();
  	try {
  		PackageInfo pi = pm.getPackageInfo(mAppInfo.packageName, 
  				PackageManager.GET_UNINSTALLED_PACKAGES);
  		if(pi != null) {
  			installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
  		}
  	} catch (NameNotFoundException e) {
  	}
  	pm.installPackage(Uri.fromFile(new File(path)), null, installFlags, context.getPackageName());*/
  }
/*    public static PackageParser.Package getPackageInfo(File sourceFile) {
        final String archiveFilePath = sourceFile.getAbsolutePath();
        PackageParser packageParser = new PackageParser(archiveFilePath);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        PackageParser.Package pkg =  packageParser.parsePackage(sourceFile,
                archiveFilePath, metrics, 0);
        // Nuke the parser reference.
        packageParser = null;
        return pkg;
    }*/
}
