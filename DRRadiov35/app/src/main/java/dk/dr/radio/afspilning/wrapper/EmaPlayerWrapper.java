package dk.dr.radio.afspilning.wrapper;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;


import java.io.IOException;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * @author Jacob Nordfalk 28-11-14.
 *
public class EmaPlayerWrapper extends EMAudioPlayer implements  MediaPlayerWrapper {

  public EmaPlayerWrapper() {
    super(App.instans);
  }


  @Override
  public void setDataSource(final String url) throws IOException {
    super.setDataSource(App.instans, Uri.parse(url));
  }

  @Override
  public void setAudioStreamType(int streamMusic) {
    super.setAudioStreamType(streamMusic);
  }

  @Override
  public void prepare() throws IOException {
    super.prepareAsync();
  }

  @Override
  public void seekTo(int offsetMs) {
    super.seekTo(offsetMs);
  }

  @Override
  public long getDuration() {
    return super.getDuration();
  }

  @Override
  public long getCurrentPosition() {
    return super.getCurrentPosition();
  }

  @Override
  public void start() {
    super.start();
  }


  @Override
  public void stop() {
    super.stopPlayback();
  }

  @Override
  public void release() {
    super.release();
  }

  @Override
  public void reset() {
    super.reset();
  }

  @Override
  public boolean isPlaying() {
    return super.isPlaying();
  }

  @Override
  public void setVolume(float leftVolume, float rightVolume) {
    super.setVolume(leftVolume, rightVolume);
  }

  public void setWakeMode(Context context, int mode) {
    super.setWakeMode(context, mode);
  }

  @Override
  public void setMediaPlayerLytter(MediaPlayerLytter lytter) {
    setOnCompletionListener(lytter);
    setOnCompletionListener(lytter);
    setOnErrorListener(lytter);
    setOnPreparedListener(lytter);
    setOnBufferingUpdateListener(lytter);
    //setOnSeekCompleteListener(lytter);
  }
}
*/