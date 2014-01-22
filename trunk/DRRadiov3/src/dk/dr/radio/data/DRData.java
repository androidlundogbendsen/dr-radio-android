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

import android.os.Handler;

import dk.dr.radio.data.stamdata.Stamdata;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class DRData {

  public static DRData instans;


  public static final String STAMDATA_URL = "http://javabog.dk/privat/stamdata_android_v3_013_01.json";
  //public static final String STAMDATA_URL = "http://www.dr.dk/tjenester/iphone/radio/settings/android29.drxml";

  public static boolean udvikling;

  private Handler handler = new Handler();


  public Stamdata stamdata;


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

          tjekForNyeStamdata();

        } catch (Exception ex) {
          Log.e(ex);
        }
      }
    }
  };


  /**
   * Tjek om en evt ny udgave af stamdata skal indlæses
   */
  private void tjekForNyeStamdata() {
    final String STAMDATA_SIDST_INDLÆST = "stamdata_sidst_indlæst";
    long sidst = App.prefs.getLong(STAMDATA_SIDST_INDLÆST, 0);
    long nu = System.currentTimeMillis();
    long alder = (nu - sidst) / 1000 / 60;
    /*
    if (alder >= 30) try { // stamdata er ældre end en halv time
      Log.d("Stamdata er " + alder + " minutter gamle, opdaterer dem...");
      // Opdater tid (hvad enten indlæsning lykkes eller ej)
      App.prefs.edit().putLong(STAMDATA_SIDST_INDLÆST, nu).commit();

      String stamdatastr = JsonIndlaesning.hentUrlSomStreng(STAMDATA_URL);
      //Log.d(stamdatastr);
      final Stamdata stamdata2 = Stamdata.parseStamdatafil(stamdatastr);

      MANGLER: Parsning af stamdata.parseAlleKanaler(alleKanalerStr);

      // Hentning og parsning gik godt - vi gemmer den nye udgave i prefs
      App.prefs.edit().putString(STAMDATA_URL, stamdatastr).commit();

    } catch (Exception e) {
      Log.e("Fejl parsning af stamdata. Url=" + STAMDATA_URL, e);
    }
    */
  }


}
