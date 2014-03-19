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
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Udsendelse;

/**
 * Created by j on 01-03-14.
 */
@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class Hentning {
  private DownloadManager downloadService = null;

  public static class HentningData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Long> downloadIdFraSlug = new LinkedHashMap<String, Long>();
    private Map<Long, Udsendelse> udsendelseFraDownloadId = new LinkedHashMap<Long, Udsendelse>();
  }
  private HentningData data;
  public List<Runnable> observatører = new ArrayList<Runnable>();

  public boolean virker() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
  }

  public Hentning(App app) {
    if (virker()) {
      downloadService = (DownloadManager) App.instans.getSystemService(Context.DOWNLOAD_SERVICE);
    }
  }

  private String FILNAVN = App.instans.getFilesDir()+"/Hentning.ser";

  private void tjekDataOprettet() {
    if (data!=null) return;
    if (new File(FILNAVN).exists()) try {
      data = (HentningData) Serialisering.hent(FILNAVN);
      return;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    data = new HentningData();
  }

  private Runnable gemListe = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(gemListe);
      try {
        long tid = System.currentTimeMillis();
        Serialisering.gem(data, FILNAVN);
        Log.d("Hentning: Gemning tog "+(System.currentTimeMillis()-tid)+" ms - filstr:" + new File(FILNAVN).length());
      } catch (IOException e) {
        Log.rapporterFejl(e);
      }
    }
  };


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

      long downloadId = App.hentning.downloadService.enqueue(req);
      data.downloadIdFraSlug.put(udsendelse.slug, downloadId);
      data.udsendelseFraDownloadId.put(downloadId, udsendelse);
      gemListe.run();
      for (Runnable obs : App.hentning.observatører) obs.run();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }


  public Collection<Udsendelse> getUdsendelser() {
    if (!virker()) return new ArrayList<Udsendelse>();
    tjekDataOprettet();
    return data.udsendelseFraDownloadId.values();
  }

  /**
   * Giver status
   * @param udsendelse
   * @return
   */
  public Cursor getStatus(Udsendelse udsendelse) {
    if (!virker()) return null;
    tjekDataOprettet();
    Log.d("getStatus downloadIdFraSlug = "+data.downloadIdFraSlug+"  u="+udsendelse);
    Long downloadId = data.downloadIdFraSlug.get(udsendelse.slug);
    if (downloadId==null) return null;
    DownloadManager.Query query = new DownloadManager.Query();
    query.setFilterById(downloadId);
    Cursor c = App.hentning.downloadService.query(query);
    if (c.moveToFirst()) {
      return c;
    }
    c.close();
    return null;
  }

  public void annullér(Udsendelse u) {
    tjekDataOprettet();
    Long id = data.downloadIdFraSlug.remove(u.slug);
    data.udsendelseFraDownloadId.remove(id);
    Log.d("Hentning: data.udsendelseFraDownloadId= "+data.udsendelseFraDownloadId);
    Log.d("Hentning: data.downloadIdFraSlug="+data.downloadIdFraSlug);
    downloadService.remove(id);
    gemListe.run();
    for (Runnable obs : App.hentning.observatører) obs.run();
  }



  public static class DownloadServiceReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d("DLS " + intent);
      if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) try {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        Udsendelse u = App.hentning.data.udsendelseFraDownloadId.get(downloadId);
        if (u==null) throw new IllegalStateException("Ingen udsendelse for hentning for "+downloadId);

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
        App.hentning.gemListe.run();
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