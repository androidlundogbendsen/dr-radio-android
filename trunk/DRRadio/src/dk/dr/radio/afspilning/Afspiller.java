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

package dk.dr.radio.afspilning;

import dk.dr.radio.diverse.Opkaldshaandtering;
import java.io.IOException;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import dk.dr.radio.Afspilning_akt;
import dk.dr.radio.data.DRData;
import dk.dr.radio.diverse.AfspillerWidget;
import dk.dr.radio.R;
import dk.dr.radio.util.Log;
import dk.dr.radio.util.MedieafspillerInfo;
import java.util.ArrayList;

/**
 * Tidligere AfspillerService - afspilerdel
 * @author j
 */
public class Afspiller implements OnPreparedListener, OnSeekCompleteListener,
        OnCompletionListener, OnInfoListener, OnErrorListener, OnBufferingUpdateListener {

  /** ID til notifikation i toppen. Skal bare være unikt og det samme altid */
  private static final int NOTIFIKATION_ID = 117;

  /** Bruges fra widget til at kommunikere med servicen */
  //public static final int WIDGET_HENT_INFO = 10;
  public static final int WIDGET_START_ELLER_STOP = 11;

  /** Afspillerens status - bruges også i broadcasts */
  public static final int STATUS_UDEFINERET = 0;
  public static final int STATUS_STOPPET = 1;
  public static final int STATUS_FORBINDER = 2;
  public static final int STATUS_SPILLER = 3;
  public int afspillerstatus = STATUS_STOPPET;


  private MediaPlayer mediaPlayer;
  private List<AfspillerListener> observatører = new ArrayList<AfspillerListener>();

  public String kanalNavn;
  public String kanalUrl;
  //private Udsendelse aktuelUdsendelse;
  private String PROGRAMNAVN = "Radio";

  private static void sætMediaPlayerLytter(MediaPlayer mediaPlayer, Afspiller lytter) {
    mediaPlayer.setOnCompletionListener(lytter);
    mediaPlayer.setOnErrorListener(lytter);
    mediaPlayer.setOnInfoListener(lytter);
    mediaPlayer.setOnPreparedListener(lytter);
    mediaPlayer.setOnBufferingUpdateListener(lytter);
    mediaPlayer.setOnSeekCompleteListener(lytter);
    if (holdSkærmTændt && lytter!=null) mediaPlayer.setWakeMode(DRData.appCtx, PowerManager.SCREEN_DIM_WAKE_LOCK);
  }
  private final NotificationManager notificationManager;
  private final Opkaldshaandtering opkaldshåndtering;
  private final TelephonyManager tm;
  private Notification notification;

  private static boolean holdSkærmTændt;


  /** Forudsætter DRData er initialiseret */
  public Afspiller() {
    mediaPlayer = new MediaPlayer();

    sætMediaPlayerLytter(mediaPlayer, this);

    // Indlæs gamle værdier så vi har nogle...
    // Fjernet. Skulle ikke være nødvendigt. Jacob 22/10-2011
    // kanalNavn = p.getString("kanalNavn", "P1");
    // kanalUrl = p.getString("kanalUrl", "rtsp://live-rtsp.dr.dk/rtplive/_definst_/Channel5_LQ.stream");

    // Xperia Play har brug for at holde skærmen tændt. Muligvis også andre....
    holdSkærmTændt = "R800i".equals(Build.MODEL);
    String NØGLEholdSkærmTændt = "holdSkærmTændt";
    holdSkærmTændt = DRData.prefs.getBoolean(NØGLEholdSkærmTændt, holdSkærmTændt);
    // Gem værdi hvis den ikke findes, sådan at indstillingsskærm viser det rigtige
    if (!DRData.prefs.contains(NØGLEholdSkærmTændt)) DRData.prefs.edit().putBoolean(NØGLEholdSkærmTændt, holdSkærmTændt).commit();

    notificationManager = (NotificationManager) DRData.appCtx.getSystemService(Context.NOTIFICATION_SERVICE);

    opkaldshåndtering = new Opkaldshaandtering(this);
    tm = (TelephonyManager) DRData.appCtx.getSystemService(Context.TELEPHONY_SERVICE);
    tm.listen(opkaldshåndtering, PhoneStateListener.LISTEN_CALL_STATE);
  }

  private int onErrorTæller;
  private long onErrorTællerNultid;

  public void startAfspilning() throws IOException {
    Log.d("AfspillerService startAfspilning() "+ kanalUrl);

    onErrorTæller = 0;
    onErrorTællerNultid = System.currentTimeMillis();

    if (afspillerstatus == STATUS_STOPPET) {
      opdaterNotification();
      // Start afspillerservicen så programmet ikke bliver lukket
      // når det kører i baggrunden under afspilning
      DRData.appCtx.startService(new Intent(DRData.appCtx, HoldAppIHukommelsenService.class));
      startAfspilningIntern();

      AudioManager audioManager = (AudioManager) DRData.appCtx.getSystemService(Context.AUDIO_SERVICE);
      // Skru op til 1/5 styrke hvis volumen er lavere end det
      int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      int nu = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
      if (nu < 1 * max / 5) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1 * max / 5, AudioManager.FLAG_SHOW_UI);
      }

    } else Log.d(" forkert status="+afspillerstatus);
  }

  synchronized private void startAfspilningIntern() throws IOException  {
    Log.d("Starter streaming fra " + kanalNavn);

    mediaPlayer.setDataSource(kanalUrl);
    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    mediaPlayer.prepareAsync();
    afspillerstatus = STATUS_FORBINDER;
    sendOnAfspilningForbinder(-1);
    opdaterWidgets();
    handler.removeCallbacks(startAfspilningIntern);
  }

  synchronized public void stopAfspilning() {
    Log.d("AfspillerService stopAfspilning");
    handler.removeCallbacks(startAfspilningIntern);
    mediaPlayer.stop();
    // mediaPlayer.reset();
    // Da mediaPlayer.reset() erfaringsmæssigt kan hænge i dette tilfælde afregistrerer vi
    // alle lyttere og bruger en ny
    final MediaPlayer gammelMediaPlayer = mediaPlayer;
    sætMediaPlayerLytter(gammelMediaPlayer, null); // afregistrér alle lyttere
    new Thread() {
      @Override
      public void run() {
        Log.d("gammelMediaPlayer.release() start");
        gammelMediaPlayer.release();
        Log.d("gammelMediaPlayer.release() færdig");
      }
    }.start();

    mediaPlayer = new MediaPlayer();
    sætMediaPlayerLytter(mediaPlayer, this); // registrér lyttere på den nye instans

    afspillerstatus = STATUS_STOPPET;
    opdaterWidgets();

    if (notification != null) notificationManager.cancelAll();
    // Stop afspillerservicen
    DRData.appCtx.stopService(new Intent(DRData.appCtx, HoldAppIHukommelsenService.class));

    // Informer evt aktivitet der lytter
    for (AfspillerListener observatør : observatører) {
      observatør.onAfspilningStoppet();
    }
  }


  /**
   * Sætter notification i toppen af skærmen
   */
  private void opdaterNotification() {
    if (notification == null) {
      notification = new Notification(R.drawable.statusbaricon, null, 0);

      // PendingIntent er til at pege på aktiviteten der skal startes hvis brugeren vælger notifikationen
      notification.contentIntent = PendingIntent.getActivity(DRData.appCtx, 0, new Intent(DRData.appCtx, Afspilning_akt.class), 0);
      notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
    }

    notification.setLatestEventInfo(DRData.appCtx, PROGRAMNAVN, kanalNavn, notification.contentIntent);
    notificationManager.notify(NOTIFIKATION_ID, notification);
  }



  public void addAfspillerListener(AfspillerListener lytter) {
    if (!observatører.contains(lytter)) {
      observatører.add(lytter);
      // Informer lytteren om aktuel status
      if (afspillerstatus == STATUS_FORBINDER) {
        lytter.onAfspilningForbinder(-1);
      } else if (afspillerstatus == STATUS_STOPPET) {
        lytter.onAfspilningStoppet();
      } else {
        lytter.onAfspilningStartet();
      }
    }
  }

  public void removeAfspillerListener(AfspillerListener lytter) {
    observatører.remove(lytter);
  }


  public void setKanal(String navn, String url) {

    kanalNavn = navn;
    kanalUrl = url;

    // Fjernet. Skulle ikke være nødvendigt. Jacob 22/10-2011
    /*
    PreferenceManager.getDefaultSharedPreferences(DRData.appCtx).edit()
            .putString("kanalNavn", kanalNavn)
            .putString("kanalUrl", kanalUrl)
            .commit();
     */


    if ((afspillerstatus == STATUS_SPILLER) || (afspillerstatus == STATUS_FORBINDER)) {
      stopAfspilning();
      try {
        startAfspilning();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    opdaterWidgets();
  }


  private void opdaterWidgets() {

    AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(DRData.appCtx);
    int[] appWidgetId=mAppWidgetManager.getAppWidgetIds(new ComponentName(DRData.appCtx, AfspillerWidget.class));

    for (int id : appWidgetId) {
      AfspillerWidget.opdaterUdseende(DRData.appCtx, mAppWidgetManager, id);
    }
  }


  public int getAfspillerstatus() {
    return afspillerstatus;
  }



  //
  //    TILBAGEKALD FRA MEDIAPLAYER
  //
  public void onPrepared(MediaPlayer mp) {
    Log.d("AfspillerService onPrepared!");
    //mp.start();
    mediaPlayer.start();
    afspillerstatus = STATUS_SPILLER; //No longer buffering
    if (observatører != null) {
      opdaterWidgets();
      for (AfspillerListener observer : observatører) {
        observer.onAfspilningStartet();
      }
    }
  }

  public void onCompletion(MediaPlayer mp) {
    Log.d("AfspillerService onCompletion!");
    // Hvis forbindelsen mistes kommer der en onCompletion() og vi er derfor
    // nødt til at genstarte, medmindre brugeren trykkede stop
    if (afspillerstatus == STATUS_SPILLER) {
      Log.d("Genstarter afspilning!");
      mediaPlayer.stop();
      // mediaPlayer.reset();
      // Da mediaPlayer.reset() erfaringsmæssigt kan hænge i dette tilfælde afregistrerer vi
      // alle lyttere og bruger en ny
      final MediaPlayer gammelMediaPlayer = mediaPlayer;
      sætMediaPlayerLytter(gammelMediaPlayer, null); // afregistrér alle lyttere
      new Thread() {
        public void run() {
          Log.d("gammelMediaPlayer.release() start");
          gammelMediaPlayer.release();
          Log.d("gammelMediaPlayer.release() færdig");
        }
      }.start();

      mediaPlayer = new MediaPlayer();
      sætMediaPlayerLytter(mediaPlayer, this); // registrér lyttere på den nye instans

      try {
        startAfspilningIntern();
      } catch (IOException e) {
        Log.e(e);
      }
    } else {
    }
  }

  public boolean onInfo(MediaPlayer mp, int hvad, int extra) {
    //Log.d("onInfo(" + MedieafspillerInfo.infokodeTilStreng(hvad) + "(" + hvad + ") " + extra);
    Log.d("onInfo(" + hvad + ") " + extra);
    return true;
  }

  Handler handler = new Handler();
  Runnable startAfspilningIntern = new Runnable() {
    public void run() {
      try {
        startAfspilningIntern();
      } catch (Exception e) {
        Log.kritiskFejlStille(e);
      }
    }
  };

  public boolean onError(MediaPlayer mp, int hvad, int extra) {
    //Log.d("onError(" + MedieafspillerInfo.fejlkodeTilStreng(hvad) + "(" + hvad + ") " + extra+ " onErrorTæller="+onErrorTæller);
    Log.d("onError(" + hvad + ") " + extra+ " onErrorTæller="+onErrorTæller);
    // Iflg http://developer.android.com/guide/topics/media/index.html :
    // "It's important to remember that when an error occurs, the MediaPlayer moves to the Error
    //  state and you must reset it before you can use it again."
    if (afspillerstatus == STATUS_SPILLER || afspillerstatus == STATUS_FORBINDER) {


      // Hvis der har været
      // 1) færre end 10 fejl eller
      // 2) der højest er 1 fejl pr 20 sekunder så prøv igen
      long dt = System.currentTimeMillis()-onErrorTællerNultid;

      if (onErrorTæller ++<10 || (dt/onErrorTæller>20000) ) {
        mediaPlayer.stop();
        mediaPlayer.reset();

        // Vi venter længere og længere tid her
        int n = onErrorTæller;
        if (n>11) n=11;
        int ventetid = 10+ 5 * (1<<n); // fra n=0:10 msek til n=10:5 sek   til max n=11:10 sek
        Log.d("Ventetid før vi prøver igen: "+ventetid + "  n="+n+" "+onErrorTæller);
        handler.postDelayed(startAfspilningIntern, ventetid);
      } else {
        stopAfspilning(); // Vi giver op efter 10. forsøg
        Toast.makeText(DRData.appCtx, "Beklager, kan ikke spille radio", Toast.LENGTH_LONG).show();
        Toast.makeText(DRData.appCtx, "Prøv at vælge et andet format i indstillingerne", Toast.LENGTH_LONG).show();
      }
    } else {
      mediaPlayer.reset();
    }
    return true;
  }

  private void sendOnAfspilningForbinder(int procent) {
    for (AfspillerListener observer : observatører) {
      observer.onAfspilningForbinder(procent);
    }
  }

  public void onBufferingUpdate(MediaPlayer mp, int procent) {
    //Log.d("Afspiller onBufferingUpdate : " + procent + "% - lyttere er "+observatører );
    if (procent < -100) procent = -1; // Ignorér vilde tal

    sendOnAfspilningForbinder(procent);
  }

  public void onSeekComplete(MediaPlayer mp) {
    Log.d("AfspillerService onSeekComplete");
  }


  public void lukNed() {
    stopAfspilning();
    notificationManager.cancelAll(); // Luk notifikationen
    tm.listen(opkaldshåndtering, PhoneStateListener.LISTEN_NONE);
  }
}
