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
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Rapportering;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class DRData {

  public static DRData instans;

  // scp /home/j/android/dr-radio-android/DRRadiov3/res/raw/grunddata_udvikling.json j:../lundogbendsen/hjemmeside/drradiov3_grunddata.json

  public static final String GRUNDDATA_URL =  App.PRODUKTION
      ? "http://www.dr.dk/tjenester/iphone/radio/settings/iphone200d.drxml"
      : "http://android.lundogbendsen.dk/drradiov3_grunddata.json";
  //public static final String GRUNDDATA_URL = "http://www.dr.dk/tjenester/iphone/radio/settings/iphone200d.json";

  public Grunddata grunddata;
  public Afspiller afspiller;

  public HashMap<String, Udsendelse> udsendelseFraSlug = new HashMap<String, Udsendelse>();
  public HashMap<String, Programserie> programserieFraSlug = new HashMap<String, Programserie>();

  public Rapportering rapportering = new Rapportering();
  public SenestLyttede senestLyttede = new SenestLyttede();
  public Favoritter favoritter = new Favoritter();
  public HentedeUdsendelser hentedeUdsendelser = new HentedeUdsendelser();  // Understøttes ikke på Android 2.2

  /**
   * Til afprøvning
   */

  // VM Options
  // -classpath $PROJECT_DIR$/../../dr-netradio/trunk/JSONParsning/lib/json-1.0.jar:$PROJECT_DIR$/out/production/DRRadiov3:$APPLICATION_HOME_DIR$/lib/idea_rt.jar:$PROJECT_DIR$/../../android-sdk-linux_86/platforms/android-18/android.jar:$PROJECT_DIR$/libs/android-support-v7-appcompat.jar:$PROJECT_DIR$/libs/android-support-v4.jar:$PROJECT_DIR$/libs/bugsense-3.6.jar:$PROJECT_DIR$/libs/volley.jar

  public static void main_x(String[] a) throws Exception {
    DRData i = DRData.instans = new DRData();
    FilCache.init(new File("/tmp/drradio-cache"));
    DRJson.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard Java

//    i.grunddata = Stamdata.xxx_parseStamdatafil(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
//    i.grunddata.skrald_parseAlleKanaler(Diverse.læsStreng(new FileInputStream("res/raw/skrald__alle_kanaler.json")));
//    i.grunddata = Stamdata.parseAndroidStamdata(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
    i.grunddata = new Grunddata();
    //i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("../DRRadiov3/res/raw/grunddata.json")));
    //i.grunddata.hentSupplerendeDataBg_KUN_TIL_UDVIKLING();

    i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("../DRRadiov3/res/raw/grunddata_testaendring.json")));
    //if (!i.grunddata.kanalFraKode.get("DRN").navn.equals("DR NyhederÆNDRET")) throw new InternalError("xx1");
    //if (i.grunddata.kanaler.size()>11) throw new InternalError("i.grunddata.kanaler.size()="+i.grunddata.kanaler.size());


    if (i.grunddata.udelukHLS==true) throw new Exception();
    i.grunddata.setUdelukHLS("C6603 C6603/18"); if (i.grunddata.udelukHLS!=true) throw new Exception();
    i.grunddata.setUdelukHLS("C6603 C6603/17"); if (i.grunddata.udelukHLS==true) throw new Exception();
    i.grunddata.setUdelukHLS("IdeaPadA10 A10/17"); if (i.grunddata.udelukHLS!=true) throw new Exception();
    i.grunddata.setUdelukHLS("IdeaPadA10 A10/23"); if (i.grunddata.udelukHLS==true) throw new Exception();
    i.grunddata.setUdelukHLS("IdeaPadA10 A11/17"); if (i.grunddata.udelukHLS==true) throw new Exception();

    i.grunddata.hentSupplerendeDataBg_KUN_TIL_UDVIKLING();

    for (Kanal kanal : i.grunddata.kanaler) {
      Log.d("\n\n===========================================\n\nkanal = " + kanal);
      if (Kanal.P4kode.equals(kanal.kode)) continue;

      for (Lydstream ls : kanal.streams) {
        //Log.d("\nLydstream = " + ls);
        if (!ls.url.endsWith("master.m3u8")) continue;
        Log.d("Lydstream = " + ls);

        String lokalM3U8indhold = main_hent(ls.url);
        /*
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
        String[] lin = lokalM3U8indhold.split("[\r\n]");
        ArrayList<String> rensetListe = new ArrayList<String>(lin.length);
        for (int n=0; n<lin.length; n++) {
          //Log.d("  "+n+" " + lin[n]);
          if (lin[n].startsWith("http")) try {
            URL u = new URL(lin[n]);
            InputStream is = u.openStream();
            is.read();
            is.close();
            // URL er OK, fortsæt
          } catch (Exception e) {
            Log.e(e);
            // Død URL - fjern den fra listen
            lin[n]=null;
            // Fjern også foregående
            lin[n-1]= null;
          }
          Log.d("  "+n+" " + lin[n]);
        }

//        String renset = M3U8parser.rensForDødeServere(indhold);
      }

      System.exit(0);

      if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder
      kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(main_hent(kanal.getUdsendelserUrl())), kanal, DRData.instans), "0");
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        JSONObject obj = new JSONObject(main_hent(u.getStreamsUrl()));
        //Log.d(obj.toString(2));
        u.streams = DRJson.parsStreams(obj.getJSONArray(DRJson.Streams.name()));
        if (u.streams.size() == 0) Log.d("Ingen lydstreams");
        //if (u.streams.size() == 0 && u.kanNokHøres) throw new IllegalStateException("u.streams.size() == 0 && u.kanNokHøres");
        //if (u.streams.size() > 0 && !u.kanNokHøres) throw new IllegalStateException("u.streams.size() > 0 && !u.kanNokHøres");

        try {
          u.playliste = DRJson.parsePlayliste(new JSONArray(main_hent(u.getPlaylisteUrl())));
          Log.d("u.playliste= " + u.playliste);
        } catch (IOException e) {
          e.printStackTrace();
        }


        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) {
          String str = main_hent(u.getProgramserieUrl());
          if ("null".equals(str)) continue;
          JSONObject data = new JSONObject(str);
          ps = DRJson.parsProgramserie(data, null);
          JSONArray prg = data.getJSONArray(DRJson.Programs.name());
          ArrayList<Udsendelse> udsendelser = DRJson.parseUdsendelserForProgramserie(prg, kanal, DRData.instans);
          ps.tilføjUdsendelser(0, udsendelser);
          i.programserieFraSlug.put(u.programserieSlug, ps);

        }
      }
      //break;
    }
  }

  private static String main_hent(String url) throws IOException {
    //String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
    Log.d(url);
    url = url.replaceAll("Ø", "%C3%98");
    url = url.replaceAll("Å", "%C3%85");
    String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60)));
    Log.d(data);
    return data;

  }


  public static void main(String[] a) throws Exception {
    DRData i = DRData.instans = new DRData();
    FilCache.init(new File("/tmp/drradio-cache"));
    DRJson.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard Java
    i.grunddata = new Grunddata();
    i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("../DRRadiov3/res/raw/grunddata.json")));
    i.grunddata.hentSupplerendeDataBg_KUN_TIL_UDVIKLING();

    // Virker ikke, giv
    //JSONArray jsonArray = new JSONArray(main_hent("http://www.dr.dk/tjenester/mu-apps/radio-drama?type=radio&includePrograms=true"));
    JSONArray jsonArray = new JSONArray(main_hent("http://www.dr.dk/tjenester/mu-apps/radio-drama"));
    ArrayList<Programserie> res = new ArrayList<Programserie>();
    for (int n=0; n<jsonArray.length(); n++) {
      JSONObject programserieJson = jsonArray.getJSONObject(n);
      String programserieSlug = programserieJson.getString(DRJson.Slug.name());
      Log.d("\n=========================================== programserieSlug = " + programserieSlug);

      Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
      if (programserie==null) {
        programserie = new Programserie();
        DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
      }
      res.add(DRJson.parsProgramserie(programserieJson, programserie));

      int offset = 0;
      final String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
      JSONObject data = new JSONObject(main_hent(url));
      if (offset == 0) {
        programserie = DRJson.parsProgramserie(data, programserie);
        DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
      }
      programserie.tilføjUdsendelser(offset, DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), null, DRData.instans));
      Log.d(programserie.slug + " = " + programserie.getUdsendelser());
    }
    Log.d("parseRadioDrama res="+res);
    System.exit(0);


    for (Kanal kanal : i.grunddata.kanaler) {
      if (Kanal.P4kode.equals(kanal.kode)) continue;
      if ("DRN".equals(kanal.kode)) continue;
      Log.d("\n=========================================== kanal = " + kanal);
      kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(main_hent(kanal.getUdsendelserUrl())), kanal, DRData.instans), "0");
      for (Udsendelse u : kanal.udsendelser) {
        Log.d("\nudsendelse = " + u);
        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) {
          String str = main_hent(u.getProgramserieUrl());
          if ("null".equals(str)) continue;
          JSONObject data = new JSONObject(str);
          ps = DRJson.parsProgramserie(data, null);
          JSONArray prg = data.getJSONArray(DRJson.Programs.name());
          ArrayList<Udsendelse> udsendelser = DRJson.parseUdsendelserForProgramserie(prg, kanal, DRData.instans);
          ps.tilføjUdsendelser(0, udsendelser);
          i.programserieFraSlug.put(u.programserieSlug, ps);
        }
        Log.d(ps.slug + " " + ps);
        System.exit(0);
      }
    }
  }

}
