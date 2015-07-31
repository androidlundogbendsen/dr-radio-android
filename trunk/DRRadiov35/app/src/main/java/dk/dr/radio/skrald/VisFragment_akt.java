package dk.dr.radio.skrald;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.FrameLayout;

import dk.dr.radio.akt.Basisaktivitet;
import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Sidevisning;


/**
 * Aktivitet der instantierer og viser ét fragment.
 */
public class VisFragment_akt extends Basisaktivitet {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    try {
      super.onCreate(savedInstanceState);
      //super.getActionBarSetDisplayHomeAsUpEnabledKompat(true);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      FrameLayout fl = new FrameLayout(this);
      fl.setId(android.R.id.input);
      setContentView(fl);

      if (savedInstanceState != null) {
        return;
      }

      String klasse = getIntent().getStringExtra(Hovedaktivitet.VIS_FRAGMENT_KLASSE);
      Fragment f = (Fragment) Class.forName(klasse).newInstance();
      Bundle b = getIntent().getExtras();
      f.setArguments(b);

      // Vis fragmentet i FrameLayoutet
      Log.d("Viser fragment " + f + " med arg " + b);
      getSupportFragmentManager().beginTransaction().add(android.R.id.input, f).commit();
      Sidevisning.vist(f.getClass());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      NavUtils.navigateUpFromSameTask(this);
    }
    return super.onOptionsItemSelected(item);
  }
}
