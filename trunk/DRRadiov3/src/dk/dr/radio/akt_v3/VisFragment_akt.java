package dk.dr.radio.akt_v3;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.FrameLayout;

import dk.dr.radio.diverse.Log;


/**
 * Aktivitet der instantierer og viser ét fragment.
 */
public class VisFragment_akt extends BasisAktivitet {
  public static final String KLASSE = "klasse";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    try {
      super.onCreate(savedInstanceState);
      FrameLayout fl = new FrameLayout(this);
      fl.setId(android.R.id.input);
      setContentView(fl);

      if (savedInstanceState != null) {
        return;
      }

      String klasse = getIntent().getStringExtra(KLASSE);
      Fragment f = (Fragment) Class.forName(klasse).newInstance();
      Bundle b = getIntent().getExtras();
      f.setArguments(b);

      // Vis fragmentet i FrameLayoutet
      Log.d("Viser fragment " + f + " med arg " + b);
      getSupportFragmentManager().beginTransaction().add(android.R.id.input, f).commit();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
