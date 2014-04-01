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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.AfspillerIkonOgNotifikation;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Opkaldshaandtering;

/**
 * @author j
 */
public class Afspiller {

  public Status afspillerstatus = Status.STOPPET;

  private MediaPlayerWrapper mediaPlayer;
  private MediaPlayerLytter lytter = new MediaPlayerLytterImpl();

  public List<Runnable> observatører = new ArrayList<Runnable>();
  public List<Runnable> forbindelseobservatører = new ArrayList<Runnable>();

  private String lydUrl;
  private int forbinderProcent;
  private Lydkilde lydkilde;

  private static void sætMediaPlayerLytter(MediaPlayerWrapper mediaPlayer, MediaPlayerLytter lytter) {
    mediaPlayer.setMediaPlayerLytter(lytter);
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
    mediaPlayer = MediaPlayerWrapper.opret();

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
    // Opret en baggrundstråd med en Handler til at sende Runnables ind i
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
    if (lydkilde.hentetStream == null && (!App.erOnline() || lydkilde.streams == null)) {
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

      AudioManager audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
      if (Build.VERSION.SDK_INT >= 8) {
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(opretFocusChangeListener(),
            // Use the music stream.
            AudioManager.STREAM_MUSIC,
            // Request permanent focus.
            AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
          startAfspilningIntern();
        }
      } else {
        startAfspilningIntern();
      }


      // Skru op til 1/5 styrke hvis volumen er lavere end det
      int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      int nu = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
      if (nu < 1 * max / 5) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1 * max / 5, AudioManager.FLAG_SHOW_UI);
      }

    } else Log.d(" forkert status=" + afspillerstatus);
  }


  // Da OnAudioFocusChangeListener ikke findes i API<8 kan vi ikke bruge klassen her
  Object onAudioFocusChangeListener;

  /**
   * Responding to the loss of audio focus
   */
  @SuppressLint("NewApi")
  private OnAudioFocusChangeListener opretFocusChangeListener() {
    if (onAudioFocusChangeListener == null)
      onAudioFocusChangeListener = new OnAudioFocusChangeListener() {

        public int lydstyreFørDuck = -1;

        @TargetApi(Build.VERSION_CODES.FROYO)
        public void onAudioFocusChange(int focusChange) {
          AudioManager am = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);


          switch (focusChange) {
            case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
              // Lower the volume while ducking.
              Log.d("JPER duck");
              lydstyreFørDuck = am.getStreamVolume(AudioManager.STREAM_MUSIC);
              am.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
              break;

            case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
              Log.d("JPER pause");
              pauseAfspilning();
              break;

            case (AudioManager.AUDIOFOCUS_LOSS):
              Log.d("JPER stop");
              //stopAfspilning();
              pauseAfspilning();
              am.abandonAudioFocus(this);
              break;

            case (AudioManager.AUDIOFOCUS_GAIN):
              Log.d("JPER Gain");
              if (DRData.instans.afspiller.afspillerstatus == Status.STOPPET) {
                //gør intet da playeren ikke spiller.
              } else {
                // Return the volume to normal and resume if paused.
                if (lydstyreFørDuck>0) {
                  am.setStreamVolume(AudioManager.STREAM_MUSIC, lydstyreFørDuck, 0);
                }
                // Genstart ikke afspilning
                //startAfspilningIntern();

              }

              break;

            default:
              break;
          }
        }
      };
    return (OnAudioFocusChangeListener) onAudioFocusChangeListener;
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
      if (App.fejlsøgning) {
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
          mediaPlayer.prepare();
          Log.d("mediaPlayer.prepare() slut  " + mpTils());
        } catch (Exception ex) {
          ex.printStackTrace();
          //ex = new Exception("spiller "+kanalNavn+" "+lydUrl, ex);
          //Log.kritiskFejlStille(ex);
          handler.post(new Runnable() {
            public void run() { // Stop afspilleren fra forgrundstråden. Jacob 14/11
              lytter.onError(null, 42, 42); // kalder stopAfspilning(); og forsøger igen senere og melder fejl til bruger efter 10 forsøg
            }
          });
        }
      }
    }.start();
  }

  synchronized private void pauseAfspilningIntern() {
    handler.removeCallbacks(startAfspilningIntern);
    // Da mediaPlayer.reset() erfaringsmæssigt kan hænge i dette tilfælde afregistrerer vi
    // alle lyttere og bruger en ny
    final MediaPlayerWrapper gammelMediaPlayer = mediaPlayer;
    sætMediaPlayerLytter(gammelMediaPlayer, null); // afregistrér alle lyttere
    new Thread() {
      @Override
      public void run() {
        try {
          try { // Ignorér IllegalStateException, det er fordi den allerede er stoppet
            gammelMediaPlayer.stop();
          } catch (IllegalStateException e) {
          }
          Log.d("gammelMediaPlayer.release() start");
          gammelMediaPlayer.release();
          Log.d("gammelMediaPlayer.release() færdig");
        } catch (Exception e) {
          Log.rapporterFejl(e);
        }
      }
    }.start();

    mediaPlayer = MediaPlayerWrapper.opret();
    sætMediaPlayerLytter(mediaPlayer, this.lytter); // registrér lyttere på den nye instans

    afspillerstatus = Status.STOPPET;
    opdaterObservatører();
  }

  synchronized public void pauseAfspilning() {
    if (!lydkilde.erDirekte())
      try { // Gem position - og spol herhen næste gang udsendelsen spiller
        lydkilde.getUdsendelse().startposition = mediaPlayer.getCurrentPosition();
      } catch (Exception e) {
        Log.rapporterFejl(e); // TODO fjern hvis der aldrig kommer fejl her
      }
    pauseAfspilningIntern();
    if (wifilock != null) wifilock.release();
    // Informer evt aktivitet der lytter
  }


  synchronized public void stopAfspilning() {
    Log.d("Afspiller stopAfspilning");
    pauseAfspilning();
    // Stop afspillerservicen
    App.instans.stopService(new Intent(App.instans, HoldAppIHukommelsenService.class));
  }


  public void setLydkilde(Lydkilde lydkilde) {
    if (lydkilde == null) {
      Log.e(new IllegalStateException("setLydkilde(null"));
      return;
    }
    if (App.fejlsøgning) App.kortToast("setLydkilde:\n" + lydkilde);


    if ((afspillerstatus == Status.SPILLER) || (afspillerstatus == Status.FORBINDER)) {
      stopAfspilning(); // gemmer lydkildens position
      this.lydkilde = lydkilde;
      try {
        startAfspilning(); // sætter afspilleren til den nye lydkildes position
      } catch (Exception e) {
        Log.rapporterFejl(e); // TODO fjern efter et par måneder i drift
      }
    } else {
      this.lydkilde = lydkilde;
    }
    opdaterObservatører();
  }


  private void opdaterObservatører() {

    AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(App.instans);
    int[] appWidgetId = mAppWidgetManager.getAppWidgetIds(new ComponentName(App.instans, AfspillerIkonOgNotifikation.class));

    for (int id : appWidgetId) {
      AfspillerIkonOgNotifikation.opdaterUdseende(App.instans, mAppWidgetManager, id);
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

  public Lydkilde getLydkilde() {
    return lydkilde;
  }


  Handler handler = new Handler();
  Runnable startAfspilningIntern = new Runnable() {
    public void run() {
      try {
        if (App.netværk.observatører.contains(startAfspilningIntern)) {
          if (!App.erOnline())
            Log.e(new IllegalStateException("Burde være online her??!"));
          long dt = System.currentTimeMillis() - onErrorTællerNultid;
          Log.d("Vi kom online igen efter " + dt + " ms");
          if (dt < 5 * 60 * 1000) {
            Log.d("Genstart afspilning");
            startAfspilningIntern(); // Genstart
          } else {
            Log.d("Brugeren har nok glemt os, afslut");
            stopAfspilning();
          }
          return;
        }
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
  class MediaPlayerLytterImpl implements MediaPlayerLytter {
    public void onPrepared(MediaPlayer mp) {
      Log.d("onPrepared " + mpTils());
      afspillerstatus = Status.SPILLER; //No longer buffering
      opdaterObservatører();
      // Det ser ud til kaldet til start() kan tage lang tid på Android 4.1 Jelly Bean
      // (i hvert fald på Samsung Galaxy S III), så vi kalder det i baggrunden
      new Thread() {
        public void run() {
          Log.d("mediaPlayer.start() " + mpTils());
          Udsendelse u = lydkilde.getUdsendelse();
          int startposition = u == null ? 0 : u.startposition;
          if (startposition > 0) {
            Log.d("mediaPlayer genoptager afspilning ved " + startposition);
            mediaPlayer.seekTo(startposition);
          }
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
        final MediaPlayerWrapper gammelMediaPlayer = mediaPlayer;
        sætMediaPlayerLytter(gammelMediaPlayer, null); // afregistrér alle lyttere
        new Thread() {
          public void run() {
            Log.d("gammelMediaPlayer.release() start");
            gammelMediaPlayer.release();
            Log.d("gammelMediaPlayer.release() færdig");
          }
        }.start();

        if (lydkilde.erDirekte()) {
          Log.d("Genstarter afspilning!");
          mediaPlayer = MediaPlayerWrapper.opret();
          sætMediaPlayerLytter(mediaPlayer, this); // registrér lyttere på den nye instans
          startAfspilningIntern();
        } else {
          lydkilde.getUdsendelse().startposition = 0;
          stopAfspilning();
        }
      }
    }

    public boolean onError(MediaPlayer mp_UBRUGT, int hvad, int extra) {
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

        if (onErrorTæller++ < (App.fejlsøgning ? 2 : 10) || (dt / onErrorTæller > 20000)) {
          pauseAfspilningIntern();
          //mediaPlayer.stop();
          //mediaPlayer.reset();

          if (App.erOnline()) {
            // Vi venter længere og længere tid her
            int n = onErrorTæller;
            if (n > 11) n = 11;
            int ventetid = 10 + 5 * (1 << n); // fra n=0:10 msek til n=10:5 sek   til max n=11:10 sek
            Log.d("Ventetid før vi prøver igen: " + ventetid + "  n=" + n + " " + onErrorTæller);
            handler.postDelayed(startAfspilningIntern, ventetid);
          } else {
            Log.d("Vent på at vi kommer online igen");
            onErrorTællerNultid = System.currentTimeMillis();
            App.netværk.observatører.add(startAfspilningIntern);
          }
        } else {
          pauseAfspilning(); // Vi giver op efter 10. forsøg
          App.langToast("Beklager, kan ikke spille radio");
          App.langToast("Prøv at vælge et andet format i indstillingerne");
        }
      } else {
        mediaPlayer.reset();
      }
      return true;
    }

    public void onBufferingUpdate(MediaPlayer mp, int procent) {
      if (App.fejlsøgning) Log.d("Afspiller onBufferingUpdate : " + procent + " " + mpTils());
      Log.d("Afspiller onBufferingUpdate : " + procent);
      if (procent < -100) procent = -1; // Ignorér vilde tal

      sendOnAfspilningForbinder(procent);
    }

    public void onSeekComplete(MediaPlayer mp) {
      Log.d("AfspillerService onSeekComplete");
      //opdaterObservatører();
    }
  }
}
