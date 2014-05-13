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

package dk.dr.radio.akt;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.Formatter;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Indstillinger_akt extends PreferenceActivity implements OnPreferenceChangeListener, Runnable {
  public static final String åbn_formatindstilling = "åbn_formatindstilling";
  private String aktueltLydformat;
  private ListPreference lydformatlp;
  Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    App.prefs.edit().putBoolean("fejlsøgning", App.fejlsøgning);
    addPreferencesFromResource(R.xml.indstillinger);
    if (App.prefs.getBoolean("udviklerEkstra", false))
      addPreferencesFromResource(R.xml.indstillinger_udvikling);

    try {

      // Find lydformat
      lydformatlp = (ListPreference) findPreference(Lydkilde.INDST_lydformat);
      lydformatlp.setEnabled(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH);
      lydformatlp.setOnPreferenceChangeListener(this);
      aktueltLydformat = lydformatlp.getValue();

      ArrayList<File> l = HentedeUdsendelser.findMuligeEksternLagerstier();
      String[] visVærdi = new String[l.size()];
      String[] værdi = new String[l.size()];
      for (int i=0; i<l.size(); i++) {
        værdi[i] = l.get(i).toString();
        StatFs stat = new StatFs(værdi[i]);
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        visVærdi[i] = l.get(i).getParent() + " ("+ Formatter.formatFileSize(App.instans, availableBlocks * blockSize)+" ledig)";
      }
      ListPreference lp = (ListPreference) findPreference(HentedeUdsendelser.NØGLE_placeringAfHentedeFiler);
      if (visVærdi.length>0) {
        lp.setEntries(visVærdi);
        lp.setEntryValues(værdi);
        if (!App.prefs.contains(HentedeUdsendelser.NØGLE_placeringAfHentedeFiler)) {
          lp.setValueIndex(0); // Værdi nummer 0 er forvalgt
        }
      } else {
        lp.setEnabled(false);
        lp.setTitle("Adgang til eksternt lager mangler");
      }
    } catch (Exception ex) {
      Log.rapporterFejl(ex);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) try {
      getActionBar().setDisplayHomeAsUpEnabled(true);
    } catch (Exception e) { Log.rapporterFejl(e); } // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/824608029
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    App.udviklerEkstra = App.prefs.getBoolean("udviklerEkstra", false);
    App.fejlsøgning = App.prefs.getBoolean("fejlsøgning", false);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
    }
    return super.onOptionsItemSelected(item);
  }

  private CharSequence[] lavDelarray(CharSequence[] entries, int antal) {
    CharSequence[] res = new CharSequence[antal];
    for (int i = 0; i < antal; i++) res[i] = entries[i];
    return res;
  }

  public boolean onPreferenceChange(Preference preference, Object newValue) {
    // På dette tidspunkt er indstillingen ikke gemt endnu, det bliver den
    // først når metoden har returneret true.
    // Vi venter derfor med at opdatere afspilleren med det nye lydformat
    // indtil GUI-tråden er færdig med kaldet til onPreferenceChange() og
    // klar igen
    handler.post(this);
    return true;
  }

  public void run() {
    String nytLydformat = lydformatlp.getValue();
    if (nytLydformat.equals(aktueltLydformat)) return;

    Log.d("Lydformatet blev ændret fra " + aktueltLydformat + " til " + nytLydformat);
    aktueltLydformat = nytLydformat;
    DRData drdata = DRData.instans;
    //String url = drdata.findKanalUrlFraKode(drdata.aktuelKanal);
    DRData.instans.afspiller.setLydkilde(DRData.instans.afspiller.getLydkilde());
  }
}
