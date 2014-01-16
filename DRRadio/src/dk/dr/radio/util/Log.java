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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;

import com.bugsense.trace.BugSenseHandler;

/**
 * Loggerklasse
 * - hvor man slipper for at angive tag
 * - man kan logge objekter (få kaldt toString)
 * - cirkulær buffer tillader at man kan gemme loggen til fejlrapportering
 *
 * @author j
 */
public class Log {
  public static final String TAG = "DRRadio";

  private static final StringBuilder log = new StringBuilder(18000);

  /**
   * Føjer data til loggen.
   * Er loggen blevet for lang trimmes den.
   * Er synkroniseret da der enkelte gange er blevet set crashes fordi der blev skrevet
   * loggen samtidig med at den var ved at blive trimmet.
   * Af performancehensyn bør logning nok begrænses til kun at omfatte det vi som udviklere
   * tror vil afhjælpe en evt senere fejlfinding
   */
  private static synchronized void logappend(String s) {
    if (log.length() > 57500) {
      log.delete(0, 10000);
    }
    // Roterende log
    int n = s.length();
    if (n > 10000) n = 10000;
    log.append(s, 0, n);
    log.append('\n');
  }

  public static synchronized String getLog() {
    return log.toString();
  }

  /**
   * Logfunktion uden TAG som tager et objekt. Sparer bytekode og tid
   */
  public static void d(Object o) {
    String s = String.valueOf(o);
    android.util.Log.d(TAG, s);
    logappend(s);
  }

  public static void e(Exception e) {
    e("fejl", e);
  }

  public static void e(String tekst, Exception e) {
    android.util.Log.e(TAG, tekst, e);
    //e.printStackTrace();
    logappend(android.util.Log.getStackTraceString(e));
  }


  public static void rapporterFejl(final Exception e) {
    BugSenseHandler.sendException(e);
    Log.e(e);
  }


  public static void rapporterOgvisFejl(final Activity akt, final Exception e) {
    BugSenseHandler.sendException(e);
    Log.e(e);

    Builder ab = new AlertDialog.Builder(akt);
    ab.setTitle("Beklager, der skete en fejl");
    ab.setMessage(e.toString());
    ab.setNegativeButton("Fortsæt", null);
    ab.setPositiveButton("Indsend fejl", new Dialog.OnClickListener() {
      public void onClick(DialogInterface arg0, int arg1) {
        String brødtekst = "Skriv, hvad der skete:\n\n\n---\n";
        brødtekst += "\nFejlspor;\n" + android.util.Log.getStackTraceString(e);
        brødtekst += "\n\n" + new MedieafspillerInfo().lavTelefoninfo(akt);
        Kontakt.kontakt(akt, "Fejl DR Radio", brødtekst, Log.log.toString());
      }

    });
    ab.create().show();
  }
}
