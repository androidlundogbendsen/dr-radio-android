package dk.dr.radio.diverse;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by j on 01-03-14.
 */
public class Hentning {
  public DownloadManager downloadService;

  public static class DownloadServiceBR extends BroadcastReceiver {

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
