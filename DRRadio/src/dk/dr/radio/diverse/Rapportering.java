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

package dk.dr.radio.diverse;

import dk.dr.radio.util.Log;
import dk.dr.radio.afspilning.AfspillerListener;

/**
 *
 * @author j
 */
public class Rapportering implements AfspillerListener {

  //  rapportering_sidsteonAfspilningStartet = System.currentTimeMillis();


  public String lydformat;

  public long afspilningForsøgtStartet;

  public long afspilningFaktiskStartet;


  public void nulstil() {
    lydformat = "UKENDT";
    afspilningFaktiskStartet = afspilningForsøgtStartet = 0;
  }



  public void onAfspilningStartet() {
    afspilningFaktiskStartet = System.currentTimeMillis();
  }

  public void onAfspilningStoppet() {
  }

  public void onAfspilningForbinder(int bufferProcent) {
    // Hvis vi får 'forbinder' med procenter efter afspilning faktisk er startet så er der hakker
    if (bufferProcent>0 && afspilningFaktiskStartet>0) nulstil();
  }

  /** Giver en rappport - hvis der er noget at fortælle
   * @return null hvis det ikke er værd at rapportere
   */
  public String rapport() {
    if (afspilningFaktiskStartet==0) return null;
    long nu = System.currentTimeMillis();
    long spilletUafbrudt = (nu - afspilningFaktiskStartet)/1000;
    long forsøgtTilFaktiskStart = (afspilningFaktiskStartet - afspilningForsøgtStartet)/1000;
    String rapport = "Rapportering: "+lydformat+" forsøgtTilFaktiskStart="+forsøgtTilFaktiskStart+" spilletUafbrudt="+spilletUafbrudt;
    Log.d(rapport);

    if (spilletUafbrudt > 60*10) { // Over 10 minutters uafbrudt afspilning
      rapport = "forsøgtTilFaktiskStart="+forsøgtTilFaktiskStart+" spilletUafbrudt="+spilletUafbrudt;
      //Log.d("Over 10 minutters uafbrudt afspilning - indsender rapport");
      return rapport;
    }
    return null;
  }

}
