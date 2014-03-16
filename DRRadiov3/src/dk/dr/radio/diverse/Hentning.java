package dk.dr.radio.diverse;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Favoritter;
import dk.dr.radio.data.Udsendelse;

/**
 * Created by j on 01-03-14.
 */
@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class Hentning {
  private DownloadManager downloadService = null;

  private static final String PREF_NØGLE = "slugFraDownloadId";
  private HashMap<String, String> downloadIdFraSlug;
  private HashMap<String, String> slugFraDownloadId;
  public List<Runnable> observatører = new ArrayList<Runnable>();

  public boolean virker() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
  }

  public Hentning(App app) {
    if (virker()) {
      downloadService = (DownloadManager) App.instans.getSystemService(Context.DOWNLOAD_SERVICE);
    }
  }



  private void tjekDataOprettet() {
    if (slugFraDownloadId != null) return;
    String str = App.prefs.getString(PREF_NØGLE, "");
    Log.d("Hentning: læst " + str);
    slugFraDownloadId = Favoritter.strengTilMap(str);
    downloadIdFraSlug = new HashMap<String, String>();
    for (Map.Entry<String,String> en : slugFraDownloadId.entrySet()) {
      downloadIdFraSlug.put(en.getValue(), en.getKey());
    }
  }


  private void gem() {
    String str = Favoritter.mapTilStreng(slugFraDownloadId);
    Log.d("Hentning: gemmer " + str);
    App.prefs.edit().putString(PREF_NØGLE, str).commit();
  }



  public void hent(Udsendelse udsendelse) {
    tjekDataOprettet();
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

      int downloadId = (int) App.hentning.downloadService.enqueue(req);
      downloadIdFraSlug.put(udsendelse.slug, ""+downloadId);
      slugFraDownloadId.put(""+downloadId, udsendelse.slug);
      gem();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }

  /**
   * Giver status
   * @param udsendelse
   * @return
   */
  public Cursor getStatus(Udsendelse udsendelse) {
    if (!virker()) return null;
    tjekDataOprettet();
    Log.d("getStatus downloadIdFraSlug = "+downloadIdFraSlug+"  u="+udsendelse);
    String downloadId = downloadIdFraSlug.get(udsendelse.slug);
    if (downloadId==null) return null;
    DownloadManager.Query query = new DownloadManager.Query();
    query.setFilterById(Integer.parseInt(downloadId));
    Cursor c = App.hentning.downloadService.query(query);
    if (c.moveToFirst()) {
      return c;
    }
    c.close();
    return null;
  }


  public Set<String> getUdsendelseSlugSæt() {
    tjekDataOprettet();
    return downloadIdFraSlug.keySet();
  }


  public static class DownloadServiceReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d("DLS " + intent);
      Bundle b = intent.getExtras();
      b.size(); // forårsager at bundlen bliver unparcelet
      Log.d("DLS " + b);
      if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) try {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        String slug = App.hentning.slugFraDownloadId.get("" + downloadId);
        Udsendelse u = DRData.instans.udsendelseFraSlug.get(slug);
        if (slug==null) throw new IllegalStateException("Ingen udsendelse for hentning");

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = App.hentning.downloadService.query(query);
        if (c.moveToFirst()) {
          Log.d("DLS " + c + "  "+c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
          if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            App.langToast("Udsendelsen "+u.titel+" blev hentet");
          } else {
            App.langToast("Det lykkedes ikke at hente udsendelsen "+u.titel+"\nTjek at du har tilstrækkeligt ledigt plads");
          }
        }
        c.close();
        for (Runnable obs : App.hentning.observatører) obs.run();
      } catch (Exception e) { Log.rapporterFejl(e);
      } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
        // Åbn download manager
        Intent dm = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        dm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(dm);
      }
    }
  }


  public void status() {
    if (!virker()) return;
//    Cursor c= downloadService.query(new DownloadManager.Query().setFilterById(lastDownload));
    Cursor c= downloadService.query(new DownloadManager.Query());

    while (c.moveToNext())
    {
      Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
      Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
      Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
      Log.d(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
      Log.d(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
      Log.d(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
    }
    c.close();
  }
}
