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

package dk.dr.radio.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Rapportering;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class DRData {

  public static DRData instans;
  public static final String STAMDATA_URL = "http://javabog.dk/privat/stamdata_android_v3_013_01.json";

  public Stamdata stamdata;
  public Afspiller afspiller;
  public Kanal aktuelKanal;

  public final Rapportering rapportering = new Rapportering();

  /**
   * Til afprøvning
   */
  public static void main(String[] a) throws Exception {
    DRData i = new DRData();
    FilCache.init(new File("/tmp/drradio-cache"));
    DRJson.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard Java

//    i.stamdata = Stamdata.xxx_parseStamdatafil(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
//    i.stamdata.skrald_parseAlleKanaler(Diverse.læsStreng(new FileInputStream("res/raw/skrald__alle_kanaler.json")));


    i.stamdata = Stamdata.parseAndroidStamdata(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
    i.stamdata.parseFællesStamdata(Diverse.læsStreng(new FileInputStream("res/raw/stamdata2_faelles.json")));
    i.stamdata.hentSupplerendeDataBg();

/*
    for (Kanal k : i.stamdata.kanaler) {
      if (k.p4underkanal) continue;
      Log.d("\n\nkanal = " + k);
      String f = FilCache.hentFil(k.logoUrl, true, true, 1000 * 60 * 60 * 24 * 7);
      new File(f).renameTo(new File("/tmp/drawable-hdpi/kanalappendix_" + k.kode.toLowerCase() + ".png"));
      FilCache.hentFil(k.logoUrl2, true, true, 1000 * 60 * 60 * 24 * 7);
    }
*/
    for (Kanal kanal : i.stamdata.kanaler) {
      Log.d("\n\nkanal = " + kanal);
      kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(hent(kanal.getUdsendelserUrl()))), "0");
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        JSONObject obj = new JSONObject(hent(u.getStreamsUrl()));
        //Log.d(obj.toString(2));
        u.streams = DRJson.parsStreams(obj.getJSONArray(DRJson.Streams.name()));
        if (u.streams.size() == 0) Log.d("Ingen lydstreams");

        u.playliste = DRJson.parsePlayliste(new JSONArray(hent(kanal.getPlaylisteUrl(u))));
        Log.d("u.playliste= " + u.playliste);
      }
      break;
    }

  }

  private static String hent(String url) throws IOException {
    //String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
    String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60)));
    //Log.d(data);
    return data;

  }

}
