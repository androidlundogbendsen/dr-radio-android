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

import java.io.File;
import java.io.FileInputStream;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.data.stamdata.Stamdata;
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

//    i.stamdata = Stamdata.xxx_parseStamdatafil(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
//    i.stamdata.skrald_parseAlleKanaler(Diverse.læsStreng(new FileInputStream("res/raw/skrald__alle_kanaler.json")));


    i.stamdata = Stamdata.parseAndroidStamdata(Diverse.læsStreng(new FileInputStream("res/raw/stamdata1_android_v3_01.json")));
    i.stamdata.parseFællesStamdata(Diverse.læsStreng(new FileInputStream("res/raw/stamdata2_faelles.json")));
    i.stamdata.hentSupplerendeDataBg();

//    Log.d(i.stamdata.kanaler);
    for (Kanal k : i.stamdata.kanaler) {
      Log.d("k = " + k);

    }


  }

}
