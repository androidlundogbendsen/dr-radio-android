package dk.dr.radio.akt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import com.androidquery.AQuery;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.diverse.App;
import dk.dr.radio.skrald.Om_DRRadio_akt;
import dk.dr.radio.v3.R;


/**
 * Venstremenu-navigering
 * Se <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for en nærmere beskrivelse.
 */
public class Venstremenu_frag extends Fragment {

  /**
   * Remember the position of the selected item.
   */
  private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

  /**
   * Per the design guidelines, you should show the drawer on launch until the user manually
   * expands it. This shared preference tracks this.
   */
  private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

  /**
   * Helper component that ties the action bar to the navigation drawer.
   */
  private ActionBarDrawerToggle mDrawerToggle;

  private DrawerLayout mDrawerLayout;
  private ListView navListView;
  private View mFragmentContainerView;

  private int mCurrentSelectedPosition = 0;
  private boolean mFromSavedInstanceState;
  private boolean mUserLearnedDrawer;
  private Navigation_adapter navAdapter;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Read in the flag indicating whether or not the user has demonstrated awareness of the
    // drawer. See PREF_USER_LEARNED_DRAWER for details.
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // Indicate that this fragment would like to influence the set of actions in the action bar.
    setHasOptionsMenu(true);
    if (savedInstanceState != null) {
      mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
      mFromSavedInstanceState = true;
    } else {
      mCurrentSelectedPosition = 9;
      navAdapter.vælgMenu(getActivity(), mCurrentSelectedPosition);
    }

    // Select either the default item (0) or the last selected item.
    selectItem(mCurrentSelectedPosition);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    navListView = (ListView) inflater.inflate(R.layout.venstremenu_frag, container, false);
    navListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectItem(position);
        navAdapter.vælgMenu(getActivity(), position);
      }
    });
    navAdapter = new Navigation_adapter(getActionBar().getThemedContext());
    navListView.setAdapter(navAdapter);
    navListView.setItemChecked(mCurrentSelectedPosition, true);
    return navListView;
  }


  public boolean isDrawerOpen() {
    return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
  }

  /**
   * Users of this fragment must call this method to set up the navigation drawer interactions.
   *
   * @param fragmentId   The android:id of this fragment in its activity's layout.
   * @param drawerLayout The DrawerLayout containing this fragment's UI.
   */
  public void setUp(int fragmentId, DrawerLayout drawerLayout) {
    mFragmentContainerView = getActivity().findViewById(fragmentId);
    mDrawerLayout = drawerLayout;

    // set a custom shadow that overlays the main content when the drawer opens
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    // set up the drawer's list view with items and click listener

    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the navigation drawer and the action bar app icon.
    mDrawerToggle = new ActionBarDrawerToggle(getActivity(),                    /* host Activity */
        mDrawerLayout,                    /* DrawerLayout object */
        R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
        R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
        R.string.navigation_drawer_close  /* "close drawer" description for accessibility */) {
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        if (!isAdded()) {
          return;
        }

        getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        if (!isAdded()) {
          return;
        }

        if (!mUserLearnedDrawer) {
          // The user manually opened the drawer; store this flag to prevent auto-showing
          // the navigation drawer automatically in the future.
          mUserLearnedDrawer = true;
          SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
          sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
        }

        getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
      }
    };

    // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
    // per the navigation drawer design guidelines.
    if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
      mDrawerLayout.openDrawer(mFragmentContainerView);
    }

    // Defer code dependent on restoration of previous instance state.
    mDrawerLayout.post(new Runnable() {
      @Override
      public void run() {
        mDrawerToggle.syncState();
      }
    });

    mDrawerLayout.setDrawerListener(mDrawerToggle);
  }

  private void selectItem(int position) {
    mCurrentSelectedPosition = position;
    if (navListView != null) {
      navListView.setItemChecked(position, true);
    }
    if (mDrawerLayout != null) {
      mDrawerLayout.closeDrawer(mFragmentContainerView);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Forward the new configuration the drawer toggle component.
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // If the drawer is open, show the global app actions in the action bar. See also
    // showGlobalContextActionBar, which controls the top-left area of the action bar.
    if (mDrawerLayout != null && isDrawerOpen()) {
      inflater.inflate(R.menu.global, menu);
      showGlobalContextActionBar();
    }
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }


    return super.onOptionsItemSelected(item);
  }

  /**
   * Per the navigation drawer design guidelines, updates the action bar to show the global app
   * 'context', rather than just what's in the current screen.
   */
  private void showGlobalContextActionBar() {
    ActionBar actionBar = getActionBar();
    //actionBar.setDisplayShowTitleEnabled(true);
    //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setTitle(R.string.app_name);
  }

  private ActionBar getActionBar() {
    return ((ActionBarActivity) getActivity()).getSupportActionBar();
  }

  public void visMenu() {
    mDrawerLayout.openDrawer(mFragmentContainerView);
    navListView.requestFocus();
  }

  public void skjulMenu() {
    mDrawerLayout.closeDrawer(mFragmentContainerView);
  }


  class Navigation_adapter extends Basisadapter {
    public int LIVE_KANALER_INDEX;
    private final LayoutInflater layoutInflater;
    private AQuery aq;
    ArrayList<MenuElement> elem = new ArrayList<MenuElement>();

    @Override
    public int getCount() {
      return elem.size();
    }

    // Reelt skal ingen views genbruges til andre menupunkter, så vi giver dem alle en forskellig type
    @Override
    public int getViewTypeCount() {
      return elem.size();
    }

    // Reelt skal ingen views genbruges til andre menupunkter, så vi giver dem alle en forskellig type
    @Override
    public int getItemViewType(int position) {
      return position;
    }

    @Override
    public boolean isEnabled(int position) {
      return elem.get(position).type >= 2;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      return elem.get(position).layout;
    }

    class MenuElement {
      final int type;
      final String data;
      final View layout;
      public Runnable runnable;

      MenuElement(int type, String data, View layout) {
        this.type = type;
        this.data = data;
        this.layout = layout;
      }

      MenuElement(int type, String data, View layout, Runnable r) {
        this.type = type;
        this.data = data;
        this.layout = layout;
        runnable = r;
      }
    }


    private View aq(int nav_elem_soeg) {
      View v = layoutInflater.inflate(nav_elem_soeg, null);
      aq = new AQuery(v);
      return v;
    }


    private void tilføj(boolean b, int venstremenu_elem_soeg, Object o) {
      //me = new MenuElement()
    }

    public Navigation_adapter(final Context themedContext) {
      layoutInflater = (LayoutInflater) themedContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      //tilføj(true, R.layout.venstremenu_elem_soeg, null);
      //aq.id(R.id.tekst).typeface(App.skrift_normal);

      boolean gib = App.prefs.getBoolean("brugGibsonIVenstremenu", true);


      elem.add(new MenuElement(0, null, aq(R.layout.venstremenu_elem_soeg)));
      if (gib) aq.id(R.id.tekst).typeface(App.skrift_normal);

      elem.add(new MenuElement(3, null, aq(R.layout.venstremenu_elem_overskrift)));
      aq.id(R.id.tekst).text(Html.fromHtml("<b>Senest lyttede</b>"));
      if (gib) aq.typeface(App.skrift_normal);

      elem.add(new MenuElement(1, null, aq(R.layout.venstremenu_elem_adskiller_tynd)));

      elem.add(new MenuElement(3, null, aq(R.layout.venstremenu_elem_overskrift)));
      aq.id(R.id.tekst).text(Html.fromHtml("<b>Dine favoritprogrammer</b><br/>(2 nye udsendelser)"));
      if (gib) aq.typeface(App.skrift_normal);

      elem.add(new MenuElement(1, null, aq(R.layout.venstremenu_elem_adskiller_tynd)));

      elem.add(new MenuElement(3, null, aq(R.layout.venstremenu_elem_overskrift)));
      aq.id(R.id.tekst).text(Html.fromHtml("<b>Downloadede udsendelser</b> (13)"));
      if (gib) aq.typeface(App.skrift_normal);

      elem.add(new MenuElement(1, null, aq(R.layout.venstremenu_elem_adskiller_tyk)));

      elem.add(new MenuElement(3, null, aq(R.layout.venstremenu_elem_overskrift)));
      aq.id(R.id.tekst).text(Html.fromHtml("<b>Alle programmer A-Å"));
      if (gib) aq.typeface(App.skrift_normal);

      elem.add(new MenuElement(1, null, aq(R.layout.venstremenu_elem_adskiller_tynd)));

      LIVE_KANALER_INDEX = elem.size();
      elem.add(new MenuElement(4, null, aq(R.layout.venstremenu_elem_overskrift)));
      aq.id(R.id.tekst).text(Html.fromHtml("<b>Live kanaler</b>"));
      if (gib) aq.typeface(App.skrift_normal);

      elem.add(new MenuElement(4, null, aq(R.layout.venstremenu_elem_overskrift), new Runnable() {
        @Override
        public void run() {
          getActivity().startActivity(new Intent(getActivity(), Om_DRRadio_akt.class));
        }
      }));
      aq.id(R.id.tekst).text(Html.fromHtml("<b>Kontakt / info / om</b>"));
      if (gib) aq.typeface(App.skrift_normal);

      elem.add(new MenuElement(1, null, aq(R.layout.venstremenu_elem_udvikler)));
      aq.id(R.id.tekst).clicked(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          App.udvikling = !App.udvikling;
          ((CheckBox) v).setChecked(App.udvikling);
        }
      });

      elem.add(new MenuElement(1, null, aq(R.layout.venstremenu_elem_overskrift)));
      aq.id(R.id.tekst).text(Html.fromHtml("<br/><br/>(fjernes):<br/><br/><b>HØR LIVE RADIO</b>"));

      elem.add(new MenuElement(4, null, aq(R.layout.venstremenu_elem_overskrift), new Runnable() {
        @Override
        public void run() {
          startActivity(new Intent(getActivity(), Kanalvalg_v2_akt.class));
        }
      }));
      aq.id(R.id.tekst).text(Html.fromHtml("<b>Kanalvalg fra v2</b>"));


    }


    public void vælgMenu(FragmentActivity akt, int position) {
      MenuElement e = elem.get(position);

      if (e.runnable != null) {
        e.runnable.run();
        return;
      }

      Bundle b = new Bundle();
      Fragment f;

      if (e.type == 4) {
        f = new Kanaler_frag();
      } else if (e.type == 2) {
        f = new Kanalvalg_v2_frag();
        //b.putString(Kanal_frag.P_kode, e.data);
      } else {
        App.kortToast("Ikke implementeret");
        f = new Kanalvalg_v2_frag();
      }

      f.setArguments(b);
      FragmentManager fragmentManager = akt.getSupportFragmentManager();
      fragmentManager.beginTransaction().replace(R.id.indhold_frag, f).commit();
    }

  }
}
