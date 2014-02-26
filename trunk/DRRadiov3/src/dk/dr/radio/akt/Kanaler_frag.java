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

import com.androidquery.AQuery;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.akt.diverse.PagerSlidingTabStrip;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanaler_frag extends Basisfragment implements ActionBar.TabListener {

  protected AQuery aq;
  protected View rod;
  private ViewPager viewPager;
  private ArrayList<Kanal> kanaler = new ArrayList<Kanal>();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    rod = inflater.inflate(R.layout.kanaler_frag_v3, container, false);
    aq = new AQuery(rod);

    for (Kanal k : DRData.instans.stamdata.kanaler) {
      if (!k.p4underkanal) kanaler.add(k);
    }

    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(new KanalAdapter(getActivity().getSupportFragmentManager()));


    PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) rod.findViewById(R.id.tabs);
    tabs.setViewPager(viewPager);

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


  public class KanalAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

    public KanalAdapter(FragmentManager fm) {
      super(fm);
    }

    //@Override
    //public float getPageWidth(int position) { return(0.9f); }

    @Override
    public Fragment getItem(int position) {
      Kanal_frag f = new Kanal_frag();
      Bundle b = new Bundle();
      b.putString(Kanal_frag.P_kode, kanaler.get(position).kode);
      f.setArguments(b);
      return f;
    }

    @Override
    public int getCount() {
      return kanaler.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return kanaler.get(position).navn;
    }


    @Override
    public int getPageIconResId(int position) {
      return kanaler.get(position).kanallogo_resid;
    }
  }
}

