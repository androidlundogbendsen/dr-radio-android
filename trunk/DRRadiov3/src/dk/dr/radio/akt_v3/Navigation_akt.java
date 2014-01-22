package dk.dr.radio.akt_v3;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Navigation_akt extends BasisAktivitet {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private Navigation_frag navigationFrag;

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence actionbartitel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.navigation_akt);

    navigationFrag = (Navigation_frag) getSupportFragmentManager().findFragmentById(R.id.navigation_frag);
    actionbartitel = getTitle();

    // Set up the drawer.
    navigationFrag.setUp(R.id.navigation_frag, (DrawerLayout) findViewById(R.id.drawer_layout));
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(actionbartitel);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    Log.d("XXX navigationFrag.isDrawerOpen()=" + navigationFrag.isDrawerOpen());
    if (!navigationFrag.isDrawerOpen()) {
      // Only show items in the action bar relevant to this screen
      // if the drawer is not showing. Otherwise, let the drawer
      // decide what to show in the action bar.
      getMenuInflater().inflate(R.menu.hoved_akt, menu);
      restoreActionBar();
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
  }


}
