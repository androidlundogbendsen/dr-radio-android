package dk.dr.radio.diverse;

import android.content.Intent;

import com.gemius.sdk.MobilePlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import dk.dr.radio.akt.Afspiller_frag;
import dk.dr.radio.akt.AlleUdsendelserAtilAA_frag;
import dk.dr.radio.akt.DramaOgBog_frag;
import dk.dr.radio.akt.FangBrowseIntent_akt;
import dk.dr.radio.akt.Favoritprogrammer_frag;
import dk.dr.radio.akt.Hentede_udsendelser_frag;
import dk.dr.radio.akt.Indstillinger_akt;
import dk.dr.radio.akt.Kanal_frag;
import dk.dr.radio.akt.Kanaler_frag;
import dk.dr.radio.akt.Kontakt_info_om_frag;
import dk.dr.radio.akt.P4kanalvalg_frag;
import dk.dr.radio.akt.Programserie_frag;
import dk.dr.radio.akt.Senest_lyttede_frag;
import dk.dr.radio.akt.Soeg_efter_program_frag;
import dk.dr.radio.akt.Udsendelse_frag;
import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.v3.R;

/**
 * Created by j on 28-11-14.
 */
public class Sidevisning {
  private static final HashMap<Class, String> m = new HashMap<Class, String>();

  private static Sidevisning instans = null;
  public static Sidevisning i() {
    if (instans==null && App.ÆGTE_DR) try {
    /*
    Klasserne til Gallup er ikke med i offentligt repo, derfor indlæses klassen dynamisk
    instans = new GallupSidevisning();

    Generér ny version af Gallup JAR-fil med
    unzip /home/j/android/dr-radio-android/gallupstatistik/build/outputs/aar/gallupstatistik-debug.aar classes.jar -d /tmp
    mv /tmp/classes.jar /home/j/android/dr-radio-android/DRRadiov35/app/libs/gallupstatistik.jar
    */
      instans = ((Class<? extends Sidevisning>) Class.forName("dk.dr.radio.diverse.GallupSidevisning")).newInstance();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

    if (instans==null) {
      instans = new Sidevisning();
    }
    return instans;
  }

  public static final String DEL = "del_udsendelse";
  public static final String KONTAKT_SKRIV = "kontakt__skriv_meddelelse";

  static {
    m.put(Afspiller_frag.class, "afspiller_popop");
    m.put(AlleUdsendelserAtilAA_frag.class, "alle_udsendelser");
    m.put(DramaOgBog_frag.class, "drama_og_bog");
    m.put(FangBrowseIntent_akt.class, "fang_browser");
    m.put(Favoritprogrammer_frag.class, "favoritter");
    m.put(Hentede_udsendelser_frag.class, "hentede_udsendelser");
    m.put(HentedeUdsendelser.class, "hentet_en_udsendelse");
    m.put(Indstillinger_akt.class, "indstillinger");
    m.put(Kanal_frag.class, "kanal");
    m.put(Kanaler_frag.class, "kanaler");
    m.put(Kontakt_info_om_frag.class, "kontakt");
    m.put(P4kanalvalg_frag.class, "p4_kanalvalg");
    m.put(Programserie_frag.class, "programserie");
    m.put(Senest_lyttede_frag.class, "senest_lyttede");
    m.put(Soeg_efter_program_frag.class, "soeg");
    m.put(Udsendelse_frag.class, "udsendelse");
    m.put(String.class, DEL); // bare en eller anden unik klasse - det er værdien der skal bruges
    m.put(Integer.class, KONTAKT_SKRIV); // bare en eller anden unik klasse - det er værdien der skal bruges
  }
  private final static HashSet<String> besøgt = new HashSet<String>();
  private static Intent intent;

  public void vist(String side, String slug) {
    //if (!App.PRODUKTION) App.kortToast("vist "+side+" "+slug);
    // Gemius sidevisningsstatistik
    // appname=MyApp|version=1.0.0
    // app=DRRadio|platform=Android|page=Z|action=W|version=1.0.0
    // app = DRNyheder/DRRadio/DRTV/DRRamasjang/DRUltra
    // Z = notset (gælder alle apps, undtagen nyhedsappen)
    // W = begin/background/termination (gælder alle apps, undtagen nyhedsappen)
    besøgt.add(side);

    String data = "app=DRRadio|platform=Android|page="+side+(slug==null ? "" : "/" + slug)+"|action=pageload|version="+App.versionsnavn;
    sendTilGemius(data);
  }

  private void sendTilGemius(String data) {
    if (App.fejlsøgning || App.EMULATOR) Log.d("sendTilGemius "+data);
    if (intent==null) {
      String nøgle = App.instans.getString(R.string.gemius_sidevisninsstatistik_nøgle);
      if (nøgle.length()==0) return; // Nøgle til indrapportering mangler
      if (!App.prefs.getBoolean("Rapportér statistik", true)) return; // statistikrapportering fravalgt
      intent = new Intent(App.instans, MobilePlugin.class);
      intent.putExtra(MobilePlugin.IDENTIFIER, nøgle);
      intent.putExtra(MobilePlugin.SERVERPREFIX, "main");
    }
    intent.putExtra(MobilePlugin.EXTRAPARAMS, data);
    App.instans.startService(intent);
  }

  public static void vist(Class fk, String slug) {
    String side = m.get(fk);
    if (side==null) {
      if (App.ÆGTE_DR) Log.rapporterFejl(new IllegalArgumentException("Klasse mangler navn til sidevisning: "+fk));
      side = fk.getSimpleName();
      m.put(fk, side);
    }
    i().vist(side, slug);
  }

  public static void vist(Class fk) {
    vist(fk, null);
  }

  public static void vist(String side) {
    i().vist(side, null);
  }

  /** Giver sorteret af viste sider, som en streng */
  public static String getViste() {
    return new TreeSet<String>(besøgt).toString();
  }

  /** Giver sorteret af ikke-viste sider, som en streng */
  public static String getIkkeViste() {
    TreeSet<String> ejBesøgt = new TreeSet<String>(m.values());
    ejBesøgt.removeAll(besøgt);
    return ejBesøgt.toString();
  }


  public void synlig(boolean synligNu) {
    if (App.fejlsøgning) App.kortToast("synligNu = "+synligNu);

    String data = "app=DRRadio|platform=Android|page=notset|action="+(synligNu?"begin":"background")+"|version="+App.versionsnavn;
    sendTilGemius(data);
  }
}
