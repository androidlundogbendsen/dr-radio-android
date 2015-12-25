package dk.dr.radio.data;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import dk.dr.radio.akt.Hentede_udsendelser_frag;
import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Serialisering;
import dk.dr.radio.diverse.Sidevisning;
import dk.dr.radio.v3.R;

/**
 * Created by j on 01-03-14.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class HentedeUdsendelser {
  public static final String NØGLE_placeringAfHentedeFiler = "placeringAfHentedeFiler";
  private DownloadManager downloadService = null;
  private ArrayList<Udsendelse> udsendelser;

  public static class Data implements Serializable {
    // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/1415558087
    // - at proguard obfuskering havde
    // Se også http://stackoverflow.com/questions/16210831/serialization-deserialization-proguard
    private static final long serialVersionUID = -3292059648694915445L;

    private Map<String, Long> downloadIdFraSlug = new LinkedHashMap<String, Long>();
    private Map<Long, Udsendelse> udsendelseFraDownloadId = new LinkedHashMap<Long, Udsendelse>();
    private ArrayList<Udsendelse> udsendelser = new ArrayList<Udsendelse>();
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
      if (data.udsendelser == null) { // Feltet data.udsendelser kom med 2. okt 2014 - tjek kan slettes efter sommer 2015
        data.udsendelser = new ArrayList<Udsendelse>(data.udsendelseFraDownloadId.values());
      }
      // Sæt korrekt hentetStream på alle hentede udsendelser
      for (Udsendelse serialiseretUds : data.udsendelser) {
        Udsendelse u = DRData.instans.udsendelseFraSlug.get(serialiseretUds.slug);
        if (u==null) {
          // Serialiserede udsendelser skal med i slug-listen
          DRData.instans.udsendelseFraSlug.put(serialiseretUds.slug, serialiseretUds);
          tjekOmHentet(serialiseretUds);
        } else {
          tjekOmHentet(u);
        }
      }
      return;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    data = new Data();
    gemListe(); // For at undgå at fejl rapporteres mere end 1 gang
  }

  private void gemListe() {
    try {
      long tid = System.currentTimeMillis();
      Serialisering.gem(data, FILNAVN);
      Log.d("Hentning: Gemning tog " + (System.currentTimeMillis() - tid) + " ms - filstr:" + new File(FILNAVN).length());
    } catch (IOException e) {
      Log.rapporterFejl(e);
    }
  }


  public void hent(Udsendelse udsendelse) {
    tjekDataOprettet();
    try {
      List<Lydstream> prioriteretListe = udsendelse.findBedsteStreams(true);
      if (prioriteretListe == null || prioriteretListe.size() < 1) {
        Log.rapporterFejl(new IllegalStateException("ingen streamurl"), udsendelse.slug);
        App.langToast(R.string.Beklager_udsendelsen_kunne_ikke_hentes);
        return;
      }
      Uri uri = Uri.parse(prioriteretListe.get(0).url);

      String brugervalg = App.prefs.getString(NØGLE_placeringAfHentedeFiler, null);
      File dir;
      if (brugervalg != null && new File(brugervalg).exists()) dir = new File(brugervalg);
      else dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
      Log.d("Hent uri=" + uri +" til "+dir);
      dir = new File(dir, App.instans.getString(R.string.HENTEDE_UDS_MAPPENAVN));
      dir.mkdirs();
      if (!dir.exists()) throw new IOException("kunne ikke oprette " + dir);
      udsendelse.hentetStreamDestination = new File(dir, udsendelse.slug.replace(':','_') + ".mp3");
/* NYT
      String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
      if (!dir.getPath().startsWith(externalPath)) {
        dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
        Log.d("DownloadManager kan ikke hente til dette sted - gem midlertidigt på "+dir);
      }
*/
      int typer = App.prefs.getBoolean("hentKunOverWifi", false) ?
          DownloadManager.Request.NETWORK_WIFI :
          DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE;

      DownloadManager.Request req = new DownloadManager.Request(uri)
          .setAllowedNetworkTypes(typer)
          .setAllowedOverRoaming(false)
          .setTitle(udsendelse.titel)
          .setDescription(udsendelse.beskrivelse);

      req.setDestinationUri(Uri.fromFile(new File(dir, udsendelse.hentetStreamDestination.getName())));

      if (Build.VERSION.SDK_INT >= 11) req.allowScanningByMediaScanner();

      long downloadId = downloadService.enqueue(req);
      data.downloadIdFraSlug.put(udsendelse.slug, downloadId);
      data.udsendelseFraDownloadId.put(downloadId, udsendelse);
      if (!data.udsendelser.contains(udsendelse)) data.udsendelser.add(udsendelse);
      Log.d("Hentning: hent() data.udsendelseFraDownloadId= " + data.udsendelseFraDownloadId);
      Log.d("Hentning: hent() data.downloadIdFraSlug=" + data.downloadIdFraSlug);
      gemListe();
      for (Runnable obs : new ArrayList<Runnable>(observatører)) obs.run();
    } catch (Exception e) {
      Log.rapporterFejl(e);
      App.langToast(R.string.Kunne_ikke_få_adgang_til_eksternt_lager__se_evt__);
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
/* NYT
    if (Build.VERSION.SDK_INT>=19) try {
      for (File f : App.instans.getExternalFilesDirs(Environment.DIRECTORY_PODCASTS)) {
        res.put(f);
      }
    } catch (Exception e) { Log.rapporterFejl(e); }
    else {
      res.put(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS));
    }
*/
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

    Log.d("findMuligeEksternLagerstier: " + res.res);
    ArrayList<File> liste = new ArrayList<File>(res.res.values());
    return liste;
  }

  public Collection<Udsendelse> getUdsendelser() {
    tjekDataOprettet();
    return data.udsendelser;
  }

  public HentetStatus getHentetStatus(Udsendelse udsendelse) {
    if (!virker()) return null;
    tjekDataOprettet();
    Long downloadId = data.downloadIdFraSlug.get(udsendelse.slug);
    if (downloadId == null) return null;
    DownloadManager.Query query = new DownloadManager.Query();
    query.setFilterById(downloadId);
    Cursor c = downloadService.query(query);
    if (c==null) return null; // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/4066198043
    if (!c.moveToFirst()) {
      c.close();
      return null;
    }

    HentetStatus hs = new HentetStatus();
    hs.status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
    //if (hs.status==DownloadManager.STATUS_FAILED || hs.status==DownloadManager.STATUS_PAUSED) hs.grund = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
    hs.iAlt = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)) / 1000000;
    hs.hentet = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) / 1000000;
    hs.uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
    c.close();
    if (hs.status == DownloadManager.STATUS_SUCCESSFUL) {
      hs.statustekst = App.instans.getString(R.string.Klar___mb_, hs.iAlt);
    } else if (hs.status == DownloadManager.STATUS_FAILED) {
      hs.statustekst = App.instans.getString(R.string.Mislykkedes);
    } else if (hs.status == DownloadManager.STATUS_PENDING) {
      hs.statustekst = App.instans.getString(R.string.Venter___);
    } else if (hs.status == DownloadManager.STATUS_PAUSED) {
      hs.statustekst = App.instans.getString(R.string.Hentning_pauset__)+App.instans.getString(R.string.Hentet___mb_af___mb, hs.hentet, hs.iAlt);
    } else { // RUNNING
      if (hs.hentet > 0 || hs.iAlt > 0) hs.statustekst = App.instans.getString(R.string.Hentet___mb_af___mb, hs.hentet, hs.iAlt);
      else hs.statustekst = App.instans.getString(R.string.Henter__);
    }
    return hs;
  }

  /** Sletter udsendelsen fuldstændigt fra listen */
  public void slet(Udsendelse u) {
    data.udsendelser.remove(u);
    stop(u);
  }

  /** Sletter udsendelsen, men viser den stadig på listen, hvis brugern vil hente den igen senere */
  public void stop(Udsendelse u) {
    tjekDataOprettet();
    sletLokalFil(u);
    Long id = data.downloadIdFraSlug.remove(u.slug);
    if (id == null) {
      Log.d("stop() udsendelse " + u + " ikke i data.downloadIdFraSlug - den er nok allerede stoppet");
    } else {
      data.udsendelseFraDownloadId.remove(id);
      downloadService.remove(id);
    }
    gemListe();
    for (Runnable obs : new ArrayList<Runnable>(observatører)) obs.run();
  }

  private void sletLokalFil(Udsendelse u) {
    HentetStatus hs = getHentetStatus(u);
    if (hs.uri != null) {
      new File(URI.create(hs.uri).getPath()).delete();
    }
  }


  public static class DownloadServiceReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d("HentedeUdsendelser DLS " + intent);
      if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) try {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        DRData.instans.hentedeUdsendelser.tjekDataOprettet(); // Fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/803968027
        Udsendelse u = DRData.instans.hentedeUdsendelser.data.udsendelseFraDownloadId.get(downloadId);
        if (u == null) {
          Log.d("Ingen udsendelse for hentning for " + downloadId + " den er nok blevet slettet");
          return;
        }

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = DRData.instans.hentedeUdsendelser.downloadService.query(query);
        if (c.moveToFirst()) {
          Log.d("HentedeUdsendelser DLS " + c + "  " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
          if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            App.langToast(App.instans.getString(R.string.Udsendelsen___blev_hentet, u.titel));
            Log.registrérTestet("Hente udsendelse", u.slug);
            /* NYT
            String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            File hentet =  new File(URI.create(uri));
            if (!hentet.equals(u.hentetStreamDestination)) {
              Log.d("HentedeUdsendelser flytter fil fra " + hentet + " til " + u.hentetStreamDestination);
              FilCache.kopierOgLuk(new FileInputStream(hentet), new FileOutputStream(u.hentetStreamDestination));
              hentet.delete();
            }
            */
          } else {
            App.langToast(App.instans.getString(R.string.Det_lykkedes_ikke_at_hente_udsendelsen___tjek_at___, u.titel));
          }
        }
        c.close();
        DRData.instans.hentedeUdsendelser.gemListe();
        for (Runnable obs : new ArrayList<Runnable>(DRData.instans.hentedeUdsendelser.observatører)) obs.run();
        Sidevisning.vist(HentedeUdsendelser.class, u.slug);
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
      else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
        // Åbn app'en, under hentninger

        if (App.aktivitetIForgrunden instanceof FragmentActivity) {
          // Skift til Hentede_frag
          try {
            FragmentManager fm = ((FragmentActivity) App.aktivitetIForgrunden).getSupportFragmentManager();
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
              .putExtra(Hovedaktivitet.VIS_FRAGMENT_KLASSE, Hentede_udsendelser_frag.class.getName());
          i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(i);
        }
        Sidevisning.vist(HentedeUdsendelser.class);

/*
        Intent dm = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        dm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(dm);
        */
      }
    }
  }


  public void tjekOmHentet(Udsendelse udsendelse) {
    if (!virker()) return;
    /* NYT
    if (udsendelse.hentetStream == null && udsendelse.hentetStreamDestination != null && udsendelse.hentetStreamDestination.exists()) {
      File file = udsendelse.hentetStreamDestination;
      if (file.exists()) {
        udsendelse.hentetStream = new Lydstream();
        udsendelse.hentetStream.url = file.getPath();
        udsendelse.hentetStream.score = 500; // Rigtig god!
        udsendelse.kanHøres = true;
        Log.registrérTestet("Afspille hentet udsendelse", udsendelse.slug);
      } else {
        Log.rapporterFejl(new IllegalStateException("FilXXX " + file + " hentet, men fandtes ikke alligevel??!"));
      }
    }
    */
    if (udsendelse.hentetStream == null) {
      HentetStatus hs = getHentetStatus(udsendelse);
      if (hs == null) return;
      if (hs.status != DownloadManager.STATUS_SUCCESSFUL) return;
      File file = new File(URI.create(hs.uri).getPath());
        if (file.exists()) {
          udsendelse.hentetStream = new Lydstream();
          udsendelse.hentetStream.url = hs.uri;
          udsendelse.hentetStream.score = 500; // Rigtig god!
          udsendelse.kanHøres = true;
          Log.registrérTestet("Afspille hentet udsendelse", udsendelse.slug);
          Log.rapporterFejl(new IllegalStateException("Nylig opgradering? Denne blok fjernes i 2016... "));
          udsendelse.hentetStreamDestination = file;
        } else {
//          Log.rapporterFejl(new IllegalStateException("Fil " + file + "  fandtes ikke alligevel??! for " + udsendelse));
          Log.rapporterFejl(new IllegalStateException("Fil " + file + " hentet, men fandtes ikke alligevel??!"));
        }
    } else {
      if (!new File(URI.create(udsendelse.hentetStream.url).getPath()).exists()) {
        Log.d("Fil findes pt ikke" + udsendelse.hentetStream);
        udsendelse.hentetStream = null;
      }
    }
  }

}
