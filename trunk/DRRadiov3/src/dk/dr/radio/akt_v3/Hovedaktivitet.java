package dk.dr.radio.akt_v3;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Hovedaktivitet extends Basisaktivitet {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private Venstremenu_frag venstremenuFrag;

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence actionbartitel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.hoved_akt);

    venstremenuFrag = (Venstremenu_frag) getSupportFragmentManager().findFragmentById(R.id.navigation_frag);
    actionbartitel = getTitle();

    // Set up the drawer.
    venstremenuFrag.setUp(R.id.navigation_frag, (DrawerLayout) findViewById(R.id.drawer_layout));
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(actionbartitel);
  }


  /**
   * Om tilbageknappen skal afslutte programmet eller vise venstremenuen
   */
  static boolean tilbageViserVenstremenu = true; // hack - static, ellers skulle den gemmes i savedInstanceState

  @Override
  public void onBackPressed() {
    if (tilbageViserVenstremenu) {
      venstremenuFrag.visMenu();
      tilbageViserVenstremenu = false;
    } else {
      super.onBackPressed();
      tilbageViserVenstremenu = true;
    }
  }


  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    tilbageViserVenstremenu = true;
    return super.dispatchTouchEvent(ev);
  }

  @Override
  public boolean dispatchTrackballEvent(MotionEvent ev) {
    tilbageViserVenstremenu = true;
    return super.dispatchTrackballEvent(ev);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    Log.d("XXX venstremenuFrag.isDrawerOpen()=" + venstremenuFrag.isDrawerOpen());
    if (!venstremenuFrag.isDrawerOpen()) {
      // Only show items in the action bar relevant to this screen
      // if the drawer is not showing. Otherwise, let the drawer
      // decide what to show in the action bar.
      getMenuInflater().inflate(R.menu.hoved_akt, menu);
      //restoreActionBar();
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }


  public void sætTitel(String titel) {
    actionbartitel = titel;
    restoreActionBar();
  }


}
