package dk.dr.radio.akt;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Hovedaktivitet extends Basisaktivitet {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private Venstremenu_frag venstremenuFrag;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // android:logo="@drawable/dr_logo" ignoreres på Android 2, sæt det derfor også her:
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB) {
      getSupportActionBar().setLogo(R.drawable.dr_logo);
    }

    if (App.prefs.getBoolean("tving_lodret_visning", true)) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    setContentView(R.layout.hoved_akt);

    //ActionBar actionBar = getSupportActionBar();
    //actionBar.setDisplayShowTitleEnabled(false);

    venstremenuFrag = (Venstremenu_frag) getSupportFragmentManager().findFragmentById(R.id.venstremenu_frag);

    // Set up the drawer.
    venstremenuFrag.setUp(R.id.venstremenu_frag, (DrawerLayout) findViewById(R.id.drawer_layout));

    if (savedInstanceState == null) try {
      venstremenuFrag.sætListemarkering(Venstremenu_frag.FORSIDE_INDEX); // "Forside

      String visFragment = getIntent().getStringExtra(VisFragment_akt.KLASSE);
      if (visFragment==null) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, new Kanaler_frag())
            .commit();
        venstremenuFrag.sætListemarkering(Venstremenu_frag.FORSIDE_INDEX); // "Forside
      } else {
        Fragment f = (Fragment) Class.forName(visFragment).newInstance();
        Bundle b = getIntent().getExtras();
        f.setArguments(b);

        // Vis fragmentet i FrameLayoutet
        Log.d("Viser fragment " + f + " med arg " + b);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, f).commit();
      }

    } catch (Exception e) { Log.rapporterFejl(e); }
  }


  @Override
  public void onBackPressed() {
    if (venstremenuFrag.isDrawerOpen()) {
      venstremenuFrag.skjulMenu();
    } else {
      super.onBackPressed();
    }
  }

  /**
   * Om tilbageknappen skal afslutte programmet eller vise venstremenuen
   static boolean tilbageViserVenstremenu = true; // hack - static, ellers skulle den gemmes i savedInstanceState

   @Override public void onBackPressed() {
   if (tilbageViserVenstremenu) {
   venstremenuFrag.visMenu();
   tilbageViserVenstremenu = false;
   } else {
   super.onBackPressed();
   tilbageViserVenstremenu = true;
   }
   }


   @Override public boolean dispatchTouchEvent(MotionEvent ev) {
   tilbageViserVenstremenu = true;
   return super.dispatchTouchEvent(ev);
   }

   @Override public boolean dispatchTrackballEvent(MotionEvent ev) {
   tilbageViserVenstremenu = true;
   return super.dispatchTrackballEvent(ev);
   }
   */
}
