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

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import dk.dr.radio.afspilning.AfspillerStartStopReciever;
import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.DRData;

/**
 * Til håndtering af knapper på fjernbetjening (f.eks. på Bluetooth headset.)
 * Se også http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
 */
public class MediabuttonReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
    Log.d("MediabuttonReciever " + intent+" "+event);

    if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()) || event==null || event.getAction()!=KeyEvent.ACTION_DOWN) {
      return;
    }

    switch (event.getKeyCode()) {
      case KeyEvent.KEYCODE_HEADSETHOOK:
      case KeyEvent.KEYCODE_MEDIA_STOP:
      case KeyEvent.KEYCODE_MEDIA_PAUSE:
        if (DRData.instans.afspiller.getAfspillerstatus()!= Status.STOPPET) {
          DRData.instans.afspiller.stopAfspilning();
        }
        break;
      case KeyEvent.KEYCODE_MEDIA_PLAY:
      case KeyEvent.KEYCODE_MEDIA_NEXT:
      case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
      case KeyEvent.KEYCODE_MEDIA_REWIND:
      case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
        if (DRData.instans.afspiller.getAfspillerstatus()== Status.STOPPET) {
          DRData.instans.afspiller.startAfspilning();
        }
        break;
      case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
      default:
        if (DRData.instans.afspiller.getAfspillerstatus()== Status.STOPPET) {
          DRData.instans.afspiller.stopAfspilning();
        } else {
          DRData.instans.afspiller.startAfspilning();
        }
    }
  }

  @TargetApi(Build.VERSION_CODES.FROYO)
  public static void registrér() {
    if (Build.VERSION.SDK_INT < 8) return;
//    if (!App.prefs.getBoolean("MediabuttonReceiver", true)) return;

    ComponentName eventReceiver = new ComponentName(App.instans.getPackageName(), MediabuttonReceiver.class.getName());
    AudioManager audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
    audioManager.registerMediaButtonEventReceiver(eventReceiver);
  }

  @TargetApi(Build.VERSION_CODES.FROYO)
  public static void afregistrér() {
    if (Build.VERSION.SDK_INT < 8) return;
    ComponentName eventReceiver = new ComponentName(App.instans.getPackageName(), MediabuttonReceiver.class.getName());
    AudioManager audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
    audioManager.unregisterMediaButtonEventReceiver(eventReceiver);
  }
}