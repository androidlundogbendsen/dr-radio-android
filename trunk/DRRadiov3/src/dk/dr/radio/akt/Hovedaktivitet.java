package dk.dr.radio.akt;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;

import dk.dr.radio.akt.diverse.Basisaktivitet;
import dk.dr.radio.v3.R;

public class Hovedaktivitet extends Basisaktivitet {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private Venstremenu_frag venstremenuFrag;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.hoved_akt);

    //ActionBar actionBar = getSupportActionBar();
    //actionBar.setDisplayShowTitleEnabled(false);

    venstremenuFrag = (Venstremenu_frag) getSupportFragmentManager().findFragmentById(R.id.navigation_frag);

    // Set up the drawer.
    venstremenuFrag.setUp(R.id.navigation_frag, (DrawerLayout) findViewById(R.id.drawer_layout));
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
