package dk.dr.radio.akt_v3;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

import dk.dr.radio.R;


public class ListeAkt extends BasisAktivitet {
  VariabelFaneAdapter faneAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.v3_liste_akt);

    Bundle args = getIntent().getExtras();
    if (args == null) args = new Bundle();
    ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
    ViewPager viewPager0 = (ViewPager) findViewById(R.id.pager0);

    faneAdapter = new VariabelFaneAdapter(getSupportFragmentManager());
    faneAdapter.tilføj("P1", new ListeFrag(), putString(args, ListeFrag.P_url, "http://www.dr.dk/tjenester/mu-apps/schedule/P1D/0"));
    if (viewPager0 != null) {
      viewPager0.setAdapter(faneAdapter);
      faneAdapter = new VariabelFaneAdapter(getSupportFragmentManager());
    }
    //faneAdapter.tilføj("fremtidige", new MineOpkaldFrag(), putString(args, MineOpkaldFrag.P_url, "1"));
    faneAdapter.tilføj("P3", new ListeFrag(), putString(args, ListeFrag.P_url, "http://www.dr.dk/tjenester/mu-apps/schedule/P3/0"));
    viewPager.setAdapter(faneAdapter);
  }


  public static class VariabelFaneAdapter extends FragmentPagerAdapter {
    ArrayList<String> faneNavne = new ArrayList<String>();
    ArrayList<Fragment> faneFragmenter = new ArrayList<Fragment>();

    public VariabelFaneAdapter(FragmentManager fm) {
      super(fm);
    }

    //@Override
    //public float getPageWidth(int position) { return(0.50f); }

    @Override
    public Fragment getItem(int position) {
      return faneFragmenter.get(position);
    }

    @Override
    public int getCount() {
      return faneNavne.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return faneNavne.get(position);
    }

    public void tilføj(String opgave, Fragment opgaveFrag, Bundle args) {
      faneNavne.add(opgave);
      opgaveFrag.setArguments(args);
      faneFragmenter.add(opgaveFrag);
    }
  }

}
