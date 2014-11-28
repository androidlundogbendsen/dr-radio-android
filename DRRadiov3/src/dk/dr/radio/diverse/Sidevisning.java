package dk.dr.radio.diverse;

import android.content.Intent;

import com.gemius.sdk.MobilePlugin;

import java.util.HashMap;
import java.util.HashSet;

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
import dk.dr.radio.v3.R;

/**
 * Created by j on 28-11-14.
 */
public class Sidevisning {
  private final static HashMap<Class, String> m = new HashMap<Class, String>();
  static {
    m.put(Afspiller_frag.class, "afspiller");
    m.put(AlleUdsendelserAtilAA_frag.class, "alle_udsendelser");
    m.put(DramaOgBog_frag.class, "drama_og_bog");
    m.put(FangBrowseIntent_akt.class, "fang_browser");
    m.put(Favoritprogrammer_frag.class, "favoritter");
    m.put(Hentede_udsendelser_frag.class, "hentede_udsendelser");
    m.put(Indstillinger_akt.class, "indstillinger");
    m.put(Kanal_frag.class, "kanal");
    m.put(Kanaler_frag.class, "kanaler");
    m.put(Kontakt_info_om_frag.class, "kontakt");
    m.put(P4kanalvalg_frag.class, "p4_kanalvalg");
    m.put(Programserie_frag.class, "programserie");
    m.put(Senest_lyttede_frag.class, "senest_lyttede");
    m.put(Soeg_efter_program_frag.class, "søg");
    m.put(Udsendelse_frag.class, "udsendelse");
    m.put(String.class, "del_udsendelse"); // bare en eller anden unik klasse - det er værdien der skal bruges
  }
  private final static HashSet<String> besøgt = new HashSet<String>();
  private static Intent intent;

  public static void vist(String side, String slug) {
    // Gemius sidevisningsstatistik
    String data = "side=" + side + (slug == null ? "" : "|slug=" + slug);
    besøgt.add(side);
    Log.d("sidevisning "+data);
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
      Log.rapporterFejl(new IllegalArgumentException("Klasse mangler navn til sidevisning: "+fk));
      side = fk.getSimpleName();
      m.put(fk, side);
    }
    vist(side, slug);
  }

  public static void vist(Class fk) {
    vist(fk, null);
  }
}
