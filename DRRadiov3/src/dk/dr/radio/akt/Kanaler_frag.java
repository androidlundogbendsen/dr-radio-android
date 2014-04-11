package dk.dr.radio.akt;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.PagerSlidingTabStrip;
import dk.dr.radio.v3.R;

public class Kanaler_frag extends Basisfragment implements ViewPager.OnPageChangeListener {

  private ViewPager viewPager;
  private ArrayList<Kanal> kanaler = new ArrayList<Kanal>();


  private Venstremenu_frag venstremenuFrag;
  private int mForgåendePosition = -1;
  private int mNuværendePosition = -1;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    for (Kanal k : DRData.instans.grunddata.kanaler) {
      if (!k.p4underkanal) kanaler.add(k);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);
    View rod = inflater.inflate(R.layout.kanaler_frag, container, false);

    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(new KanalAdapter(getChildFragmentManager()));

    venstremenuFrag = (Venstremenu_frag) getFragmentManager().findFragmentById(R.id.venstremenu_frag);


    PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) rod.findViewById(R.id.tabs);
    tabs.setViewPager(viewPager);
    if (savedInstanceState == null) {
      int kanalindex = kanaler.indexOf(DRData.instans.afspiller.getLydkilde().getKanal());
      if (kanalindex == -1) kanalindex = 3; // Hvis vi ikke rammer nogen af de overordnede kanaler, så er det P4
      viewPager.setCurrentItem(kanalindex);
    }
    tabs.setOnPageChangeListener(this);

    return rod;
  }


  @Override
  public void onDestroyView() {
    viewPager = null;
    super.onDestroyView();
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    //Log.d("onPageScrolled( " + position + " " + positionOffset);
    mNuværendePosition = position;
  }


  @Override
  public void onPageSelected(int position) {
    // Husk foretrukken getKanal
    App.prefs.edit().putString(App.FORETRUKKEN_KANAL, kanaler.get(position).kode).commit();
//    DRData.instans.aktuelKanal = kanaler.get(position);
    Log.d("onPageSelected( " + position);

    mForgåendePosition = position;
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    Log.d("onPageScrollStateChanged( " + state);

    Log.d("mNuværendePosition " + mNuværendePosition + ", mForgåendePosition " + mForgåendePosition);

    if (state == 1 && mForgåendePosition == 0 && mNuværendePosition == 0) {
      //visSlideMenu = (mForgåendePosition == 0 && mNuværendePosition == 0) ;
      venstremenuFrag.visMenu();
      mForgåendePosition = -1;
      mNuværendePosition = -1;
    }
    if (state == 0 && (mForgåendePosition == -1 && mNuværendePosition == 0)) {
      //visSlideMenu = (mForgåendePosition == -1 && mNuværendePosition == 0 ) ;
      venstremenuFrag.visMenu();
    }
    
    /*if (visSlideMenu){
      venstremenuFrag.visMenu();
    	visSlideMenu = false;
    }*/


  }


  public class KanalAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {
    //public class KanalAdapter extends FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

    public KanalAdapter(FragmentManager fm) {
      super(fm);
    }

    //@Override
    //public float getPageWidth(int position) { return(0.9f); }

    @Override
    public Basisfragment getItem(int position) {
      Basisfragment f = position < kanaler.size() - 1 ? new Kanal_frag() : new Kanal_nyheder_frag();
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

