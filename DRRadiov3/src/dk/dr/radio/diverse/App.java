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
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.akt.Basisaktivitet;
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

public class App extends Application implements Runnable {
  public static final String P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING = "P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING";
  public static final String P4_FORETRUKKEN_AF_BRUGER = "P4_FORETRUKKEN_AF_BRUGER";
  public static final String FORETRUKKEN_KANAL = "FORETRUKKEN_kanal";
  public static final boolean PRODUKTION = false;
  public static boolean EMULATOR = true;
  public static App instans;
  public static SharedPreferences prefs;
  public static ConnectivityManager connectivityManager;
  public static String versionsnavn = "(ukendt)";
  public static NotificationManager notificationManager;
  public static boolean fejlsøgning = false; // TODO - omdøb til fejlsøgning
  public static boolean udviklerEkstra = false; // Vis ekstra muligheder til udviklere og fejlfinding
  public static Handler forgrundstråd;
  public static Typeface skrift_gibson;
  public static Typeface skrift_gibson_fed;
  public static Typeface skrift_georgia;

  public static Netvaerksstatus netværk;
  public static RequestQueue volleyRequestQueue;
  public static EgenTypefaceSpan skrift_gibson_fed_span;
  public static DRFarver color;
  public static Resources res;
  public static long opstartstidspunkt;


  @SuppressLint("NewApi")
  @Override
  public void onCreate() {
    opstartstidspunkt = System.currentTimeMillis();
    instans = this;
    netværk = new Netvaerksstatus();
    EMULATOR = Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator");
    if (!EMULATOR) BugSenseHandler.initAndStartSession(this, getString(R.string.bugsense_nøgle));
    super.onCreate();

    forgrundstråd = new Handler();
    connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    fejlsøgning = prefs.getBoolean("fejlsøgning", false);
    udviklerEkstra = prefs.getBoolean("udviklerEkstra", false);
    res = App.instans.getResources();
    App.color = new DRFarver();

    // HTTP-forbindelser havde en fejl præ froyo, men jeg har også set problemet på Xperia Play, der er 2.3.4 (!)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      System.setProperty("http.keepAlive", "false");
    }
    String packageName = getPackageName();
    try {
      //noinspection ConstantConditions
      App.versionsnavn = packageName+"/"+getPackageManager().getPackageInfo(packageName, 0).versionName;
      if (EMULATOR) App.versionsnavn += " UDV";
      Log.d("App.versionsnavn="+App.versionsnavn);
      Class.forName("android.os.AsyncTask"); // Fix for http://code.google.com/p/android/issues/detail?id=20915
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

    FilCache.init(getCacheDir());


    // Initialisering af Volley
    volleyRequestQueue = Volley.newRequestQueue(this);

    // Prior to Gingerbread, HttpUrlConnection was unreliable.
    // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
    HttpStack stack =
          Build.VERSION.SDK_INT >= 9 ? new HurlStack()
        : Build.VERSION.SDK_INT >= 8 ? new HttpClientStack(AndroidHttpClient.newInstance(App.versionsnavn))
        : new HttpClientStack(new DefaultHttpClient()); // Android 2.1

    // Vi bruger vores eget Netværkslag, da DRs Varnish-servere ofte svarer med HTTP-kode 500,
    // som skal håndteres som et timeout og at der skal prøves igen
    Network network = new DrBasicNetwork(stack);
    // Vi bruger vores egen DrDiskBasedCache, da den indbyggede i Volley
    // har en opstartstid på flere sekunder
    File cacheDir = new File(getCacheDir(), "volley4");
    volleyRequestQueue = new RequestQueue(new DrDiskBasedCache(cacheDir), network);
    volleyRequestQueue.start();

    try {
      DRData.instans = new DRData();
      DRData.instans.grunddata = new Grunddata();
      DRData.instans.grunddata.parseFællesGrunddata(Diverse.læsStreng(res.openRawResource(R.raw.grunddata)));

      String kanalkode = prefs.getString(FORETRUKKEN_KANAL, null);
      Kanal aktuelKanal = DRData.instans.grunddata.kanalFraKode.get(kanalkode);
      if (aktuelKanal == null || aktuelKanal==Grunddata.ukendtKanal) {
        aktuelKanal = DRData.instans.grunddata.forvalgtKanal;
        Log.d("forvalgtKanal="+aktuelKanal);
      }

      DRData.instans.afspiller = new Afspiller();
      DRData.instans.afspiller.setLydkilde(aktuelKanal);

      String pn = App.instans.getPackageName();
      for (final Kanal k : DRData.instans.grunddata.kanaler) {
        k.kanallogo_resid = res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", pn);
      }


      IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
      registerReceiver(netværk, filter);
      netværk.onReceive(this, null); // Få opdateret netværksstatus
      //langToast("xxxx "+App.fejlsøgning);

      if (erOnline()) {
        App.forgrundstråd.postDelayed(this, 100); // Initialisér onlinedata
      } else {
        netværk.observatører.add(this); // Vent på vi kommer online og lav så et tjek
      }




      /*
      // indlæs grunddata fra Prefs hvis de findes
      String stamdatastr = prefs.getString(DRData.GRUNDDATA_URL, null);

      if (stamdatastr == null) {
        // Indlæs fra raw this vi ikke har nogle cachede grunddata i prefs
        InputStream is = getResources().openRawResource(R.raw.stamdata1_android_v3_01);
        stamdatastr = Diverse.læsStreng(is);
      }

      DRData.instans.grunddata = Stamdata.xxx_parseStamdatafil(stamdatastr);
      String alleKanalerUrl = DRData.instans.grunddata.json.getString("alleKanalerUrl");

      String alleKanalerStr = prefs.getString(alleKanalerUrl, null);
      if (alleKanalerStr == null) {
        // Indlæs fra raw this vi ikke har nogle cachede grunddata i prefs
        InputStream is = getResources().openRawResource(R.raw.skrald__alle_kanaler);
        alleKanalerStr = Diverse.læsStreng(is);
      }
      DRData.instans.grunddata.skrald_parseAlleKanaler(alleKanalerStr);
      */
    } catch (Exception ex) {
      // TODO popop-advarsel til bruger om intern fejl og rapporter til udvikler-dialog
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

    Log.d("onCreate tog " + (System.currentTimeMillis() - opstartstidspunkt) + " ms");
  }

  /**
   * ONLINEINITIALISERING
   */
  public void run() {
    if (!erOnline()) return;
    boolean færdig = true;
    // Tidligere hentSupplerendeDataBg

    if (App.netværk.status== Netvaerksstatus.Status.WIFI)  { // Tjek at alle kanaler har deres streamsurler
      for (final Kanal k : DRData.instans.grunddata.kanaler) {
        if (k.streams != null) continue;
//        Log.d("run()1 " + (System.currentTimeMillis() - opstartstidspunkt) + " ms");
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
//        Log.d("run()2 " + (System.currentTimeMillis() - opstartstidspunkt) + " ms");
        App.volleyRequestQueue.add(req);
//        Log.d("run()3 " + (System.currentTimeMillis() - opstartstidspunkt) + " ms");
      }
    }

    if (DRData.instans.favoritter.getAntalNyeUdsendelser() < 0) {
      færdig = false;
      DRData.instans.favoritter.startOpdaterAntalNyeUdsendelser.run();
    }

    if (!færdig) {
      App.forgrundstråd.postDelayed(this, 15000); // prøv igen om 15 sekunder og se om alle data er klar der
    }

    if (prefs.getString(P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, null) == null) {
      {
        færdig = false;
        new AsyncTask() {
          @Override
          protected Object doInBackground(Object[] params) {
            try {
              String p4kanal = P4Stedplacering.findP4KanalnavnFraIP();
              if (App.fejlsøgning) App.langToast("p4kanal: " + p4kanal);
              if (p4kanal != null) prefs.edit().putString(P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, p4kanal).commit();
              Log.rapporterFejl(new Exception("Ny enhed - fundet P4-kanal " + p4kanal));
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        }.execute();
      }
    }
    if (færdig) {
      netværk.observatører.remove(this); // Hold ikke mere øje med om vi kommer online
    }
  }


  /*
   * Kilde: http://developer.android.com/training/basics/network-ops/managing.html
   */
  public static boolean erOnline() {
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    return (networkInfo != null && networkInfo.isConnected());
  }


  public static Basisaktivitet aktivitetIForgrunden = null;
  public static Basisaktivitet senesteAktivitetIForgrunden = null;
  private static int erIGang = 0;

  /**
   * Signalerer over for brugeren at netværskommunikation er påbegyndt eller afsluttet.
   * Forårsager at det 'drejende hjul' (ProgressBar) vises på den aktivitet der er synlig p.t.
   *
   * @param netværkErIGang true for påbegyndt og false for afsluttet.
   */
  public static void sætErIGang(boolean netværkErIGang) {
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
      Basisaktivitet a = aktivitetIForgrunden; // trådsikkerhed
      if (a != null) {
        a.setSupportProgressBarIndeterminateVisibility(erIGang > 0);
      }
    }
  };

  public void onResume(Basisaktivitet akt) {
    //((NotificationManager) getSystemService("notification")).cancelAll();
    setProgressBarIndeterminateVisibility.run();
    senesteAktivitetIForgrunden = aktivitetIForgrunden = akt;
  }

  public void onPause() {
    aktivitetIForgrunden = null;
  }


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
    if (Math.abs(App.serverkorrektionTilKlienttidMs - serverkorrektionTilKlienttidMs2) > 30000) {
      Log.d("SERVERTID korrigerer tid - serverkorrektionTilKlienttidMs=" + serverkorrektionTilKlienttidMs2+" klokken på serveren er "+new Date(servertid));
      App.serverkorrektionTilKlienttidMs = serverkorrektionTilKlienttidMs2;
      new Exception("SERVERTID korrigeret til "+new Date(servertid)).printStackTrace();
    }
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
