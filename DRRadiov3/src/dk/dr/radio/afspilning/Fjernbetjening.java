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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.Build;
import android.view.KeyEvent;

import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;

import dk.dr.radio.akt.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Til håndtering af knapper på fjernbetjening (f.eks. på Bluetooth headset.)
 * Se også http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
 */
public class Fjernbetjening extends BroadcastReceiver {

  private static RemoteControlClient remoteControlClient;
  private static Udsendelse forrigeUdsendelse;

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  public static void opdaterBillede(Afspiller afspiller) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;

    Lydkilde lk = afspiller.getLydkilde();
    Kanal k = lk.getKanal();
    Udsendelse u = lk.getUdsendelse();
    Log.d("Fjernbetjening opdaterBillede " + lk + " k=" + k + " u=" + u + " d=" + lk.erDirekte());
    if (lk.erDirekte()) {
      /*
      Bitmap bm = null;
      // Undgå at afkode billedet hver gang:
      // bm = BitmapFactory.decodeResource(App.instans.getResources(), k.kanallogo_resid);
      // i stedet henter vi den som en BitmapDrawable, den er nemlig cachet
      Drawable dr = App.instans.getResources().getDrawable(k.kanallogo_resid);
      if (dr instanceof BitmapDrawable) {
        bm = ((BitmapDrawable) dr).getBitmap();
      }
          .putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, bm)
      */
      remoteControlClient.editMetadata(false)
          .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, u == null ? null : u.titel)
          .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "DR " + k.navn + " Direkte")
//          .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, )
          .apply();
    } else {
      remoteControlClient.editMetadata(false)
          .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, u.titel)
          .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, k==null?null:"DR " + k.navn)
          .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, u.playliste == null || u.playliste.size() == 0 ? "" : u.playliste.get(0).kunstner)
          .apply();
    }

    if (u!=null && u!=forrigeUdsendelse) {
      // Skift baggrundsbillede
      forrigeUdsendelse = u;
      final String burl = Basisfragment.skalérSlugBilledeUrl(u.slug, 800, 400);
      App.kortToast("asynk artwork\n" + burl);

      App.volleyRequestQueue.add(new ImageRequest(burl,
          new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bm) {
              App.kortToast("asynk artwork " + bm.getHeight() + "\n" + burl);
              remoteControlClient.editMetadata(false)
                  .putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, bm)
                  .apply();
            }
          }, 0, 0, null, null));
    }

    Status s = afspiller.getAfspillerstatus();
    int ps = s == Status.STOPPET ? RemoteControlClient.PLAYSTATE_STOPPED : s == Status.SPILLER ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_BUFFERING;

    //if (Build.VERSION.SDK_INT >= 18)
    //  remoteControlClient.setPlaybackState(ps, 0, 1);
    //else
    remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
  }


  @Override
  public void onReceive(Context context, Intent intent) {
    KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
    Log.d("MediabuttonReciever " + event);

    if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()) || event == null || event.getAction() != KeyEvent.ACTION_DOWN) {
      return;
    }

    switch (event.getKeyCode()) {
      case KeyEvent.KEYCODE_HEADSETHOOK:
      case KeyEvent.KEYCODE_MEDIA_STOP:
      case KeyEvent.KEYCODE_MEDIA_PAUSE:
        if (DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET) {
          DRData.instans.afspiller.stopAfspilning();
        }
        break;
      case KeyEvent.KEYCODE_MEDIA_PLAY:
      case KeyEvent.KEYCODE_MEDIA_REWIND:
      case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
        if (DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
          DRData.instans.afspiller.startAfspilning();
        }
        break;
      case KeyEvent.KEYCODE_MEDIA_NEXT:
        App.kortToast("næste");
//        DRData.instans.afspiller.forrige();
        break;
      case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        App.kortToast("forrige");
//        DRData.instans.afspiller.næste();
        break;
      case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
      default:
        if (DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
          DRData.instans.afspiller.startAfspilning();
        } else {
          DRData.instans.afspiller.stopAfspilning();
        }
    }
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public static void registrér() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) return;

    ComponentName eventReceiver = new ComponentName(App.instans.getPackageName(), Fjernbetjening.class.getName());
    AudioManager audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
    audioManager.registerMediaButtonEventReceiver(eventReceiver);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;

    Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(eventReceiver);
    PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(App.instans, 0, mediaButtonIntent, 0);
    // create and register the remote control client
    remoteControlClient = new RemoteControlClient(mediaPendingIntent);
    remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
            | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
            | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
            | RemoteControlClient.FLAG_KEY_MEDIA_STOP
            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
    );
    audioManager.registerRemoteControlClient(remoteControlClient);
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public static void afregistrér() {
    if (Build.VERSION.SDK_INT < 8) return;
    ComponentName eventReceiver = new ComponentName(App.instans.getPackageName(), Fjernbetjening.class.getName());
    AudioManager audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
    audioManager.unregisterMediaButtonEventReceiver(eventReceiver);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) return;
    remoteControlClient.editMetadata(true).apply();
    remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
    audioManager.unregisterRemoteControlClient(remoteControlClient);
  }
}