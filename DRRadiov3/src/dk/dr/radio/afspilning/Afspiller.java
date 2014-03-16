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
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.akt.diverse.AfspillerWidget;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Opkaldshaandtering;


/**
 * @author j
 */
public class Afspiller {

  public Status afspillerstatus = Status.STOPPET;

  private MediaPlayer mediaPlayer;
  private MediaPlayerLytter lytter = new MediaPlayerLytter();

  public List<Runnable> observatører = new ArrayList<Runnable>();
  public List<Runnable> forbindelseobservatører = new ArrayList<Runnable>();

  private String lydUrl;
  private int forbinderProcent;
  private Lydkilde lydkilde;
  private int duration;
  private int currentPosition;

  private static void sætMediaPlayerLytter(MediaPlayer mediaPlayer, MediaPlayerLytter lytter) {
    mediaPlayer.setOnCompletionListener(lytter);
    mediaPlayer.setOnErrorListener(lytter);
    mediaPlayer.setOnInfoListener(lytter);
    mediaPlayer.setOnPreparedListener(lytter);
    mediaPlayer.setOnBufferingUpdateListener(lytter);
    mediaPlayer.setOnSeekCompleteListener(lytter);
    if (lytter != null && App.prefs.getBoolean(NØGLEholdSkærmTændt, false)) {
      mediaPlayer.setWakeMode(App.instans, PowerManager.SCREEN_DIM_WAKE_LOCK);
    }
  }

  static final String NØGLEholdSkærmTændt = "holdSkærmTændt";
  private WifiLock wifilock = null;

  /**
   * Forudsætter DRData er initialiseret
   */
  public Afspiller() {
    mediaPlayer = new MediaPlayer();

    sætMediaPlayerLytter(mediaPlayer, this.lytter);
    // Indlæs gamle værdier så vi har nogle...
    // Fjernet. Skulle ikke være nødvendigt. Jacob 22/10-2011
    // kanalNavn = p.getString("kanalNavn", "P1");
    // lydUrl = p.getString("lydUrl", "rtsp://live-rtsp.dr.dk/rtplive/_definst_/Channel5_LQ.stream");

    // Gem værdi hvis den ikke findes, sådan at indstillingsskærm viser det rigtige
    if (!App.prefs.contains(NØGLEholdSkærmTændt)) {
      // Xperia Play har brug for at holde skærmen tændt. Muligvis også andre....
      boolean holdSkærmTændt = "R800i".equals(Build.MODEL);
      App.prefs.edit().putBoolean(NØGLEholdSkærmTændt, holdSkærmTændt).commit();
    }

    wifilock = ((WifiManager) App.instans.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DR Radio");
    wifilock.setReferenceCounted(false);
    Opkaldshaandtering opkaldshåndtering = new Opkaldshaandtering(this);
    TelephonyManager tm = (TelephonyManager) App.instans.getSystemService(Context.TELEPHONY_SERVICE);
    tm.listen(opkaldshåndtering, PhoneStateListener.LISTEN_CALL_STATE);
    /*
    // Opret en beggrundstråd med en Handler til at sende Runnables ind i
    new Thread() {
      public void run() {
        Looper.prepare();
        baggrundstråd = new Handler();
        Looper.loop();
      }
    }.start();
    */
  }

  private int onErrorTæller;
  private long onErrorTællerNultid;

  public void startAfspilning() {
    if (lydkilde.hentetStream==null && (!App.erOnline() || lydkilde.streams == null)) {
      App.kortToast("Kunne ikke oprette forbindelse");
      return;
    }
    lydUrl = lydkilde.findBedsteStream(false).url;
    DRData.instans.senestLyttede.registrérLytning(lydkilde);

    Log.d("startAfspilning() " + lydUrl);

    onErrorTæller = 0;
    onErrorTællerNultid = System.currentTimeMillis();

    if (afspillerstatus == Status.STOPPET) {
      //opdaterNotification();
      // Start afspillerservicen så programmet ikke bliver lukket
      // når det kører i baggrunden under afspilning
      App.instans.startService(new Intent(App.instans, HoldAppIHukommelsenService.class));
      if (App.prefs.getBoolean("wifilås", true) && wifilock != null) {
        wifilock.acquire();
      }
      startAfspilningIntern();
      AudioManager audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
      // Skru op til 1/5 styrke hvis volumen er lavere end det
      int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      int nu = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
      if (nu < 1 * max / 5) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1 * max / 5, AudioManager.FLAG_SHOW_UI);
      }

    } else Log.d(" forkert status=" + afspillerstatus);
  }

  long setDataSourceTid = 0;
  boolean setDataSourceLyd = false;

  private String mpTils() {
    AudioManager ar = (AudioManager) App.instans.getSystemService(App.AUDIO_SERVICE);
    //return mediaPlayer.getCurrentPosition()+ "/"+mediaPlayer.getDuration() + "    "+mediaPlayer.isPlaying()+ar.isMusicActive();
    if (!setDataSourceLyd && ar.isMusicActive()) {
      setDataSourceLyd = true;
      String str = "Det tog " + (System.currentTimeMillis() - setDataSourceTid) / 100 / 10.0 + " sek før lyden kom";
      Log.d(str);
      if (App.udvikling) {
        App.langToast(str);
      }
    }
    return "    " + ar.isMusicActive() + " dt=" + (System.currentTimeMillis() - setDataSourceTid) + "ms";
  }

  synchronized private void startAfspilningIntern() {
    Log.d("mediaPlayer.setDataSource( " + lydUrl);

    afspillerstatus = Status.FORBINDER;
    sendOnAfspilningForbinder(-1);
    opdaterObservatører();
    handler.removeCallbacks(startAfspilningIntern);

    // mediaPlayer.setDataSource() bør kaldes fra en baggrundstråd da det kan ske
    // at den hænger under visse netværksforhold
    new Thread() {
      public void run() {
        setDataSourceTid = System.currentTimeMillis();
        setDataSourceLyd = false;
        try {
          mediaPlayer.setDataSource(lydUrl);
          Log.d("mediaPlayer.setDataSource() slut");
          mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
          Log.d("mediaPlayer.setDataSource() slut  " + mpTils());
          mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
          mediaPlayer.prepare();
          Log.d("mediaPlayer.prepare() slut  " + mpTils());
        } catch (Exception ex) {
          ex.printStackTrace();
          //ex = new Exception("spiller "+kanalNavn+" "+lydUrl, ex);
          //Log.kritiskFejlStille(ex);
          handler.post(new Runnable() {
            public void run() { // Stop afspilleren fra forgrundstråden. Jacob 14/11
              lytter.onError(mediaPlayer, 42, 42); // kalder stopAfspilning(); og forsøger igen senere og melder fejl til bruger efter 10 forsøg
            }
          });
        }
      }
    }.start();
  }

  synchronized public void stopAfspilning() {
    Log.d("Afspiller stopAfspilning");
    handler.removeCallbacks(startAfspilningIntern);
    // Da mediaPlayer.reset() erfaringsmæssigt kan hænge i dette tilfælde afregistrerer vi
    // alle lyttere og bruger en ny
    final MediaPlayer gammelMediaPlayer = mediaPlayer;
    sætMediaPlayerLytter(gammelMediaPlayer, null); // afregistrér alle lyttere
    new Thread() {
      @Override
      public void run() {
        try {
          gammelMediaPlayer.stop();
          Log.d("gammelMediaPlayer.release() start");
          gammelMediaPlayer.release();
          Log.d("gammelMediaPlayer.release() færdig");
        } catch (Exception e) {
          Log.rapporterFejl(e);
        }
      }
    }.start();

    mediaPlayer = new MediaPlayer();
    sætMediaPlayerLytter(mediaPlayer, this.lytter); // registrér lyttere på den nye instans

    afspillerstatus = Status.STOPPET;
    opdaterObservatører();

    //if (notification != null) notificationManager.cancelAll();
    // Stop afspillerservicen
    App.instans.stopService(new Intent(App.instans, HoldAppIHukommelsenService.class));
    if (wifilock != null) wifilock.release();
    // Informer evt aktivitet der lytter
  }


  public void setLydkilde(Lydkilde lydkilde) {
    if (lydkilde == null) {
      Log.e(new IllegalStateException("setLydkilde(null"));
      return;
    }
    if (App.udvikling) App.kortToast("setLydkilde:\n" + lydkilde);
    this.lydkilde = lydkilde;


    if ((afspillerstatus == Status.SPILLER) || (afspillerstatus == Status.FORBINDER)) {
      stopAfspilning();
      try {
        startAfspilning();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    opdaterObservatører();
  }


  private void opdaterObservatører() {

    AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(App.instans);
    int[] appWidgetId = mAppWidgetManager.getAppWidgetIds(new ComponentName(App.instans, AfspillerWidget.class));

    for (int id : appWidgetId) {
      AfspillerWidget.opdaterUdseende(App.instans, mAppWidgetManager, id);
    }

    // Notificér alle i observatørlisen - fra en kopi, sådan at de kan fjerne
    // sig selv fra listen uden at det giver ConcurrentModificationException
    for (Runnable observatør : new ArrayList<Runnable>(observatører)) {
      observatør.run();
    }
  }


  public Status getAfspillerstatus() {
    return afspillerstatus;
  }


  public int getForbinderProcent() {
    return forbinderProcent;
  }

  public MediaPlayer getMediaPlayer() {
    return mediaPlayer;
  }

  public Lydkilde getLydkilde() {
    return lydkilde;
  }


  Handler handler = new Handler();
  Runnable startAfspilningIntern = new Runnable() {
    public void run() {
      try {
        startAfspilningIntern();
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    }
  };

  private void sendOnAfspilningForbinder(int procent) {
    forbinderProcent = procent;
    for (Runnable runnable : forbindelseobservatører) {
      runnable.run();
    }
  }

  public void seekTo(int progress) {
    mediaPlayer.seekTo(progress);
  }

  public int getDuration() {
    return mediaPlayer.getDuration();
  }

  public int getCurrentPosition() {
    return mediaPlayer.getCurrentPosition();
  }

  //
  //    TILBAGEKALD FRA MEDIAPLAYER
  //
  class MediaPlayerLytter implements OnPreparedListener, OnSeekCompleteListener, OnCompletionListener, OnInfoListener, OnErrorListener, OnBufferingUpdateListener {
    public void onPrepared(MediaPlayer mp) {
      Log.d("onPrepared " + mpTils());
      afspillerstatus = Status.SPILLER; //No longer buffering
      opdaterObservatører();
      // Det ser ud til kaldet til start() kan tage lang tid på Android 4.1 Jelly Bean
      // (i hvert fald på Samsung Galaxy S III), så vi kalder det i baggrunden
      new Thread() {
        public void run() {
          Log.d("mediaPlayer.start() " + mpTils());
          mediaPlayer.start();
          Log.d("mediaPlayer.start() slut " + mpTils());
        }
      }.start();
    }

    public void onCompletion(MediaPlayer mp) {
      Log.d("AfspillerService onCompletion!");
      // Hvis forbindelsen mistes kommer der en onCompletion() og vi er derfor
      // nødt til at genstarte, medmindre brugeren trykkede stop
      if (afspillerstatus == Status.SPILLER) {
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

        if (lydkilde.erStreaming()) {
          Log.d("Genstarter afspilning!");
          mediaPlayer = new MediaPlayer();
          sætMediaPlayerLytter(mediaPlayer, this); // registrér lyttere på den nye instans
          startAfspilningIntern();
        } else {
          stopAfspilning();
        }
      }
    }

    public boolean onInfo(MediaPlayer mp, int hvad, int extra) {
      //Log.d("onInfo(" + MedieafspillerInfo.infokodeTilStreng(hvad) + "(" + hvad + ") " + extra);
      Log.d("onInfo(" + hvad + ") " + extra + " " + mpTils());
      return true;
    }

    public boolean onError(MediaPlayer mp, int hvad, int extra) {
      //Log.d("onError(" + MedieafspillerInfo.fejlkodeTilStreng(hvad) + "(" + hvad + ") " + extra+ " onErrorTæller="+onErrorTæller);
      Log.d("onError(" + hvad + ") " + extra + " onErrorTæller=" + onErrorTæller);


      if (Build.VERSION.SDK_INT >= 16 && hvad == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
        // Ignorer, da Samsung Galaxy SIII på Android 4.1 Jelly Bean
        // sender denne fejl (onError(1) -110) men i øvrigt spiller fint videre!
        return true;
      }

      // Iflg http://developer.android.com/guide/topics/media/index.html :
      // "It's important to remember that when an error occurs, the MediaPlayer moves to the Error
      //  state and you must reset it before you can use it again."
      if (afspillerstatus == Status.SPILLER || afspillerstatus == Status.FORBINDER) {


        // Hvis der har været
        // 1) færre end 10 fejl eller
        // 2) der højest er 1 fejl pr 20 sekunder så prøv igen
        long dt = System.currentTimeMillis() - onErrorTællerNultid;

        if (onErrorTæller++ < (App.udvikling ? 2 : 10) || (dt / onErrorTæller > 20000)) {
          mediaPlayer.stop();
          mediaPlayer.reset();

          // Vi venter længere og længere tid her
          int n = onErrorTæller;
          if (n > 11) n = 11;
          int ventetid = 10 + 5 * (1 << n); // fra n=0:10 msek til n=10:5 sek   til max n=11:10 sek
          Log.d("Ventetid før vi prøver igen: " + ventetid + "  n=" + n + " " + onErrorTæller);
          handler.postDelayed(startAfspilningIntern, ventetid);
        } else {
          stopAfspilning(); // Vi giver op efter 10. forsøg
          App.langToast("Beklager, kan ikke spille radio");
          App.langToast("Prøv at vælge et andet format i indstillingerne");
        }
      } else {
        mediaPlayer.reset();
      }
      return true;
    }

    public void onBufferingUpdate(MediaPlayer mp, int procent) {
      Log.d("Afspiller onBufferingUpdate : " + procent + " " + mpTils());
      if (procent < -100) procent = -1; // Ignorér vilde tal

      sendOnAfspilningForbinder(procent);
    }

    public void onSeekComplete(MediaPlayer mp) {
      Log.d("AfspillerService onSeekComplete");
      //opdaterObservatører();
    }
  }
}
