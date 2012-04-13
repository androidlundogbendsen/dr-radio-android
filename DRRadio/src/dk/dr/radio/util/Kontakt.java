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

package dk.dr.radio.util;

import android.app.Activity;
import android.content.Intent;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.JsonIndlaesning;

/**
 *
 * @author j
 */
public class Kontakt {



  public static void kontakt(Activity akt, String emne, String txt) {

    String[] modtagere = null;
    try {
      modtagere = JsonIndlaesning.jsonArrayTilArrayListString(DRData.instans.stamdata.json.getJSONArray("feedback_modtagere")).toArray(new String[0]);
    } catch (Exception ex) {
      Log.e("JSONParsning af feedback_modtagere", ex);
      modtagere = new String[] { "MIKP@dr.dk", "fraa@dr.dk", "jacob.nordfalk@gmail.com"};
    }

    Intent i = new Intent(android.content.Intent.ACTION_SEND);
    i.setType("plain/text");
    i.putExtra(android.content.Intent.EXTRA_EMAIL, modtagere);
    i.putExtra(android.content.Intent.EXTRA_SUBJECT, emne);
    i.putExtra(android.content.Intent.EXTRA_TEXT, txt);
    akt.startActivity(Intent.createChooser(i, "Send meddelelse..."));
  }
}
