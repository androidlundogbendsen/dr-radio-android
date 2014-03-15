package dk.dr.radio.diverse;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.File;

import dk.dr.radio.data.Udsendelse;

/**
 * Created by j on 01-03-14.
 */
public class Hentning {
  public DownloadManager downloadService = (DownloadManager) App.instans.getSystemService(Context.DOWNLOAD_SERVICE);

  @SuppressLint("NewApi")
  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
  public void hent(Udsendelse udsendelse) {
    try {
      Uri uri = Uri.parse(udsendelse.findBedsteStream(true).url);
      Log.d("uri=" + uri);

      DownloadManager.Request req = new DownloadManager.Request(uri)
          .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
          .setAllowedOverRoaming(false)
          .setTitle(udsendelse.titel)
          .setDescription(udsendelse.beskrivelse);
      File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
      dir.mkdirs();
      if (dir.exists()) {
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, udsendelse.slug + ".mp3");
      }

      if (Build.VERSION.SDK_INT >= 11) req.allowScanningByMediaScanner();

      long downloadId = App.hentning.downloadService.enqueue(req);
      App.langToast("downloadId=" + downloadId + "\n" + dir);
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

  }

  public static class DownloadServiceReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      App.langToast("DLS " + intent);
      if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
        App.langToast("Færdig");
      } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
        // Åbn download manager
        Intent dm = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        dm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(dm);
      }
    }
  }

}
