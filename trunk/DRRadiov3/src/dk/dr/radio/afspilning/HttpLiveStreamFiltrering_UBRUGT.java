package dk.dr.radio.afspilning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import dk.dr.radio.data.Diverse;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 06-06-14.
 */
public class HttpLiveStreamFiltrering_UBRUGT {
  public static final HttpLiveStreamFiltrering_UBRUGT instans = new HttpLiveStreamFiltrering_UBRUGT();
  private String rensetMasterM3U8;
  private ServerSocket ss;
  private boolean fjernetNogen;
  private boolean fjernetAlle;


  public void setMasterM3U8Url(String lydUrl) throws IOException {
    String masterM3U3 = Diverse.læsStreng(new URL(lydUrl).openStream());
    String[] lin = masterM3U3.split("[\r\n]");
    fjernetNogen = false;
    fjernetAlle = true;
    for (int n = 1; n < lin.length; n++) {
      //Log.d("  "+n+" " + lin[n]);
      if (lin[n].startsWith("http")) try {
        URL u = new URL(lin[n]);
        InputStream is = u.openStream();
        is.read();
        is.close();
        // URL er OK, fortsæt
        fjernetAlle = false;
      } catch (Exception e) {
        Log.e("Fjerner DØD URL: " + lin[n], e);
        // Død URL - fjern den fra listen
        lin[n] = null;
        // Fjern også foregående
        lin[n - 1] = null;
        fjernetNogen = true;
      }
      Log.d("  " + n + " " + lin[n]);
    }

    if (fjernetAlle) throw new IOException("Alle servere var døde");

    if (!fjernetNogen) {
      // Ingen grund til at stykke den sammen igen
      rensetMasterM3U8 = masterM3U3;
    } else {
      // Styk ny m3u8-fil sammen med servere der var oppe
      StringBuilder sb = new StringBuilder(masterM3U3.length());
      for (String l : lin) if (l != null) sb.append(l).append("\n");
      rensetMasterM3U8 = sb.toString();
    }
  }


  public String getRensetMasterM3U8Url() throws IOException {
    // Det følgende opretter en 'mini-webserver'
    if (ss == null) {
      ss = new ServerSocket(0);
      startTråd();
    }

    return "http://localhost:" + ss.getLocalPort() + "/master.m3u8";
  }

  private void startTråd() throws IOException {
    new Thread() {
      @Override
      public void run() {
        while (true)
          try {
            Log.d("Venter på socket " + ss);
            //startSignal.countDown(); // og.... NU må MediaPlayer godt spørge på URLen
            Socket s = ss.accept();
            Log.d("VI FIK EN socket " + s);
            //int n = s.getInputStream().read(new byte[100]);
            //Log.d("Læste " + n + " byte. Sender " + bos);
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String l = br.readLine();
            while (l != null && l.length() > 0) {
              Log.d("Socket: " + l);
              l = br.readLine();
            }
            //Log.d("Sender " + rensetMasterM3U8.replace('\n', '/'));
            Log.d("Sender rensetMasterM3U8");
            byte[] ba = rensetMasterM3U8.getBytes();
            OutputStream os = s.getOutputStream();
            os.write(("HTTP/1.1 200 OK\n" +
                "Content-Type: application/vnd.apple.mpegurl\n" +
                "Content-Length: " + ba.length + "\n" +
                "Connection: close\n" +
                "Cache-Control: max-age=0, no-cache, no-store\n" +
                "Pragma: no-cache\n" +
                "\n").getBytes());
            os.write(ba);
          } catch (Exception e) {
            Log.rapporterFejl(e);
          }
        // Skal aldrig lukkes:
        // ss.close();
      }
    }.start();
  }

  public String getRensetMasterM3U8() {
    return rensetMasterM3U8;
  }

  public boolean fjernedeNogen() {
    return fjernetNogen;
  }
}


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

    /*
    //if (!lydUrl.endsWith("master.m3u8") || App.PRODUKTION || tæller++%2==0) {
    if (!lydUrl.endsWith("master.m3u8") || !App.prefs.getBoolean("Filtrér akamai", true)) {
      Log.d("Bruger oprindelig lyd-URL: "+lydUrl);
      mediaPlayer.setDataSource(lydUrl);
      return;
    }

    Log.d("Gemmer "+lydUrl+" lokalt og spiller den");

    HttpLiveStreamFiltrering filter = HttpLiveStreamFiltrering.instans;
    filter.setMasterM3U8Url(lydUrl);

    String rensetMasterM3U8 = filter.getRensetMasterM3U8();

    if (App.PRODUKTION && !filter.fjernedeNogen()) {
      mediaPlayer.setDataSource(lydUrl); // Alle servere er opppe, ingen grund til ikke at bruge den oprindelige URL
      return;
    }

    if (filter.fjernedeNogen()) {
      Log.rapporterFejl(new Exception("Nogle servere fjernet, hurra, det virker!"), lydUrl);
    }

    String rensetMasterM3U8Url = filter.getRensetMasterM3U8Url();

    Log.d("mediaPlayer.setDataSource("+rensetMasterM3U8Url);
    mediaPlayer.setDataSource(rensetMasterM3U8Url);
    */
