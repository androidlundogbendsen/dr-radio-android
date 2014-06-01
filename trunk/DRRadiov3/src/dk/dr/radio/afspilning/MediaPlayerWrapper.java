package dk.dr.radio.afspilning;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.GenericArrayType;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import dk.dr.radio.data.Diverse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
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

  public static String rensMasterM3U8ForDødeServere(String masterM3U3) {
    String[] lin = masterM3U3.split("[\r\n]");
    boolean fjernetNogen = false;
    boolean fjernetAlle = true;
    for (int n=1; n<lin.length; n++) {
      //Log.d("  "+n+" " + lin[n]);
      if (lin[n].startsWith("http")) try {
        URL u = new URL(lin[n]);
        InputStream is = u.openStream();
        is.read();
        is.close();
        // URL er OK, fortsæt
        fjernetAlle = false;
      } catch (Exception e) {
        Log.e(e);
        // Død URL - fjern den fra listen
        lin[n]=null;
        // Fjern også foregående
        lin[n-1]= null;
        fjernetNogen = true;
      }
      Log.d("  "+n+" " + lin[n]);
    }
    if (!fjernetNogen) return masterM3U3; // Ingen grund til at stykke den sammen igen
    if (fjernetAlle) return null; // Duer ikke!

    // Styk ny m3u8-fil sammen med servere der var oppe
    StringBuilder sb = new StringBuilder(masterM3U3.length());
    for (String l : lin) if (l!=null) sb.append(l).append("\n");

    return sb.toString();
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

    //if (!lydUrl.endsWith("master.m3u8") || App.PRODUKTION || tæller++%2==0) {
    if (!lydUrl.endsWith("master.m3u8") || !App.prefs.getBoolean("Filtrér akamai", true)) {
      Log.d("Bruger oprindelig lyd-URL: "+lydUrl);
      mediaPlayer.setDataSource(lydUrl);
      return;
    }

    Log.d("Gemmer "+lydUrl+" lokalt og spiller den");
    //App.kortToast("Lav venligt en rapport hvis det ikke virker, tak!!!");

    /* Følgende dur ikke, URLen skal starte med http:// og slutte med  .m3u8
       se http://androidxref.com/4.0.4/xref/frameworks/base/media/libmediaplayerservice/MediaPlayerService.cpp#583
      //File f = File.createTempFile("afspil_","master.m3u8",App.instans.getFilesDir());
      File f = new File(Environment.getExternalStorageDirectory(), "master.m3u8");
      Log.d(f + " <<-- "+lydUrl);
      InputStream is = new URL((lydUrl)).openStream();
      FilCache.kopierOgLuk(is, new FileOutputStream(f));
      FileInputStream fis = new FileInputStream(f);
      mediaPlayer.setDataSource(fis.getFD());
      fis.close();
      f.delete();
    */

    /*
HTTP/1.1 200 OK
Server: AkamaiGHost
Mime-Version: 1.0
Content-Type: application/vnd.apple.mpegurl
Content-Length: 596
Expires: Wed, 21 May 2014 14:58:46 GMT
Cache-Control: max-age=0, no-cache, no-store
Pragma: no-cache
Date: Wed, 21 May 2014 14:58:46 GMT
Connection: keep-alive
Set-Cookie: _alid_=WzFUnunxc/iaxEroCqOV3A==; path=/i/p1_9@143503/; domain=drradio1-lh.akamaihd.net

#EXTM3U
#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=64000,CODECS="mp4a.40.2"
http://drradio1-lh.akamaihd.net/i/p1_9@143503/index_64_a-p.m3u8?sd=10&rebase=on
#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=64000,CODECS="mp4a.40.2"
http://drradio1-lh.akamaihd.net/i/p1_9@143503/index_64_a-b.m3u8?sd=10&rebase=on
#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=192000,CODECS="mp4a.40.2"
http://drradio1-lh.akamaihd.net/i/p1_9@143503/index_192_a-p.m3u8?sd=10&rebase=on
#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=192000,CODECS="mp4a.40.2"
http://drradio1-lh.akamaihd.net/i/p1_9@143503/index_192_a-b.m3u8?sd=10&rebase=on
     */

    String masterM3U3 = Diverse.læsStreng(new URL((lydUrl)).openStream());
    final String rensetMasterM3U8 = rensMasterM3U8ForDødeServere(masterM3U3);
    if (rensetMasterM3U8==null) throw new IOException("Alle servere var døde");

    if (rensetMasterM3U8.equals(masterM3U3) && App.PRODUKTION) {
      mediaPlayer.setDataSource(lydUrl); // Alle servere er opppe, ingen grund til ikke at bruge den oprindelige URL
      return;
    }

    if (!rensetMasterM3U8.equals(masterM3U3)) {
      Log.rapporterFejl(new Exception("Nogle servere fjernet, hurra, det virker!"), lydUrl);
    }

    // Det følgende opretter en 'mini-webserver'
    final ServerSocket ss = new ServerSocket(0);
    //final CountDownLatch startSignal = new CountDownLatch(1);
    new Thread() {
      @Override
      public void run() {
        try {
          Log.d("Venter på socket "+ss);
          //startSignal.countDown(); // og.... NU må MediaPlayer godt spørge på URLen
          Socket s = ss.accept();
          Log.d("VI FIK EN socket "+s);
          //int n = s.getInputStream().read(new byte[100]);
          //Log.d("Læste " + n + " byte. Sender " + bos);
          BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
          String l = br.readLine();
          while (l!=null && l.length()>0) {
            Log.d("Socket: "+l);
            l = br.readLine();
          }
          //Log.d("Sender " + rensetMasterM3U8.replace('\n', '/'));
          Log.d("Sender " + rensetMasterM3U8);
          byte[] ba = rensetMasterM3U8.getBytes();
          OutputStream os = s.getOutputStream();
          os.write(("HTTP/1.1 200 OK\n" +
              "Content-Type: application/vnd.apple.mpegurl\n" +
              "Content-Length: "+ba.length+"\n" +
              "Connection: close\n" +
              "Cache-Control: max-age=0, no-cache, no-store\n" +
              "Pragma: no-cache\n" +
              "\n").getBytes());
          os.write(ba);
          ss.close();
        } catch (IOException e) {
          Log.rapporterFejl(e);
        }
      }
    }.start();

    //try {
    //  startSignal.await(1, TimeUnit.SECONDS); // vent på at MediaPlayer må spørge på URLen
    //} catch (InterruptedException e) {
    //  Log.rapporterFejl(e);
    //}
    // ... og sæt i gang
    Log.d("mediaPlayer.setDataSource(http://localhost:"+ss.getLocalPort()+"/master.m3u8");
    mediaPlayer.setDataSource("http://localhost:"+ss.getLocalPort()+"/master.m3u8");

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
    if (mediaPlayerWrapperKlasse == null) {
      try {
        mediaPlayerWrapperKlasse = (Class<? extends MediaPlayerWrapper>) Class.forName("dk.dr.radio.afspilning.AkamaiMediaPlayerWrapper");
      } catch (ClassNotFoundException e) {
        mediaPlayerWrapperKlasse = MediaPlayerWrapper.class;
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
