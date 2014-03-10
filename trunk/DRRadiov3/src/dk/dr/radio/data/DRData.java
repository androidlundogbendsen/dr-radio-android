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
import java.util.HashMap;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Rapportering;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class DRData {

  public static DRData instans;
  public static final String GRUNDDATA_URL = "http://javabog.dk/privat/drradiov3_grunddata.json";

  public Grunddata grunddata;
  public Afspiller afspiller;
  public Kanal aktuelKanal;

  public HashMap<String, Udsendelse> udsendelseFraSlug = new HashMap<String, Udsendelse>();
  public HashMap<String, Programserie> programserieFraSlug = new HashMap<String, Programserie>();

  public Rapportering rapportering = new Rapportering();
  public SenestLyttede senestLyttede = new SenestLyttede();
  public Favoritter favoritter = new Favoritter();

  /**
   * Til afprøvning
   */
  public static void main(String[] a) throws Exception {
    DRData i = DRData.instans = new DRData();
    FilCache.init(new File("/tmp/drradio-cache"));
    DRJson.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard Java

//    i.grunddata = Stamdata.xxx_parseStamdatafil(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
//    i.grunddata.skrald_parseAlleKanaler(Diverse.læsStreng(new FileInputStream("res/raw/skrald__alle_kanaler.json")));
//    i.grunddata = Stamdata.parseAndroidStamdata(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
    i.grunddata = new Grunddata();
    i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("res/raw/grunddata.json")));
    i.grunddata.hentSupplerendeDataBg();

/*
    for (Kanal k : i.grunddata.kanaler) {
      if (k.p4underkanal) continue;
      Log.d("\n\nkanal = " + k);
      String f = FilCache.hentFil(k.logoUrl, true, true, 1000 * 60 * 60 * 24 * 7);
      new File(f).renameTo(new File("/tmp/drawable-hdpi/kanalappendix_" + k.kode.toLowerCase() + ".png"));
      FilCache.hentFil(k.logoUrl2, true, true, 1000 * 60 * 60 * 24 * 7);
    }
*/
    for (Kanal kanal : i.grunddata.kanaler) {
      Log.d("\n\n===========================================\n\nkanal = " + kanal);
      if (Kanal.P4kode.equals(kanal.kode)) continue;
      kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(hent(kanal.getUdsendelserUrl())), kanal, DRData.instans), "0");
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        JSONObject obj = new JSONObject(hent(u.getStreamsUrl()));
        //Log.d(obj.toString(2));
        u.streams = DRJson.parsStreams(obj.getJSONArray(DRJson.Streams.name()));
        if (u.streams.size() == 0) Log.d("Ingen lydstreams");
        if (u.streams.size() == 0 && u.kanHøres) throw new IllegalStateException();
        if (u.streams.size() > 0 && !u.kanHøres) throw new IllegalStateException();

        try {
          u.playliste = DRJson.parsePlayliste(new JSONArray(hent(kanal.getPlaylisteUrl(u))));
          Log.d("u.playliste= " + u.playliste);
        } catch (IOException e) {
          e.printStackTrace();
        }


        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) {
          String str = hent(u.getProgramserieUrl());
          if ("null".equals(str)) continue;
          JSONObject data = new JSONObject(str);
          ps = DRJson.parsProgramserie(data);
          ps.udsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
          i.programserieFraSlug.put(u.programserieSlug, ps);

        }
      }
      //break;
    }
  }

  private static String hent(String url) throws IOException {
    //String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
    Log.d(url);
    url = url.replaceAll("Ø", "%C3%98");
    url = url.replaceAll("Å", "%C3%85");
    String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60)));
    Log.d(data);
    return data;

  }

}
