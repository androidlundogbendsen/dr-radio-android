package dk.dr.radio.akt;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.androidquery.AQuery;

import java.util.ArrayList;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.ui.Basisfragment;
import dk.dr.radio.v3.R;

import static dk.dr.radio.diverse.ui.Basisaktivitet.putString;

public class Kanaler_frag extends Basisfragment implements ActionBar.TabListener {


  protected AQuery aq;
  protected View rod;
  private FaneAdapter faneAdapter;
  private ViewPager viewPager;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    rod = inflater.inflate(R.layout.kanaler_frag, container, false);
    aq = new AQuery(rod);
    faneAdapter = new FaneAdapter(getActivity().getSupportFragmentManager());
    Bundle args = new Bundle();


    Log.d("xxx DRData.instans.stamdata.kanalkoder=" + DRData.instans.stamdata.kanalkoder);

    for (String kode : DRData.instans.stamdata.kanalkoder) {
      faneAdapter.tilføj(kode, new Kanalvisning_frag(), putString(args, Kanalvisning_frag.P_kode, kode));
    }

    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(faneAdapter);


    Hovedaktivitet a = (Hovedaktivitet) getActivity();
    a.sætTitel("KanalViewpager");
    final ActionBar actionBar = a.getSupportActionBar();
    actionBar.removeAllTabs();
    // Tilføj okoner for kanalerne til action bar.
    for (String kode : DRData.instans.stamdata.kanalkoder) {
      Kanal k = DRData.instans.stamdata.kanalFraKode.get(kode);
      if (k == null) {
        Log.rapporterFejl(new Exception("kanalFraKode mangler kode " + kode));
        continue;
      }
      ActionBar.Tab fane = actionBar.newTab();
      /*
        if (k.kanalappendis_resid > 0) fane.setIcon(k.kanalappendis_resid);
        else fane.setText(k.navn); // Intet ikon, så vis tekst - burde ikke ske
       */
      if (k.logoUrl2.length() > 0) {
        ImageView iv = new ImageView(getActivity());
        //new AQuery(iv).image("http://www.dr.dk/tjenester/iphone/radio/logos-no-dr/v2/P1.png");
        new AQuery(iv).image(k.logoUrl2);
        fane.setCustomView(iv);
      } else fane.setText(k.navn); // Intet ikon, så vis tekst - burde ikke ske
      fane.setTabListener(this);
      actionBar.addTab(fane);
    }
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        actionBar.setSelectedNavigationItem(position);
      }
    });


    return rod;
  }


  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);


  }

  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    if (viewPager == null) return;
    viewPager.setCurrentItem(tab.getPosition());
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }


  public static class FaneAdapter extends FragmentPagerAdapter {
    ArrayList<String> faneNavne = new ArrayList<String>();
    ArrayList<Fragment> faneFragmenter = new ArrayList<Fragment>();

    public FaneAdapter(FragmentManager fm) {
      super(fm);
    }

    //@Override
    //public float getPageWidth(int position) { return(0.9f); }

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

    public void tilføj(String opgave, Fragment fragment, Bundle args) {
      faneNavne.add(opgave);
      fragment.setArguments(args);
      faneFragmenter.add(fragment);
    }
  }
}

