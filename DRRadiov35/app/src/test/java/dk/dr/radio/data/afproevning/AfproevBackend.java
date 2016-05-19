package dk.dr.radio.data.afproevning;


import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.DRBackendTidsformater;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = AfproevBackend.TestApp.class)
//@Config(constants = BuildConfig.class, sdk = 21)
public class AfproevBackend {

  static String hentStreng(String url) throws IOException {
    //String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
    Log.d(url);
    url = url.replaceAll("Ø", "%C3%98");
    url = url.replaceAll("Å", "%C3%85");
    String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 12 * 1000 * 60 * 60)));
    Log.d(data);
    return data;

  }

  static void hentSupplerendeData(Grunddata ths) {
    for (Kanal k : ths.kanaler)
      try {
        if (k.kode.equals("P4F")) continue;
        String url = k.getStreamsUrl();
        String data = hentStreng(url);
        JSONObject o = new JSONObject(data);
        k.setStreams(o);
        //Log.d(k.kode + " k.lydUrl=" + k.streams);
      } catch (Exception e) {
        Log.e("Fejl for "+k,e);
      }
    Log.d("DRData.instans.grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug);
  }

  public static class TestApp extends App {
        static {
            IKKE_Android_VM = true;
        }

        @Override
        public void onCreate() {
            Log.d("onCreate " + Build.PRODUCT + Build.MODEL);
            FilCache.init(new File("/tmp/drradio-cache"));
            Log.d("arbejdsmappe = "+new File(".").getAbsolutePath());
            DRBackendTidsformater.servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard Java
            super.onCreate();
        }
    }

    @Test
    public void tjekUdelukFraHLS() throws Exception {
        System.out.println("hejsa");

        DRData i = DRData.instans;
        //i.grunddata = new Grunddata();
        //i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json")));
        i.grunddata.android_json.put("udeluk_HLS2", "C6603 .*/18, IdeaPadA10 A10/17, LIFETAB_E7312 LIFETAB_E7310/17, LIFETAB_E10310/.*");
        i.grunddata.udelukHLS = false;
        i.grunddata.tjekUdelukFraHLS("C6603 C6603/18");
        if (i.grunddata.udelukHLS != true) throw new Exception();
        i.grunddata.tjekUdelukFraHLS("C6603 C6603/17");
        if (i.grunddata.udelukHLS == true) throw new Exception();
        i.grunddata.tjekUdelukFraHLS("IdeaPadA10 A10/17");
        if (i.grunddata.udelukHLS != true) throw new Exception();
        i.grunddata.tjekUdelukFraHLS("IdeaPadA10 A10/23");
        if (i.grunddata.udelukHLS == true) throw new Exception();
        i.grunddata.tjekUdelukFraHLS("IdeaPadA10 A11/17");
        if (i.grunddata.udelukHLS == true) throw new Exception();
        i.grunddata.tjekUdelukFraHLS("LIFETAB_E10310/16");
        if (i.grunddata.udelukHLS != true) throw new Exception();
        //assertTrue(Robolectric.setupActivity(Hovedaktivitet.class) != null);
    }

    @Test
    public void tjek_hent_a_til_å() throws Exception {
        System.out.println("tjek_hent_a_til_å");
        DRData i = DRData.instans;
        //i.grunddata = new Grunddata();
        //i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json")));
        //hentSupplerendeData(i.grunddata);


        // A-Å-liste
        {
          JSONArray jsonArray = new JSONArray(hentStreng(DRData.getAtilÅUrl()));


          // http://www.dr.dk/tjenester/mu-apps/series?type=radio&includePrograms=true&urn=urn:dr:mu:bundle:50d2ab93860d9a09809ca4f2
          ArrayList<Programserie> res = new ArrayList<Programserie>();
          for (int n = 0; n < jsonArray.length(); n++) {
            JSONObject programserieJson = jsonArray.getJSONObject(n);
            String programserieSlug = programserieJson.getString(DRJson.Slug.name());
    //        Log.d("\n=========================================== programserieSlug = " + programserieSlug);

            Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
            if (programserie == null) {
              programserie = new Programserie();
              DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
            }
            res.add(DRJson.parsProgramserie(programserieJson, programserie));
    /*
            int offset = 0;
            // Virker ikke, giver ALLE udsendelser i RadioDrama:
            // final String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
            final String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
            JSONObject data = new JSONObject(hentStreng(url));
            if (offset == 0) {
              programserie = DRJson.parsProgramserie(data, programserie);
              DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
            }
            programserie.tilføjUdsendelser(offset, DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), null, DRData.instans));
            Log.d(programserie.slug + " = " + programserie.getUdsendelser());
    */
          }
          Log.d("res=" + res);
        }
    }

    @Test
    public void tjek_hent_podcast() throws Exception {
        System.out.println("tjek_hent_podcast");
        DRData i = DRData.instans;
        //i.grunddata = new Grunddata();
        //i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json")));
        //hentSupplerendeData(i.grunddata);
        i.dramaOgBog.parseSvar(hentStreng(i.dramaOgBog.url));
//        assertThat(i.dramaOgBog.karusel, hasSize(greaterThan(0)));
        assertNotSame(new ArrayList<Udsendelse>(), i.dramaOgBog.karusel);
      System.out.println("tjek_hent_podcast slut");

    }

    @Test
    public void tjekAktuelleUdsendelser() throws Exception {
        System.out.println("tjekAktuelleUdsendelser");
        DRData i = DRData.instans;// = new DRData();
        //i.grunddata = new Grunddata();
        //i.grunddata.parseFællesGrunddata(Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json")));

        //hentSupplerendeData(i.grunddata);
        //System.exit(0);

        for (Kanal kanal : i.grunddata.kanaler) {
          Log.d("\n\n===========================================\n\nkanal = " + kanal);
          if (Kanal.P4kode.equals(kanal.kode)) continue;
          if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder

          String datoStr = DRJson.apiDatoFormat.format(new Date());
          kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(
              hentStreng(DRData.getKanalUdsendelserUrlFraKode(kanal.kode, datoStr))), kanal, new Date(), DRData.instans), "0");
          int antalUdsendelser = 0;
          int antalUdsendelserMedPlaylister = 0;
          for (Udsendelse u : kanal.udsendelser) {
            Log.d("\nudsendelse = " + u);
            antalUdsendelser++;
            JSONObject obj = new JSONObject(hentStreng(u.getStreamsUrl()));
            //Log.d(obj.toString(2));
            boolean MANGLER_SeriesSlug = !obj.has(DRJson.SeriesSlug.name());

            u.setStreams(obj);
            if (!u.harStreams()) Log.d("Ingen lydstreams");

            try {
              u.playliste = DRJson.parsePlayliste(new JSONArray(hentStreng(DRData.getPlaylisteUrl(u))));
              if (u.playliste.size()>0) {
                antalUdsendelserMedPlaylister++;
                Log.d("u.playliste= " + u.playliste);
              }
            } catch (IOException e) {
              e.printStackTrace();
            }

            boolean gavNull = false;
            Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
            if (ps == null) try {
              String str = hentStreng(DRData.getProgramserieUrl(null, u.programserieSlug));
              if ("null".equals(str)) gavNull = true;
              else {
                JSONObject data = new JSONObject(str);
                ps = DRJson.parsProgramserie(data, null);
                JSONArray prg = data.getJSONArray(DRJson.Programs.name());
                ArrayList<Udsendelse> udsendelser = DRJson.parseUdsendelserForProgramserie(prg, kanal, DRData.instans);
                ps.tilføjUdsendelser(0, udsendelser);
                i.programserieFraSlug.put(u.programserieSlug, ps);
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
            if (MANGLER_SeriesSlug)
              Log.d("MANGLER_SeriesSlug " + u + " gavNull=" + gavNull + "  fra dagsprogram =" + u.programserieSlug);
          }
          if (antalUdsendelserMedPlaylister < antalUdsendelser*50) {
            new Exception(antalUdsendelserMedPlaylister +" ud af "+ antalUdsendelser + " har playlister for "+kanal).printStackTrace();
          }
        }
    }
}
