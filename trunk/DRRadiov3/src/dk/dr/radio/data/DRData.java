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

import java.util.HashMap;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Rapportering;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class DRData {

  public static DRData instans;

  // scp /home/j/android/dr-radio-android/DRRadiov3/res/raw/grunddata_udvikling.json j:../lundogbendsen/hjemmeside/drradiov3_grunddata.json

  public static final String GRUNDDATA_URL = App.PRODUKTION
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
  public ProgramserierAtilAA programserierAtilÅ = new ProgramserierAtilAA();
  public Radiodrama radiodrama = new Radiodrama();
}
