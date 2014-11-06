/**
 DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
 Some parts of the code are loosely based on Sveriges Radio Play for Android.

 DR Radio 2 for Android is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2 as published by
 the Free Software Foundation.

 DR Radio 2 for Android is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

 */

package dk.dr.radio.diverse;

/**
 *
 * @author j
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.androidquery.callback.BitmapAjaxCallback;
import com.bugsense.trace.BugSenseHandler;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.Fjernbetjening;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Diverse;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.volley.DrBasicNetwork;
import dk.dr.radio.diverse.volley.DrDiskBasedCache;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class App extends Application {
  public static final String P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING = "P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING";
  public static final String P4_FORETRUKKEN_AF_BRUGER = "P4_FORETRUKKEN_AF_BRUGER";
  public static final String FORETRUKKEN_KANAL = "FORETRUKKEN_kanal";
  public static final String NØGLE_advaretOmInstalleretPåSDKort = "erInstalleretPåSDKort";
  public static final boolean PRODUKTION = false;
  public static boolean EMULATOR = true; // Sæt i onCreate(), ellers virker det ikke i std Java
  public static App instans;
  public static SharedPreferences prefs;
  public static ConnectivityManager connectivityManager;
  public static String versionsnavn = "(ukendt)";
  public static NotificationManager notificationManager;
  public static boolean fejlsøgning = false;
  public static Handler forgrundstråd;
  public static Typeface skrift_gibson;
  public static Typeface skrift_gibson_fed;
  public static Typeface skrift_georgia;

  public static Netvaerksstatus netværk;
  public static Fjernbetjening fjernbetjening;
  public static RequestQueue volleyRequestQueue;
  public static boolean erInstalleretPåSDKort;
  private DrDiskBasedCache volleyCache;
  public static EgenTypefaceSpan skrift_gibson_fed_span;
  public static DRFarver color;
  public static Resources res;
  /** Tidsstempel der kan bruges til at afgøre hvilke filer der faktisk er brugt efter denne opstart */
  private static long TIDSSTEMPEL_VED_OPSTART;
  public static AccessibilityManager accessibilityManager;


  @SuppressLint("NewApi")
  @Override
  public void onCreate() {
    TIDSSTEMPEL_VED_OPSTART = System.currentTimeMillis();
    instans = this;
    netværk = new Netvaerksstatus();
    EMULATOR = Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator");
    if (!EMULATOR)
      BugSenseHandler.initAndStartSession(this, getString(PRODUKTION ? R.string.bugsense_nøgle : R.string.bugsense_testnøgle));
    super.onCreate();

    forgrundstråd = new Handler();
    connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    fejlsøgning = prefs.getBoolean("fejlsøgning", false);
    res = App.instans.getResources();
    App.color = new DRFarver();

    // HTTP-forbindelser havde en fejl præ froyo, men jeg har også set problemet på Xperia Play, der er 2.3.4 (!)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      System.setProperty("http.keepAlive", "false");
    }
    String packageName = getPackageName();
    try {
      //noinspection ConstantConditions
      PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
      App.versionsnavn = packageName + "/" + pi.versionName;
      if (EMULATOR) App.versionsnavn += " EMU";
      Log.d("App.versionsnavn=" + App.versionsnavn);

      App.erInstalleretPåSDKort = 0!=(pi.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE);
      /* check for API level 7 - check files dir
      try {
        String filesDir = context.getFilesDir().getAbsolutePath();
        if (filesDir.startsWith("/data/")) {
          return false;
        } else if (filesDir.contains("/mnt/") || filesDir.contains("/sdcard/")) {
          return true;
        }
      } catch (Throwable e) {
        // ignore
      }
      */
      if (!App.erInstalleretPåSDKort) prefs.edit().remove(NØGLE_advaretOmInstalleretPåSDKort).commit();

      Class.forName("android.os.AsyncTask"); // Fix for http://code.google.com/p/android/issues/detail?id=20915
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

    accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);


    // Initialisering af Volley
    volleyRequestQueue = Volley.newRequestQueue(this);

    // Prior to Gingerbread, HttpUrlConnection was unreliable.
    // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
    HttpStack stack =
        Build.VERSION.SDK_INT >= 9 ? new HurlStack()
            : Build.VERSION.SDK_INT >= 8 ? new HttpClientStack(AndroidHttpClient.newInstance(App.versionsnavn))
//            : new HttpClientStack(new DefaultHttpClient()); // Android 2.1 -
            : new HurlStack(); // Android 2.1
    // HTTP connection reuse was buggy pre-froyo
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
      System.setProperty("http.keepAlive", "false");
    }
    // Vi bruger vores eget Netværkslag, da DRs Varnish-servere ofte svarer med HTTP-kode 500,
    // som skal håndteres som et timeout og at der skal prøves igen
    Network network = new DrBasicNetwork(stack);
    // Vi bruger vores egen DrDiskBasedCache, da den indbyggede i Volley
    // har en opstartstid på flere sekunder
    File cacheDir = new File(getCacheDir(), "volley");
    volleyCache = new DrDiskBasedCache(cacheDir);
    volleyRequestQueue = new RequestQueue(volleyCache, network);
    volleyRequestQueue.start();

    // P4 stedplacering skal ske så tidligt som muligt - ellers
    // når P4-valgskærmbilledet at blive instantieret med ukendt placering og foreslår derfor København
    if (prefs.getString(P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, null) == null) startP4stedplacering();

    try {
      DRData.instans = new DRData();
      DRData.instans.grunddata = new Grunddata();

      // Indlæsning af grunddata/stamdata.
      // Først tjekkes om vi har en udgave i prefs, og ellers bruges den i raw-mappen
      // På et senere tidspunkt henter vi nye grunddata
      String grunddata = prefs.getString(DRData.GRUNDDATA_URL, null);
      if (grunddata == null)
        grunddata = Diverse.læsStreng(res.openRawResource(App.PRODUKTION ? R.raw.grunddata : R.raw.grunddata_udvikling));
      DRData.instans.grunddata.parseFællesGrunddata(grunddata);
      if (App.fejlsøgning && DRData.instans.grunddata.udelukHLS) App.kortToast("HLS er udelukket");

      String pn = App.instans.getPackageName();
      for (final Kanal k : DRData.instans.grunddata.kanaler) {
        k.kanallogo_resid = res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", pn);
      }

      String kanalkode = prefs.getString(FORETRUKKEN_KANAL, null);
      // Hvis brugeren foretrækker P4 er vi nødt til at finde underkanalen
      kanalkode = tjekP4OgVælgUnderkanal(kanalkode);

      Kanal aktuelKanal = DRData.instans.grunddata.kanalFraKode.get(kanalkode);
      if (aktuelKanal == null || aktuelKanal == Grunddata.ukendtKanal) {
        aktuelKanal = DRData.instans.grunddata.forvalgtKanal;
        Log.d("forvalgtKanal=" + aktuelKanal);
      }

      if (aktuelKanal.streams == null) { // ikke && App.erOnline(), det kan være vi har en cachet udgave
        final Kanal kanal = aktuelKanal;
        Request<?> req = new DrVolleyStringRequest(aktuelKanal.getStreamsUrl(), new DrVolleyResonseListener() {
          @Override
          public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
            if (uændret) return; // ingen grund til at parse det igen
            JSONObject o = new JSONObject(json);
            kanal.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
            Log.d("hentSupplerendeDataBgX " + kanal.kode + " fraCache=" + fraCache + " => " + kanal.slug + " k.lydUrl=" + kanal.streams);
          }
        }) {
          public Priority getPriority() {
            return Priority.HIGH;
          }
        };
        App.volleyRequestQueue.add(req);
      }

      DRData.instans.afspiller = new Afspiller();
      DRData.instans.afspiller.setLydkilde(aktuelKanal);


      registerReceiver(netværk, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
      netværk.onReceive(this, null); // Få opdateret netværksstatus
      fjernbetjening = new Fjernbetjening();

      // udeståendeInitialisering kaldes når aktivitet bliver synlig første gang
      // - muligvis aldrig hvis app'en kun betjenes via levende ikon

    } catch (Exception ex) {
      // Burde der være popop-advarsel til bruger om intern fejl og rapporter til udvikler-dialog ?
      Log.rapporterFejl(ex);
    }

    try { // DRs skrifttyper er ikke offentliggjort i SVN, derfor kan følgende fejle:
      skrift_gibson = Typeface.createFromAsset(getAssets(), "Gibson-Regular.otf");
      skrift_gibson_fed = Typeface.createFromAsset(getAssets(), "Gibson-SemiBold.otf");
      skrift_georgia = Typeface.createFromAsset(getAssets(), "Georgia.ttf");
    } catch (Exception e) {
      Log.e("DRs skrifttyper er ikke tilgængelige", e);
      skrift_gibson = Typeface.DEFAULT;
      skrift_gibson_fed = Typeface.DEFAULT_BOLD;
      skrift_georgia = Typeface.DEFAULT;
    }
    skrift_gibson_fed_span = new EgenTypefaceSpan("Gibson fed", App.skrift_gibson_fed);

    Log.d("onCreate tog " + (System.currentTimeMillis() - TIDSSTEMPEL_VED_OPSTART) + " ms");
  }

  public static String tjekP4OgVælgUnderkanal(String kanalkode) {
    if (Kanal.P4kode.equals(kanalkode)) {
      kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_AF_BRUGER, null);
      if (kanalkode == null) kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, "KH4");
      Log.d("P4 underkanal=" + kanalkode);
    }
    return kanalkode;
  }

  public static void advarEvtOmAlarmerHvisInstalleretPåSDkort(Activity akt) {
    if (App.erInstalleretPåSDKort && prefs.getBoolean(NØGLE_advaretOmInstalleretPåSDKort, false)) {
      AlertDialog.Builder dialog = new AlertDialog.Builder(akt);
      dialog.setTitle("SD-kort");
      dialog.setIcon(R.drawable.ic_dr_icon);
      dialog.setMessage("Vækning fungerer muligvis ikke altid, når DR Radio er flyttet til SD-kort");
      dialog.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          prefs.edit().putBoolean(NØGLE_advaretOmInstalleretPåSDKort, true).commit();
        }
      });
      dialog.show();
    }
  }

  private void startP4stedplacering() {
    new AsyncTask() {
      @Override
      protected Object doInBackground(Object[] params) {
        try {
          String p4kanal = P4Stedplacering.findP4KanalnavnFraIP();
          if (App.fejlsøgning) App.langToast("p4kanal: " + p4kanal);
          if (p4kanal != null) prefs.edit().putString(P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, p4kanal).commit();
          //if (!App.PRODUKTION) Log.rapporterFejl(new Exception("Ny enhed - fundet P4-kanal " + p4kanal));
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    }.execute();
  }

  /**
   * Initialisering af resterende data.
   * Dette sker når app'en er synlig og telefonen er online
   */
  private Runnable udeståendeInitialisering = new Runnable() {
    @Override
    public void run() {
      if (!erOnline()) return;
      boolean færdig = true;
      Log.d("Onlineinitialisering starter efter " + (System.currentTimeMillis() - TIDSSTEMPEL_VED_OPSTART) + " ms");

      if (App.netværk.status == Netvaerksstatus.Status.WIFI) { // Tjek at alle kanaler har deres streamsurler
        for (final Kanal k : DRData.instans.grunddata.kanaler) {
          if (k.streams != null) continue;
          //        Log.d("run()1 " + (System.currentTimeMillis() - TIDSSTEMPEL_VED_OPSTART) + " ms");
          Request<?> req = new DrVolleyStringRequest(k.getStreamsUrl(), new DrVolleyResonseListener() {
            @Override
            public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
              if (uændret) return;
              JSONObject o = new JSONObject(json);
              k.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
              Log.d("hentSupplerendeDataBg " + k.kode + " fraCache=" + fraCache + " => " + k.slug + " k.lydUrl=" + k.streams);
            }
          }) {
            public Priority getPriority() {
              return Priority.LOW;
            }
          };
          App.volleyRequestQueue.add(req);
        }
      }

      if (DRData.instans.favoritter.getAntalNyeUdsendelser() < 0) {
        færdig = false;
        DRData.instans.favoritter.startOpdaterAntalNyeUdsendelser.run();
      }


      if (DRData.instans.programserierAtilÅ.liste == null) {
        færdig = false;
        DRData.instans.programserierAtilÅ.startHentData();
      }


      if (DRData.instans.dramaOgBog.liste == null) {
        færdig = false;
        DRData.instans.dramaOgBog.startHentData();
      }

      if (!færdig) {
        App.forgrundstråd.postDelayed(this, 15000); // prøv igen om 15 sekunder og se om alle data er klar der
      }

      if (prefs.getString(P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, null) == null) {
        færdig = false;
        startP4stedplacering();
      }
      if (færdig) {
        netværk.observatører.remove(this); // Hold ikke mere øje med om vi kommer online
        udeståendeInitialisering = null;
      }
      Log.d("Onlineinitialisering færdig=" + færdig);
    }
  };


  public static Runnable hentEvtNyeGrunddata = new Runnable() {
    long sidstTjekket = 0;

    @Override
    public void run() {
      if (!App.erOnline()) return;
      if (sidstTjekket + (App.EMULATOR ? 1000 : DRData.instans.grunddata.opdaterGrunddataEfterMs) > System.currentTimeMillis())
        return;
      sidstTjekket = System.currentTimeMillis();
      Log.d("hentEvtNyeGrunddata " + (sidstTjekket - App.TIDSSTEMPEL_VED_OPSTART));
      Request<?> req = new DrVolleyStringRequest(DRData.GRUNDDATA_URL, new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String nyeGrunddata, boolean fraCache, boolean uændret) throws Exception {
          if (uændret || fraCache) return; // ingen grund til at parse det igen
          String gamleGrunddata = prefs.getString(DRData.GRUNDDATA_URL, null);
          if (nyeGrunddata.equals(gamleGrunddata)) return; // Det samme som var i prefs
          Log.d("Vi fik nye grunddata: fraCache=" + fraCache + nyeGrunddata);
          if (!PRODUKTION || App.fejlsøgning) App.kortToast("Vi fik nye grunddata");
          DRData.instans.grunddata.parseFællesGrunddata(nyeGrunddata);
          String pn = App.instans.getPackageName();
          for (final Kanal k : DRData.instans.grunddata.kanaler) {
            k.kanallogo_resid = res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", pn);
          }
          // Er vi nået hertil så gik parsning godt - gem de nye stamdata i prefs, så de også bruges ved næste opstart
          prefs.edit().putString(DRData.GRUNDDATA_URL, nyeGrunddata).commit();
        }
      }) {
        public Priority getPriority() {
          return Priority.LOW;
        }
      };
      App.volleyRequestQueue.add(req);
    }
  };

  /*
   * Kilde: http://developer.android.com/training/basics/network-ops/managing.html
   */
  public static boolean erOnline() {
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    return (networkInfo != null && networkInfo.isConnected());
  }


  public static Activity aktivitetIForgrunden = null;
  public static Activity senesteAktivitetIForgrunden = null;
  private static int erIGang = 0;

  /**
   * Signalerer over for brugeren at netværskommunikation er påbegyndt eller afsluttet.
   * Forårsager at det 'drejende hjul' (ProgressBar) vises på den aktivitet der er synlig p.t.
   * @param netværkErIGang true for påbegyndt og false for afsluttet.
   */
  public static synchronized void sætErIGang(boolean netværkErIGang) {
    boolean før = erIGang > 0;
    erIGang += netværkErIGang ? 1 : -1;
    boolean nu = erIGang > 0;
    if (fejlsøgning) Log.d("erIGang = " + erIGang);
    if (erIGang < 0) {
      Log.e(new IllegalStateException("erIGang er " + erIGang));
      erIGang = 0;
    }
    if (før != nu && aktivitetIForgrunden != null) forgrundstråd.post(setProgressBarIndeterminateVisibility);
    // Fejltjek
  }

  private static Runnable setProgressBarIndeterminateVisibility = new Runnable() {
    public void run() {
      if (aktivitetIForgrunden instanceof ActionBarActivity) {
        ((ActionBarActivity) aktivitetIForgrunden).setSupportProgressBarIndeterminateVisibility(erIGang > 0);
      }
    }
  };

  public void aktivitetStartet(Activity akt) {
    //((NotificationManager) getSystemService("notification")).cancelAll();
    setProgressBarIndeterminateVisibility.run();
    senesteAktivitetIForgrunden = aktivitetIForgrunden = akt;
    if (udeståendeInitialisering != null) {
      if (App.erOnline()) {
        App.forgrundstråd.postDelayed(udeståendeInitialisering, 250); // Initialisér onlinedata
      } else {
        App.netværk.observatører.add(udeståendeInitialisering); // Vent på at vi kommer online og lav så et tjek
      }
    }
    if (kørFørsteGangAppIkkeMereErSynlig != null) forgrundstråd.removeCallbacks(kørFørsteGangAppIkkeMereErSynlig);
  }

  public void aktivitetStoppet(Activity akt) {
    if (akt != aktivitetIForgrunden) return; // en anden aktivitet er allerede startet
    aktivitetIForgrunden = null;
    if (kørFørsteGangAppIkkeMereErSynlig != null) forgrundstråd.postDelayed(kørFørsteGangAppIkkeMereErSynlig, 1000);
  }

  /**
   * Køres et sekund efter at app'en ikke mere er synlig.
   * Her rydder vi op i filer
   */
  private Runnable kørFørsteGangAppIkkeMereErSynlig = new Runnable() {
    @Override
    public void run() {
      if (aktivitetIForgrunden != null) return;
      if (App.fejlsøgning) App.kortToast("kørFørsteGangAppIkkeMereErSynlig");
      long alder = TIDSSTEMPEL_VED_OPSTART - 7 * 24 * 60 * 60 * 1000;
      int volleySlettet = volleyCache.sletFilerÆldreEnd(alder);
      int aqSlettet = Diverse.sletFilerÆldreEnd(new File(getCacheDir(), "aquery"), alder);

      if (fejlsøgning) {
        App.kortToast("volleyCache: " + volleySlettet / 1000 + " kb frigivet");
        App.kortToast("AQ: " + aqSlettet / 1000 + " kb kunne frigivet");
      }
      kørFørsteGangAppIkkeMereErSynlig = null;
    }
  };

  public static void langToast(String txt) {
    Log.d("langToast(" + txt);
    if (aktivitetIForgrunden == null) txt = "DR Radio:\n" + txt;
    final String txt2 = txt;
    forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(instans, txt2, Toast.LENGTH_LONG).show();
      }
    });
  }

  public static void kortToast(String txt) {
    Log.d("kortToast(" + txt);
    if (aktivitetIForgrunden == null) txt = "DR Radio:\n" + txt;
    final String txt2 = txt;
    forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(instans, txt2, Toast.LENGTH_SHORT).show();
      }
    });
  }


  public static void kontakt(Activity akt, String emne, String txt, String vedhæftning) {
    String[] modtagere;
    try {
      modtagere = Diverse.jsonArrayTilArrayListString(DRData.instans.grunddata.android_json.getJSONArray("kontakt_modtagere")).toArray(new String[0]);
    } catch (Exception ex) {
      Log.e(ex);
      modtagere = new String[]{"jacob.nordfalk@gmail.com"};
    }

    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("plain/text");
    i.putExtra(Intent.EXTRA_EMAIL, modtagere);
    i.putExtra(Intent.EXTRA_SUBJECT, emne);


    android.util.Log.d("KONTAKT", txt);
    if (vedhæftning != null) try {
      String logfil = "programlog.txt";
      @SuppressLint("WorldReadableFiles") FileOutputStream fos = akt.openFileOutput(logfil, akt.MODE_WORLD_READABLE);
      fos.write(vedhæftning.getBytes());
      fos.close();
      Uri uri = Uri.fromFile(new File(akt.getFilesDir().getAbsolutePath(), logfil));
      txt += "\n\nRul op øverst i meddelelsen og giv din feedback, tak.";
      i.putExtra(Intent.EXTRA_STREAM, uri);
    } catch (Exception e) {
      Log.e(e);
      txt += "\n" + e;
    }
    i.putExtra(Intent.EXTRA_TEXT, txt);
//    akt.startActivity(Intent.createChooser(i, "Send meddelelse..."));
    try {
      akt.startActivity(i);
    } catch (Exception e) {
      App.langToast(e.toString());
      Log.rapporterFejl(e);
    }
  }


  @Override
  public void onLowMemory() {
    // Ryd op når der mangler RAM
    BitmapAjaxCallback.clearCache();
    super.onLowMemory();
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public void onTrimMemory(int level) {
    if (level >= TRIM_MEMORY_BACKGROUND) BitmapAjaxCallback.clearCache();
    super.onTrimMemory(level);
  }

  /**
   * I fald telefonens ur går forkert kan det ses her - alle HTTP-svar bliver jo stemplet med servertiden
   */
  private static long serverkorrektionTilKlienttidMs = 0;

  /**
   * Giver et aktuelt tidsstempel på hvad serverens ur viser
   * @return tiden, i  millisekunder siden 1. Januar 1970 00:00:00.0 UTC.
   */
  public static long serverCurrentTimeMillis() {
    return System.currentTimeMillis() + serverkorrektionTilKlienttidMs;
  }

  public static void sætServerCurrentTimeMillis(long servertid) {
    long serverkorrektionTilKlienttidMs2 = servertid - System.currentTimeMillis();
    if (Math.abs(App.serverkorrektionTilKlienttidMs - serverkorrektionTilKlienttidMs2) > 120 * 1000) {
      Log.d("SERVERTID korrigerer tid - serverkorrektionTilKlienttidMs=" + serverkorrektionTilKlienttidMs2 + " klokken på serveren er " + new Date(servertid));
      App.serverkorrektionTilKlienttidMs = serverkorrektionTilKlienttidMs2;
      new Exception("SERVERTID korrigeret med " + serverkorrektionTilKlienttidMs2 / 1000 / 60 + " min til " + new Date(servertid)).printStackTrace();
    }
  }

  /** Kan kaldet til at afgøre om vi er igang med at teste noget fra en main()-metode eller app'en rent faktisk kører */
  public static boolean testFraMain() {
    return instans == null;
  }

  /**
   * Lille klasse der holder nogle farver vi ikke gider slå op i resurser efter hele tiden
   */
  public static class DRFarver {
    public int grå40 = res.getColor(R.color.grå40);
    public int blå = res.getColor(R.color.blå);
    public int grå60 = res.getColor(R.color.grå60);
  }
}
