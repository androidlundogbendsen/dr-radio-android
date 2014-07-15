package dk.dr.radio.afspilning;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.IOException;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Wrapper til MediaPlayer.
 * Akamai kræver at MediaPlayer'en bliver 'wrappet' sådan at deres statistikmodul
 * kommer ind imellem den og resten af programmet.
 * Dette muliggør også fjernafspilning (a la AirPlay), da f.eks.
 * ChromeCast-understøttelse kræver præcist den samme wrapping.
 * Oprettet af  Jacob Nordfalk 25-03-14.
 */
public class MediaPlayerWrapper {
  MediaPlayer mediaPlayer;

  public MediaPlayerWrapper(boolean opretMediaPlayer) {
    if (opretMediaPlayer) mediaPlayer = new MediaPlayer();
  }

  public MediaPlayerWrapper() {
    this(true);
  }

  public void setWakeMode(Context ctx, int screenDimWakeLock) {
    mediaPlayer.setWakeMode(ctx, screenDimWakeLock);
  }


  private static int tæller;

  public void setDataSource(String lydUrl) throws IOException {
    if (lydUrl.startsWith("file:")) {
      /* FIX: Det ser ud til at nogle telefonmodellers MediaPlayer har problemer
         med at åbne filer på SD-kortet hvis vi bare kalder setDataSource("file:///...
         Det er bl.a. set på:
         Telefonmodel: LT26i LT26i_1257-7813   - Android v4.1.2 (sdk: 16)
         Derfor bruger vi for en FileDescriptor i dette tilfælde
       */
      FileInputStream fis = new FileInputStream(Uri.parse(lydUrl).getPath());
      mediaPlayer.setDataSource(fis.getFD());
      fis.close();
      return;
    }

    mediaPlayer.setDataSource(lydUrl);
  }

  public void setAudioStreamType(int streamMusic) {
    mediaPlayer.setAudioStreamType(streamMusic);
  }

  public void prepare() throws IOException {
    mediaPlayer.prepare();
  }

  public void stop() {
    mediaPlayer.stop();
  }

  public void release() {
    mediaPlayer.release();
  }

  public void seekTo(int offsetMs) {
    mediaPlayer.seekTo(offsetMs);
  }

  public int getDuration() {
    return mediaPlayer.getDuration();
  }

  public int getCurrentPosition() {
    return mediaPlayer.getCurrentPosition();
  }

  public void start() {
    mediaPlayer.start();
  }

  public void reset() {
    mediaPlayer.reset();
  }

  public void setMediaPlayerLytter(MediaPlayerLytter lytter) {
    mediaPlayer.setOnCompletionListener(lytter);
    mediaPlayer.setOnCompletionListener(lytter);
    mediaPlayer.setOnErrorListener(lytter);
    mediaPlayer.setOnPreparedListener(lytter);
    mediaPlayer.setOnBufferingUpdateListener(lytter);
    mediaPlayer.setOnSeekCompleteListener(lytter);
  }


  private static Class<? extends MediaPlayerWrapper> mediaPlayerWrapperKlasse = null;

  public static MediaPlayerWrapper opret() {
    if (mediaPlayerWrapperKlasse == null) {
      if (!App.prefs.getBoolean("Rapportér statistik", true)) {
        App.langToast("DR Radio indsamler ikke brugsstatisik. Rapportér venligst om det gør en forskel for dig MHT batteriforbrug.");
        App.langToast("Hvis du er sikker på at det medfører væsentligt længere batterilevetid, så kontakt os, så vi kan kigge på problemet.");
        mediaPlayerWrapperKlasse = MediaPlayerWrapper.class;
      } else {
        try {
          mediaPlayerWrapperKlasse = (Class<? extends MediaPlayerWrapper>) Class.forName("dk.dr.radio.afspilning.AkamaiMediaPlayerWrapper");
        } catch (ClassNotFoundException e) {
          mediaPlayerWrapperKlasse = MediaPlayerWrapper.class;
          Log.e("Mangler Akamai-wrapper til statistik", e);
        }
      }
    }

    try {
      Log.d("MediaPlayerWrapper opret() " + mediaPlayerWrapperKlasse);
      return mediaPlayerWrapperKlasse.newInstance();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    return new MediaPlayerWrapper();
  }
}
