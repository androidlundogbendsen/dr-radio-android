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

package dk.dr.radio;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import dk.dr.radio.data.DRData;
import dk.dr.radio.util.Log;

public class Indstillinger_akt extends PreferenceActivity implements OnPreferenceChangeListener, Runnable {
  public static final String åbn_formatindstilling = "åbn_formatindstilling";
  private String aktueltLydformat;
  private ListPreference lydformatlp;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.indstillinger);

      try {
        DRData.tjekInstansIndlæst(this);

        // Find lydformat
        PreferenceScreen ps = this.getPreferenceScreen();
        final int POS_lydformat = 1;

        lydformatlp = (ListPreference)ps.getRootAdapter().getItem(POS_lydformat);
        if (!DRData.NØGLE_lydformat.equals(lydformatlp.getKey())) {
          if (DRData.udvikling) throw new InternalError("lydformat har skiftet plads, ret i koden");
          return; // drop resten af initialiseringen i produktion
        }

        // Udfyld med værdier der er passende for denne enhed
        // Er det Android 2.2 eller derunder kan vi godt fjerne HLS og HLS2
        if (Build.VERSION.SDK_INT < 9) {
          lydformatlp.setEntries( lavDelarray(lydformatlp.getEntries(), 2) );
          lydformatlp.setEntryValues( lavDelarray(lydformatlp.getEntryValues(), 2) );
        }

        // Har brugeren trykket på "Format" på afspilleraktiviteten skal format åbnes direkte
        if (getIntent().getBooleanExtra(åbn_formatindstilling, false)) try {
          // Jeg synes toast alligevel er overflødig. Jacob
          //Toast.makeText(this, lydformatlp.getSummary(), Toast.LENGTH_LONG).show();
          ps.onItemClick(null, null, POS_lydformat, 0);
        } catch (Exception e) {} // Ignorer - se http://www.bugsense.com/dashboard/project/57c90f98/error/11696187

        lydformatlp.setOnPreferenceChangeListener(this);
        aktueltLydformat = lydformatlp.getValue();
      } catch (Exception ex) {
        Log.kritiskFejlStille(ex);
      }
   }

  private CharSequence[] lavDelarray(CharSequence[] entries, int antal) {
    CharSequence[] res = new CharSequence[antal];
    for (int i=0; i<antal; i++) res[i] = entries[i];
    return res;
  }

  Handler handler = new Handler();

  public boolean onPreferenceChange(Preference preference, Object newValue) {
    // På dette tidspunkt er indstillingen ikke gemt endnu, det bliver den
    // først når metoden har returneret true.
    // Vi venter derfor med at opdatere afspilleren med det nye lydformat
    // indtil GUI-tråden er færdig med kaldet til onPreferenceChange() og
    // klar igen
    handler.post(this);
    return true;
  }

	/** Håndtering af MENU-knappen */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0,0,0,"udvikler");
    return super.onCreateOptionsMenu(menu);
	}

  int onPrepareOptionsMenu = 0;

	/** Håndtering af MENU-knappen. Vis 'udvikler' efter 10 tryk */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
    onPrepareOptionsMenu++;
    Log.d("onPrepareOptionsMenu="+onPrepareOptionsMenu);
    menu.findItem(0).setVisible(false);
    if (onPrepareOptionsMenu>9) {
      DRData.udvikling = !DRData.udvikling;
      DRData.toast("udvikling="+DRData.udvikling);
    }
		return super.onPrepareOptionsMenu(menu);
	}


  public void run() {
    String nytLydformat = lydformatlp.getValue();
    if (nytLydformat.equals(aktueltLydformat)) return;

    Log.d("Lydformatet blev ændret fra "+aktueltLydformat+" til "+nytLydformat);
    aktueltLydformat = nytLydformat;
    DRData drdata = DRData.instans;
    String url = drdata.findKanalUrlFraKode(drdata.aktuelKanal);
    drdata.afspiller.setKanal(drdata.aktuelKanal.longName, url);
  }
}
