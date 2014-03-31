package dk.dr.radio.afspilning;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

import dk.dr.radio.diverse.Log;

/**
 * Created by j on 25-03-14.
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

  public void setDataSource(String lydUrl) throws IOException {
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

  public void seekTo(int progress) {
    mediaPlayer.seekTo(progress);
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
    if (mediaPlayerWrapperKlasse==null) {
      try {
        mediaPlayerWrapperKlasse = (Class<? extends MediaPlayerWrapper>) Class.forName("dk.dr.radio.afspilning.AkamaiMediaPlayerWrapper");
      } catch (ClassNotFoundException e) {
        mediaPlayerWrapperKlasse = MediaPlayerWrapper.class;
      }
    }

    try {
      Log.d("MediaPlayerWrapper opret() "+mediaPlayerWrapperKlasse);
      MediaPlayerWrapper ret = mediaPlayerWrapperKlasse.newInstance();
      return ret;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    return new MediaPlayerWrapper();
  }
}
