package dk.dr.radio.skrald;

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
import android.widget.LinearLayout;

import com.androidquery.AQuery;

import java.util.ArrayList;

import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.akt.Kanal_frag;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

import static dk.dr.radio.akt.diverse.Basisaktivitet.putString;

public class Kanaler_frag_v2 extends Basisfragment implements ActionBar.TabListener {


  protected AQuery aq;
  protected View rod;
  private FaneAdapter faneAdapter;
  private ViewPager viewPager;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    rod = inflater.inflate(R.layout.skrald_kanaler_frag_v2, container, false);
    aq = new AQuery(rod);
    faneAdapter = new FaneAdapter(getActivity().getSupportFragmentManager());
    Bundle args = new Bundle();


    Log.d("xxx DRData.instans.stamdata.kanalkoder=" + DRData.instans.stamdata.kanalkoder);

    for (String kode : DRData.instans.stamdata.kanalkoder) {
      faneAdapter.tilføj(kode, new Kanal_frag(), putString(args, Kanal_frag.P_kode, kode));
    }

    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(faneAdapter);


    Hovedaktivitet a = (Hovedaktivitet) getActivity();
    //a.sætTitel("KanalViewpager");
    // Tilføj okoner for kanalerne til action bar.

    final Skrald_ScrollingTabContainerView mTabScrollView = (Skrald_ScrollingTabContainerView) aq.id(R.id.pager_title_strip3).getView();
    final ActionBar actionBar = a.getSupportActionBar();
    actionBar.removeAllTabs();

    for (String kode : DRData.instans.stamdata.kanalkoder) {
      Kanal k = DRData.instans.stamdata.kanalFraKode.get(kode);
      if (k == null) {
        Log.rapporterFejl(new Exception("kanalFraKode mangler kode " + kode));
        continue;
      }
      ActionBar.Tab fane = actionBar.newTab();
      ImageView iv = new ImageView(getActivity());
      //iv.setBackgroundColor(Color.BLUE);
      //new AQuery(iv).image("http://www.dr.dk/tjenester/iphone/radio/logos-no-dr/v2/P1.png");
      new AQuery(iv).image(k.kanallogo_resid);
      iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

      fane.setCustomView(iv);
      fane.setTabListener(this);
      actionBar.addTab(fane);

      LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
      Log.d("WWWWW " + lp.debug(""));
      /*
      //lp.setMargins(-10, -10, -10, -10);
      lp.width = 50;//ViewGroup.LayoutParams.MATCH_PARENT;
      lp.height = 25;//ViewGroup.LayoutParams.MATCH_PARENT;
      lp.gravity = Gravity.BOTTOM;
      iv.setLayoutParams(lp);
      Log.d(iv.getParent());
      Log.d(iv.getParent().getParent());
      */
/* Android 4 - faner er for brede!
02-16 23:29:18.424    3144-3144/dk.dr.radio.v3 D/DRRadio﹕ WWWWW LinearLayout.LayoutParams={width=wrap-content, height=wrap-content weight=0.0}
02-16 23:29:18.424    3144-3144/dk.dr.radio.v3 D/DRRadio﹕ com.android.internal.widget.ScrollingTabContainerView$TabView{b3371698 VFE...C. ..S...ID 0,0-0,0}
02-16 23:29:18.434    3144-3144/dk.dr.radio.v3 D/DRRadio﹕ android.widget.LinearLayout{b3370f08 V.ED.... ......I. 0,0-0,0}

Android 2 - faner er OK
02-16 22:31:30.079    1251-1251/dk.dr.radio.v3 D/DRRadio﹕ WWWWW LinearLayout.LayoutParams={width=wrap-content, height=wrap-content weight=0.0}
02-16 22:31:30.079    1251-1251/dk.dr.radio.v3 D/DRRadio﹕ android.support.v7.internal.widget.ScrollingTabContainerView$TabView@b7585bb0
02-16 22:31:30.079    1251-1251/dk.dr.radio.v3 D/DRRadio﹕ android.support.v7.internal.widget.LinearLayoutICS@b7585128

       */


    }

    viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        mTabScrollView.setTabSelected(position);
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

