package dk.dr.radio.data;

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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import dk.dr.radio.akt.Hentede_udsendelser_frag;
import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.akt.VisFragment_akt;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Serialisering;
import dk.dr.radio.v3.R;

/**
 * Created by j on 01-03-14.
 */
@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class HentedeUdsendelser {
  public static final String NØGLE_placeringAfHentedeFiler = "placeringAfHentedeFiler";
  private DownloadManager downloadService = null;

  public static class Data implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Long> downloadIdFraSlug = new LinkedHashMap<String, Long>();
    private Map<Long, Udsendelse> udsendelseFraDownloadId = new LinkedHashMap<Long, Udsendelse>();
  }

  private Data data;
  public List<Runnable> observatører = new ArrayList<Runnable>();

  /** Understøttes ikke på Android 2.2 og tidligere */
  public boolean virker() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
  }

  private final String FILNAVN;

  public HentedeUdsendelser() {
    if (virker() && App.instans != null) { // App.instans==null standard JVM (udenfor Android)
      downloadService = (DownloadManager) App.instans.getSystemService(Context.DOWNLOAD_SERVICE);
      FILNAVN = App.instans.getFilesDir() + "/HentedeUdsendelser.ser";
    } else {
      FILNAVN = "/tmp/HentedeUdsendelser.ser";
    }
  }


  private void tjekDataOprettet() {
    if (data != null) return;
    if (new File(FILNAVN).exists()) try {
      data = (Data) Serialisering.hent(FILNAVN);
      return;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    data = new Data();
  }

  private Runnable gemListe = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(gemListe);
      try {
        long tid = System.currentTimeMillis();
        Serialisering.gem(data, FILNAVN);
        Log.d("Hentning: Gemning tog " + (System.currentTimeMillis() - tid) + " ms - filstr:" + new File(FILNAVN).length());
      } catch (IOException e) {
        Log.rapporterFejl(e);
      }
    }
  };


  public void hent(Udsendelse udsendelse) {
    tjekDataOprettet();
    try {
      String url = udsendelse.findBedsteStreamUrl(true);
      if (url == null) {
        Log.rapporterFejl(new IllegalStateException("ingen streamurl"), udsendelse.slug);
        App.langToast("Beklager, udsendelsen kunne ikke hentes");
        return;
      }
      Uri uri = Uri.parse(url);
      Log.d("uri=" + uri);

      String brugervalg = App.prefs.getString(NØGLE_placeringAfHentedeFiler, null);
      File dir;
      if (brugervalg!=null && new File(brugervalg).exists()) dir = new File(brugervalg);
      else dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
      dir = new File(dir, "DR_Radio");
      dir.mkdirs();
      if (!dir.exists()) throw new IOException("kunne ikke oprette "+dir);

      DownloadManager.Request req = new DownloadManager.Request(uri)
          .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
          .setAllowedOverRoaming(false)
          .setTitle(udsendelse.titel)
          .setDescription(udsendelse.beskrivelse);
      //req.setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, udsendelse.slug + ".mp3");
      //req.setDestinationInExternalPublicDir("DR_Radio", udsendelse.slug + ".mp3");
      //req.setDestinationInExternalFilesDir(App.instans, Environment.DIRECTORY_PODCASTS, "DRRADIO4xx"+ udsendelse.slug + ".mp3");
      req.setDestinationUri(Uri.fromFile(new File(dir, udsendelse.slug + ".mp3")));

      if (Build.VERSION.SDK_INT >= 11) req.allowScanningByMediaScanner();

      long downloadId = downloadService.enqueue(req);
      data.downloadIdFraSlug.put(udsendelse.slug, downloadId);
      data.udsendelseFraDownloadId.put(downloadId, udsendelse);
      gemListe.run();
      for (Runnable obs : new ArrayList<Runnable>(observatører)) obs.run();
    } catch (Exception e) {
      Log.rapporterFejl(e);
      App.langToast("Kunne ikke få adgang til eksternt lager.\nSe eventuelt indstillingen til placering af hentede udsendelser");
    }
  }

  /**
   * Finder stien til et eksternt SD-kort - altså ikke til den 'external storage' der fra Android 4.2
   * oftest er intern.
   * Se også http://source.android.com/devices/tech/storage/,
   * http://stackoverflow.com/questions/13646669/android-securityexception-destination-must-be-on-external-storage og
   * http://www.androidpolice.com/2014/02/17/external-blues-google-has-brought-big-changes-to-sd-cards-in-kitkat-and-even-samsung-may-be-implementing-them/
   * @return en liste af stier, hvor en af dem muligvis er til et eksternt SD-kort
   */
  public static ArrayList<File> findMuligeEksternLagerstier() {

    // Hjælpemetode til at tjekke
    class Res {
      LinkedHashMap<File, File> res = new LinkedHashMap<File, File>();

      public void put(File dir) {
        File nøgle = dir;
        try {
          nøgle = nøgle.getCanonicalFile();
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (!res.containsKey(nøgle)) {
          // Se om der er en mappe, eller vi kan lave en
          boolean fandtesFørMkdirs = dir.exists();
          dir.mkdirs();
          if (dir.isDirectory()) res.put(nøgle, dir);
          if (!fandtesFørMkdirs) dir.delete(); // ryd op
        }
      }
    }

    Res res = new Res();
    res.put(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS));

    File fstab = new File("/etc/vold.fstab"); // læs i vold.fstab hvor der t.o.m Android 4.2 er nævnt det rigtige SD-kort
    if (fstab.canRead()) {
      try {
        Scanner scanner = new Scanner(fstab);

        while (scanner.hasNext()) {
          String s = scanner.nextLine().trim();
          if (s.startsWith("dev_mount")) {
            // dev_mount sdcard /mnt/sdcard auto /devices/platform/goldfish_mmc.0 /devices/platform/msm_sdcc.2/mmc_host/mmc1
            String sti = s.split("\\s")[2]; // /mnt/sdcard
            Log.d("findStiTilRigtigtSDKort - fandt " + sti);
            res.put(new File(sti, Environment.DIRECTORY_PODCASTS));
          }
        }
        scanner.close();
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    }

    Log.d("findMuligeEksternLagerstier: "+res.res);
    ArrayList<File> liste = new ArrayList<File>(res.res.values());
    return liste;
  }

  public Collection<Udsendelse> getUdsendelser() {
    if (!virker()) return new ArrayList<Udsendelse>();
    tjekDataOprettet();
    return data.udsendelseFraDownloadId.values();
  }

  /**
   * Giver status
   *
   * @param udsendelse
   * @return
   */
  public Cursor getStatusCursor(Udsendelse udsendelse) {
    if (!virker()) return null;
    tjekDataOprettet();
    Long downloadId = data.downloadIdFraSlug.get(udsendelse.slug);
    if (downloadId == null) return null;
    Log.d("HentedeUdsendelser getStatus gav downloadId = " + downloadId + " for u=" + udsendelse);
    DownloadManager.Query query = new DownloadManager.Query();
    query.setFilterById(downloadId);
    Cursor c = downloadService.query(query);
    if (c.moveToFirst()) {
      return c;
    }
    c.close();
    return null;
  }

  public static int getStatus(Cursor c) {
    return c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
  }

  public static String getStatustekst(Cursor c) {
    int status = getStatus(c);
    long iAlt = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)) / 1000000;
    long hentet = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) / 1000000;
    String txt;
    if (status == DownloadManager.STATUS_SUCCESSFUL) {
      txt = "Klar";
    } else if (status == DownloadManager.STATUS_FAILED) {
      txt = "Mislykkedes";
    } else if (status == DownloadManager.STATUS_PENDING) {
      txt = "Venter...";
    } else if (status == DownloadManager.STATUS_PAUSED) {
      txt = "Hentning pauset ... hentet " + hentet + " MB af " + iAlt + " MB";
    } else { // RUNNING
      if (hentet>0 || iAlt>0) txt = "Hentet " + hentet + " MB af " + iAlt + " MB";
      else txt = "Henter...";
    }
    return txt;
  }

  public void annullér(Udsendelse u) {
    tjekDataOprettet();
    Long id = data.downloadIdFraSlug.remove(u.slug);
    data.udsendelseFraDownloadId.remove(id);
    Log.d("Hentning: data.udsendelseFraDownloadId= " + data.udsendelseFraDownloadId);
    Log.d("Hentning: data.downloadIdFraSlug=" + data.downloadIdFraSlug);
    downloadService.remove(id);
    gemListe.run();
    for (Runnable obs : new ArrayList<Runnable>(observatører)) obs.run();
  }


  public static class DownloadServiceReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d("DLS " + intent);
      if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) try {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        Udsendelse u = DRData.instans.hentedeUdsendelser.data.udsendelseFraDownloadId.get(downloadId);
        if (u == null) throw new IllegalStateException("Ingen udsendelse for hentning for " + downloadId);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = DRData.instans.hentedeUdsendelser.downloadService.query(query);
        if (c.moveToFirst()) {
          Log.d("DLS " + c + "  " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
          if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            App.langToast("Udsendelsen " + u.titel + " blev hentet");
            Log.registrérTestet("Hente udsendelse", u.slug);
          } else {
            App.langToast("Det lykkedes ikke at hente udsendelsen " + u.titel + "\nTjek at du har tilstrækkeligt ledigt plads");
          }
        }
        c.close();
        DRData.instans.hentedeUdsendelser.gemListe.run();
        for (Runnable obs : new ArrayList<Runnable>(DRData.instans.hentedeUdsendelser.observatører)) obs.run();
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
      else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
        // Åbn app'en, under hentninger

        if (App.aktivitetIForgrunden != null) {
          // Skift til Hentede_frag
          try {
            FragmentManager fm = App.aktivitetIForgrunden.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.indhold_frag, new Hentede_udsendelser_frag());
            ft.addToBackStack("Hentning");
            ft.commit();
          } catch (Exception e1) {
            Log.rapporterFejl(e1);
          }
        } else {
          // Åbn hovedaktivitet
          Intent i = new Intent(context, Hovedaktivitet.class)
              .putExtra(VisFragment_akt.KLASSE, Hentede_udsendelser_frag.class.getName());
          i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(i);
        }

/*
        Intent dm = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        dm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(dm);
        */
      }
    }
  }


  public void status() {
    if (!virker()) return;
//    Cursor c= downloadService.query(new DownloadManager.Query().setFilterById(lastDownload));
    Cursor c = downloadService.query(new DownloadManager.Query());

    while (c.moveToNext()) {
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
