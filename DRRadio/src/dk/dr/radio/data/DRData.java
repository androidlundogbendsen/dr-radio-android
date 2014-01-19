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

import android.content.Intent;
import android.os.Handler;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.data.spiller_nu.SpillerNu;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.data.stamdata.Stamdata;
import dk.dr.radio.data.udsendelser.Udsendelser;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Rapportering;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class DRData {

  public static DRData instans;


  public static final String STAMDATA_URL = "http://javabog.dk/privat/stamdata_android29.json";
  //public static final String STAMDATA_URL = "http://www.dr.dk/tjenester/iphone/radio/settings/android29.drxml";

  public static boolean udvikling;
  public Afspiller afspiller;

  private Handler handler = new Handler();


  public Stamdata stamdata;

  public String aktuelKanalkode;
  public Kanal aktuelKanal;

  public Udsendelser udsendelser;
  private Udsendelser udsendelser2;
  public boolean udsendelser_ikkeTilgængeligt;

  public SpillerNu spillerNuListe;
  private SpillerNu spillerNuListe2;

  public static final String NØGLE_lydformat = "lydformat";
  public static final String NØGLE_kanal = "kanal";

  public final Rapportering rapportering = new Rapportering();

  /**
   * Bruges til at sende broadcasts om nye stamdata
   */
  public static final String OPDATERINGSINTENT_Stamdata = "dk.dr.radio.afspiller.OPDATERING_Stamdata";

  /**
   * Bruges til at sende broadcasts om ny info om udsendelsen (programinfo)
   */
  public static final String OPDATERINGSINTENT_Udsendelse = "dk.dr.radio.afspiller.OPDATERING_Udsendelse";

  /**
   * Bruges til at sende broadcasts om ny info om hvad der spiller nu
   */
  public static final String OPDATERINGSINTENT_SpillerNuListe = "dk.dr.radio.afspiller.OPDATERING_SpillerNuListe";


  //
  // Opdateringer i baggrunden.
  //
  private boolean baggrundsopdateringAktiv = false;
  private boolean baggrundstrådSkalVente = true;


  /**
   * Først efter indlæstning starter vi baggrundstråden - fra splash og fra afspiller_akt.
   * Dette er et separat skridt da det ikke skal ske ved opstart af levende ikon
   */
  public void tjekBaggrundstrådStartet() {
    if (!baggrundstråd.isAlive()) baggrundstråd.start();
  }


  public void setBaggrundsopdateringAktiv(boolean aktiv) {
    if (baggrundsopdateringAktiv == aktiv) return;

    baggrundsopdateringAktiv = aktiv;

    Log.d("setBaggrundsopdateringAktiv( " + aktiv);

    if (baggrundsopdateringAktiv) baggrundstrådSkalOpdatereNu(); // væk baggrundtråd
  }


  private void baggrundstrådSkalOpdatereNu() {
    baggrundstrådSkalVente = false;
    synchronized (baggrundstråd) {
      baggrundstråd.notify();
    }
  }

  final Thread baggrundstråd = new Thread() {
    @Override
    public void run() {

      // Hovedløkke
      while (true) {
        try {
          if (baggrundstrådSkalVente) synchronized (baggrundstråd) {
            if (baggrundsopdateringAktiv)
              baggrundstråd.wait(15000); // Vent 15 sekunder. Men vågn op hvis nogen kalder baggrundstråd.notify()!
            // baggrundsopdateringAktiv kan være sat til false inden for de sidste 15 sekunder og så skal vi vente videre

            if (!baggrundsopdateringAktiv) baggrundstråd.wait(); // Vent indtil tråden vækkes

            baggrundstråd.wait(50); // Vent kort så den aktiverende tråd kan gøre sit arbejde færdigt
          }
          baggrundstrådSkalVente = true;

          hentUdsendelserOgSpillerNuListe();
          tjekForNyeStamdata();

        } catch (Exception ex) {
          Log.e(ex);
        }
      }
    }
  };


  private void hentUdsendelserOgSpillerNuListe() {
    String url = stamdata.json.optString("spiller_nu_url") + aktuelKanalkode;
    spillerNuListe2 = null;

    if (stamdata.kanalerDerSkalViseSpillerNu.contains(aktuelKanalkode)) {
      try {
        spillerNuListe2 = JsonIndlaesning.hentSpillerNuListe(url);
      } catch (Exception ex) {
        Log.d("Kunne ikke hente spillerNuListe " + url);
      }
      // Al opdatering, herunder tildeling bør ske i GUI-tråden for at undgå at
      // GUIen er i gang med at bruge objektet mens det opdateres
      handler.post(new Runnable() {
        public void run() {
          spillerNuListe = spillerNuListe2;
          // Send broadcast om at listen er opdateret
          App.instans.sendBroadcast(new Intent(OPDATERINGSINTENT_SpillerNuListe));
        }
      });
    }

    try {
      url = stamdata.json.optString("program_url") + aktuelKanalkode;
      udsendelser2 = JsonIndlaesning.hentUdsendelser(url);
      udsendelser_ikkeTilgængeligt = false;
    } catch (Exception ex) {
      Log.e("Kunne ikke hente udsendelser fra " + url, ex);
      //Skal ikke rapporteres: https://www.bugsense.com/dashboard/project/57c90f98#error/28786099 , https://www.bugsense.com/dashboard/project/57c90f98#error/61580882  og https://www.bugsense.com/dashboard/project/57c90f98#error/29664901 // Log.kritiskFejl(null,ex);
      udsendelser2 = null;
      udsendelser_ikkeTilgængeligt = true;
    }
    handler.post(new Runnable() {
      public void run() {
        udsendelser = udsendelser2;
        App.instans.sendBroadcast(new Intent(OPDATERINGSINTENT_Udsendelse));
      }
    });
  }

  /**
   * Tjek om en evt ny udgave af stamdata skal indlæses
   */
  private void tjekForNyeStamdata() {
    final String STAMDATA_SIDST_INDLÆST = "stamdata_sidst_indlæst";
    long sidst = App.prefs.getLong(STAMDATA_SIDST_INDLÆST, 0);
    long nu = System.currentTimeMillis();
    long alder = (nu - sidst) / 1000 / 60;
    if (alder >= 30) try { // stamdata er ældre end en halv time
      Log.d("Stamdata er " + alder + " minutter gamle, opdaterer dem...");
      // Opdater tid (hvad enten indlæsning lykkes eller ej)
      App.prefs.edit().putLong(STAMDATA_SIDST_INDLÆST, nu).commit();

      String stamdatastr = JsonIndlaesning.hentUrlSomStreng(STAMDATA_URL);
      //Log.d(stamdatastr);
      final Stamdata stamdata2 = JsonIndlaesning.parseStamdata(stamdatastr);
      // Hentning og parsning gik godt - vi gemmer den nye udgave i prefs
      App.prefs.edit().putString(STAMDATA_URL, stamdatastr).commit();

      // Al opdatering, herunder tildeling bør ske i GUI-tråden for at undgå at
      // GUIen er i gang med at bruge objektet mens det opdateres
      handler.post(new Runnable() {
        public void run() {
          stamdata = stamdata2;
          // Send broadcast om at stamdata er opdateret
          App.instans.sendBroadcast(new Intent(OPDATERINGSINTENT_Stamdata));
        }
      });
    } catch (Exception e) {
      Log.e("Fejl parsning af stamdata. Url=" + STAMDATA_URL, e);
    }
  }


  /**
   * Skifter til en anden kanal
   *
   * @param nyKanalkode en af "P1", "P2", "P3", "P5D", "P6B", "P7M", "RAM", etc
   *                    eller evt P4-kanal "KH4", "NV4", "AR4", "AB4", "OD4", "AL4", "HO4", "TR4", "RO4", "ES4", "NS4"],
   *                    Bemærk at "P4" eller andre uden en streamUrl IKKE er tilladt
   */
  public void skiftKanal(String nyKanalkode) {
    Log.d("DRData.skiftKanal(" + nyKanalkode);
    aktuelKanalkode = nyKanalkode;
    aktuelKanal = stamdata.kanalkodeTilKanal.get(aktuelKanalkode);

    App.prefs.edit().putString(NØGLE_kanal, aktuelKanalkode).commit();
    udsendelser = null;
    spillerNuListe = null;
    udsendelser_ikkeTilgængeligt = false;
    // Væk baggrundstråden så den indlæser den nye kanals udsendelser etc og laver broadcasts med nyt info
    baggrundstrådSkalOpdatereNu();
  }

  public String findKanalUrlFraKode(Kanal kanal) {
    /*
    String lydformat = prefs.getString(NØGLE_lydformat, "shoutcast");
    boolean højKvalitet = "høj".equals(prefs.getString("lydkvalitet", "standard"));
    rapportering.nulstil();
    rapportering.lydformat = lydformat + (højKvalitet ? "_høj" : "_standard");
    String url = kanal.shoutcastUrl;
    if ("rtsp".equals(lydformat)) url = kanal.rtspUrl;
    else if ("httplive".equals(lydformat)) url = kanal.aacUrl;
    else if ("httplive2".equals(lydformat)) url = kanal.aacUrl.replaceAll("httplive", "http");
    if (højKvalitet) {
      url = url.replace("LQ", "HQ");
      url = url.replace("quality=1", "quality=2");  // on-demand radio nyheder workaround
      url = url.replace("L.stream", "H.stream");    // MP3, RTSP stream navn workaround
    }
    String info = "Kanal: " + kanal.longName + "\nlydformat: " + lydformat + "\nKvalitet: " + (højKvalitet ? "Høj" : "Normal") + "\n" + url;
    if (DRData.udvikling) langToast(info);
    Log.d(info);
    */
    String url = kanal.shoutcastUrl;
    return url;
  }

}
