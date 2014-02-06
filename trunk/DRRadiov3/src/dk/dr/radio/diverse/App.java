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
import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.akt_v3.Basisaktivitet;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Diverse;
import dk.dr.radio.data.stamdata.Stamdata;
import dk.dr.radio.v3.R;

public class App extends Application {
  public static boolean EMULATOR = true;
  public static App instans;
  public static SharedPreferences prefs;
  private static ConnectivityManager connectivityManager;
  private static String versionName;
  public static NotificationManager notificationManager;
  public static boolean udvikling = false;
  public static Handler forgrundstråd;
  public static Typeface skrift_normal;
  public static Typeface skrift_fed;


  @Override
  public void onCreate() {
    //BugSenseHandler.initAndStartSession(this, "57c90f98");
    super.onCreate();

    instans = this;
    EMULATOR = Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator");
    forgrundstråd = new Handler();
    connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    prefs = PreferenceManager.getDefaultSharedPreferences(this);

    // HTTP-forbindelser havde en fejl præ froyo, men jeg har også set problemet på Xperia Play, der er 2.3.4 (!)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      System.setProperty("http.keepAlive", "false");
    }
    try {
      Class.forName("android.os.AsyncTask"); // Fix for http://code.google.com/p/android/issues/detail?id=20915
      //noinspection ConstantConditions
      App.versionName = App.instans.getPackageManager().getPackageInfo(App.instans.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
      if (EMULATOR) App.versionName += " UDV";
      App.versionName += "/" + Build.MODEL + " " + Build.PRODUCT;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

    FilCache.init(getCacheDir());

    try {
      final DRData i = DRData.instans = new DRData();
      i.stamdata = Stamdata.parseAndroidStamdata(Diverse.læsInputStreamSomStreng(getResources().openRawResource(R.raw.stamdata1_android_v3_01)));
      i.stamdata.parseFællesStamdata(Diverse.læsInputStreamSomStreng(getResources().openRawResource(R.raw.stamdata2_faelles)));
      i.aktuelKanal = i.stamdata.forvalgtKanal;

      new AsyncTask() {
        @Override
        protected Object doInBackground(Object[] params) {
          i.stamdata.hentSupplerendeDataBg();
          return null;
        }
      }.execute();

      DRData.instans.afspiller = new Afspiller();
      /*
      // indlæs stamdata fra Prefs hvis de findes
      String stamdatastr = prefs.getString(DRData.STAMDATA_URL, null);

      if (stamdatastr == null) {
        // Indlæs fra raw this vi ikke har nogle cachede stamdata i prefs
        InputStream is = getResources().openRawResource(R.raw.stamdata1_android_v3_01);
        stamdatastr = Diverse.læsInputStreamSomStreng(is);
      }

      DRData.instans.stamdata = Stamdata.xxx_parseStamdatafil(stamdatastr);
      String alleKanalerUrl = DRData.instans.stamdata.json.getString("alleKanalerUrl");

      String alleKanalerStr = prefs.getString(alleKanalerUrl, null);
      if (alleKanalerStr == null) {
        // Indlæs fra raw this vi ikke har nogle cachede stamdata i prefs
        InputStream is = getResources().openRawResource(R.raw.skrald__alle_kanaler);
        alleKanalerStr = Diverse.læsInputStreamSomStreng(is);
      }
      DRData.instans.stamdata.skrald_parseAlleKanaler(alleKanalerStr);
      */
    } catch (Exception ex) {
      // TODO popop-advarsel til bruger om intern fejl og rapporter til udvikler-dialog
      Log.rapporterFejl(ex);
    }

    try { // DRs skrifttyper er ikke offentliggjort i SVN, derfor kan følgende fejle:
      skrift_normal = Typeface.createFromAsset(getAssets(), "Gibson-Regular.otf");
      skrift_fed = Typeface.createFromAsset(getAssets(), "Gibson-SemiBold.otf");
    } catch (Exception e) {
      Log.e("DRs skrifttyper er ikke tilgængelige", e);
      skrift_normal = Typeface.DEFAULT;
      skrift_fed = Typeface.DEFAULT_BOLD;
    }
  }


  /*
     * Version fra
     * http://developer.android.com/training/basics/network-ops/managing.html
     */
  public static boolean erOnline() {
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    return (networkInfo != null && networkInfo.isConnected());
  }


  public static Activity aktivitetIForgrunden = null;
  public static Activity senesteAktivitetIForgrunden = null;
  private static int erIGang = 0;

  public static void sætErIGang(boolean netværkErIGang) {
    boolean før = erIGang > 0;
    erIGang += netværkErIGang ? 1 : -1;
    boolean nu = erIGang > 0;
    if (udvikling) Log.d("erIGang = " + erIGang);
    if (før != nu && aktivitetIForgrunden != null) forgrundstråd.post(setProgressBarIndeterminateVisibility);
  }

  private static Runnable setProgressBarIndeterminateVisibility = new Runnable() {
    public void run() {
      Activity a = aktivitetIForgrunden; // trådsikkerhed
      if (a != null) {
        a.setProgressBarIndeterminateVisibility(erIGang > 0);
      }
    }
  };

  public void onResume(Basisaktivitet akt) {
    //((NotificationManager) getSystemService("notification")).cancelAll();
    akt.setProgressBarIndeterminateVisibility(erIGang > 0);
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
      modtagere = Diverse.jsonArrayTilArrayListString(DRData.instans.stamdata.json.getJSONArray("feedback_modtagere")).toArray(new String[0]);
    } catch (Exception ex) {
      Log.e("JSONParsning af feedback_modtagere", ex);
      modtagere = new String[]{"jacob.nordfalk@gmail.com"};
    }

    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("plain/text");
    i.putExtra(Intent.EXTRA_EMAIL, modtagere);
    i.putExtra(Intent.EXTRA_SUBJECT, emne);


    if (vedhæftning != null) try {
      String xmlFilename = "programlog.txt";
      @SuppressLint("WorldReadableFiles") FileOutputStream fos = akt.openFileOutput(xmlFilename, akt.MODE_WORLD_READABLE);
      fos.write(vedhæftning.getBytes());
      fos.close();
      Uri uri = Uri.fromFile(new File(akt.getFilesDir().getAbsolutePath(), xmlFilename));
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
      Log.rapporterOgvisFejl(akt, e);
    }
  }
}
