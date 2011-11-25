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

package dk.dr.radio.util;

import java.io.InputStreamReader;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.Settings.Secure;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author j
 */
public class MedieafspillerInfo {

  /*
    // The video is too complex for the decoder: it can't decode frames fast
    // enough. Possibly only the audio plays fine at this stage.
    MEDIA_INFO_VIDEO_TRACK_LAGGING = 700,
    // MediaPlayer is temporarily pausing playback internally in order to
    // buffer more data.
    MEDIA_INFO_BUFFERING_START = 701,
    // MediaPlayer is resuming playback after filling buffers.
    MEDIA_INFO_BUFFERING_END = 702,
    // 8xx
    // Bad interleaving means that a media has been improperly interleaved or not
    // interleaved at all, e.g has all the video samples first then all the audio
    // ones. Video is playing but a lot of disk seek may be happening.
    MEDIA_INFO_BAD_INTERLEAVING = 800,
    // The media is not seekable (e.g live stream).
    MEDIA_INFO_NOT_SEEKABLE = 801,
    // New media metadata is available.
    MEDIA_INFO_METADATA_UPDATE = 802,

    MEDIA_ERROR_SERVER_DIED = 100,
    // 2xx
    MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200,
  */
  public static String infokodeTilStreng(int hvad) {
    if (hvad == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) return "VIDEO_TRACK_LAGGING";
    //if (hvad == MediaPlayer.MEDIA_INFO_BUFFERING_START) return "BUFFERING_START";			// fjernet, da det er android 2.3+
    //if (hvad == MediaPlayer.MEDIA_INFO_BUFFERING_END) return "BUFFERING_END";				// fjernet, da det er android 2.3+
    if (hvad == MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING) return "BAD_INTERLEAVING";
    if (hvad == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) return "NOT_SEEKABLE";
    if (hvad == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) return "METADATA_UPDATE";
    if (hvad == MediaPlayer.MEDIA_INFO_UNKNOWN) return "UNKNOWN";
    return "(ukendt)";
  }

  public static String fejlkodeTilStreng(int hvad) {
    if (hvad == MediaPlayer.MEDIA_ERROR_SERVER_DIED) return "SERVER_DIED";
    if (hvad == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) return "NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";
    if (hvad == MediaPlayer.MEDIA_ERROR_UNKNOWN) return "UNKNOWN";
    return "(ukendt)";
  }

  public String build_prop_stagefright;

  public String lavTelefoninfo(Activity a) {
    String ret = "build.prop: ";
    try {
      BufferedReader br = new BufferedReader(new FileReader("/system/build.prop"));
      String l;
      build_prop_stagefright = "";
      while ((l=br.readLine())!=null) if (l.contains("stagefright")) build_prop_stagefright+=l+" ";
      ret += build_prop_stagefright;
    } catch (Exception ex) {
      ex.printStackTrace();
      ret += ex;
    }

    PackageManager pm = a.getPackageManager();
    String version="(ukendt)";
    try {
      PackageInfo pi=pm.getPackageInfo(a.getPackageName(), 0);
      version=pi.versionName;
    } catch (Exception e) {
      version=e.toString();
      e.printStackTrace();
    }

    ret+="\nProgram: "+a.getPackageName()+" version "+version
        +"\nTelefonmodel: "+Build.MODEL +" "+Build.PRODUCT
        +"\nAndroid v"+Build.VERSION.RELEASE
        +"\nsdk: "+Build.VERSION.SDK
        +"\nMedieafspiller: "+findMpUserAgent(a)
        +"\nAndroid_ID: "+Secure.getString(a.getContentResolver(), Secure.ANDROID_ID);



    // http://stackoverflow.com/questions/4617138/detect-if-flash-is-installed-on-android-and-embed-a-flash-video-in-an-activity
    // http://stackoverflow.com/questions/4458930/how-to-check-if-flash-is-installed
    PackageInfo pi = getPackageInfo(pm, "com.adobe.flashplayer");
    if (pi == null) pi = getPackageInfo(pm, "com.htc.flash");
    if (pi != null) ret +="\nFlash: "+pi.packageName+" v. "+pi.versionName;
    else ret +="\nFlash: Nej";
    return ret;
  }

  private PackageInfo getPackageInfo(PackageManager pm, String pakke) {
    try {
      PackageInfo pi = pm.getPackageInfo(pakke, 0);
      return pi;
    } catch (NameNotFoundException ex) {
      return null;
    }
  }


  public volatile String mpUserAgent = null;

  private String findMpUserAgent(Activity a) {
    if (mpUserAgent!=null) return mpUserAgent;
    MediaPlayer mp = new MediaPlayer();
    try {
      final ServerSocket serverSocket = new ServerSocket(12234);
      int socketPort = serverSocket.getLocalPort();

      Thread t = new Thread() {
        public void run() {
          try {
            Socket socket = serverSocket.accept();
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            mpUserAgent = "(ukendt)";

            String s;
            while ((s = br.readLine())!=null) {
              Log.d(s);
              if (s.startsWith("User-Agent:")) mpUserAgent = s.substring(11);
              //if (s.trim().isEmpty()) break;		// virker ikke på gamle androids
              if (s.trim().length()==0) break;
            }
            socket.close();
            serverSocket.close();
          } catch (Exception ex) {
            ex.printStackTrace();
            Log.d("serverSocket "+ex);
          }
        }
      };
      t.start();

      mp.setDataSource(String.format("http://127.0.0.1:%d/", socketPort));
      mp.prepareAsync();
      mp.start();
      t.join(200);
      if (t.isAlive()) t.interrupt();
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      mp.release();
    }

    Log.d("Medieafspiller UserAgent: "+mpUserAgent);
    return mpUserAgent;
  }



// http://stackoverflow.com/questions/4579885/determine-opencore-or-stagefright-framework-for-mediaplayer


  public String findBedsteMusikformat(Activity a) {
    // For Android 1.5-kompetibilitet bruger vi ikke Build.VERSION.SDK
    int sdk = Integer.parseInt(Build.VERSION.SDK);

    String ua = mpUserAgent;
    if (ua==null) ua = "ukendt/0.0 (Linux;Android 2.2)";

    // Emulator: stagefright/1.0 (Linux;Android 2.2)
    // Galaxy S: CORE/6.506.4.1 OpenCORE/2.02 (Linux;Android 2.2)

    // media.stagefright.enable-player=true media.stagefright.enable-meta=true media.stagefright.enable-scan=true media.stagefright.enable-http=true
    String bp = build_prop_stagefright;
    if (bp==null) bp = "";

    boolean ice, rtsp, httplive;

    ice = sdk < 8
       || sdk == 8 && !mpUserAgent.toLowerCase().contains("");
    rtsp = sdk > 9;
    httplive = sdk > 9;



    return "rtsp";
  }



/*
    try {
      Log.d("Prøver httplive.mp3");
      AssetFileDescriptor afd = a.getAssets().openFd("httplive.mp3");
      mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
      mp.prepare();
      mp.start();
      Log.d("Spiller httplive.mp3");
      Thread.sleep(5000);
    } catch (Exception ex) {
      ex.printStackTrace();
      Log.d(ex);
    } finally {
      mp.reset();
    }

    try {
      Log.d("Prøver channel5.mp3");
      AssetFileDescriptor afd = a.getAssets().openFd("channel5.mp3");
      mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
      mp.prepare();
      mp.start();
      Log.d("Spiller ...");
      Thread.sleep(5000);
    } catch (Exception ex) {
      ex.printStackTrace();
      Log.d(ex);
    } finally {
      mp.reset();
    }

    try {
      Log.d("Prøver channel5.ts");
      AssetFileDescriptor afd = a.getAssets().openFd("channel5.ts");
      mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
      mp.prepare();
      mp.start();
      Log.d("Spiller ...");
      Thread.sleep(5000);
    } catch (Exception ex) {
      ex.printStackTrace();
      Log.d(ex);
    } finally {
      mp.reset();
    }
*/
//    mp.setDataSource(ProxyPlayerActivity.instance.getAssets().openNonAssetFd("httplive.mp3").getFileDescriptor());
}
