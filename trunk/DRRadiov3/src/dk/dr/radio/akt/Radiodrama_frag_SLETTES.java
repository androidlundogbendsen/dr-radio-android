package dk.dr.radio.akt;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.PagerSlidingTabStrip;
import dk.dr.radio.v3.R;

public class Radiodrama_frag_SLETTES extends Basisfragment implements ViewPager.OnPageChangeListener, Runnable {

  private static final String INDEX = "Radiodrama_frag_index";
  private ViewPager viewPager;
  private KanalAdapter adapter;
  private ArrayList<Programserie> liste = new ArrayList<Programserie>();


  private Venstremenu_frag venstremenuFrag;
  private PagerSlidingTabStrip kanalfaneblade;
  private int viewPagerScrollState;


  @Override
  public void run() {
    liste.clear();
    if (DRData.instans.dramaOgBog.liste!=null) liste.addAll(DRData.instans.dramaOgBog.liste);
    if (adapter != null) {
      if (viewPager.getCurrentItem() >= liste.size()) {
        viewPager.setCurrentItem(0);
      }
      kanalfaneblade.notifyDataSetChanged();
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);
    View rod = inflater.inflate(R.layout.kanaler_frag, container, false);

    run();
    adapter = new KanalAdapter(getChildFragmentManager());
    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(adapter);
    viewPager.setPageTransformer(false, new ViewPager.PageTransformer() {
      @Override
      public void transformPage(View page, float position) {
        float normalizedposition = Math.abs(Math.abs(position) - 1);
        page.setAlpha(normalizedposition);

        page.setScaleX(normalizedposition / 2 + 0.5f);
        page.setScaleY(normalizedposition / 2 + 0.5f);

        page.setRotationY(position * -30);
      }
    });

    venstremenuFrag = (Venstremenu_frag) getFragmentManager().findFragmentById(R.id.venstremenu_frag);

    kanalfaneblade = (PagerSlidingTabStrip) rod.findViewById(R.id.tabs);
    kanalfaneblade.setViewPager(viewPager);
    if (savedInstanceState == null) {
      // gendan position fra sidste gang vi var herinde
      viewPager.setCurrentItem(App.prefs.getInt(INDEX, 0));
    }
    kanalfaneblade.setOnPageChangeListener(this);
    kanalfaneblade.setIndicatorHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
    kanalfaneblade.setIndicatorColor(0xFF666666);
    kanalfaneblade.setUnderlineColor(0x1A000000);
    kanalfaneblade.setDividerColor(0x1A000000);

    DRData.instans.dramaOgBog.observatører.add(this);

    return rod;
  }

  @Override
  public void onDestroyView() {
    viewPager.setAdapter(null); // forårsager crash? - men nødvendig for at undgå https://mint.splunk.com/dashboard/project/cd78aa05/errors/2151178610
    viewPager = null;
    adapter = null;
    kanalfaneblade = null;
    DRData.instans.dramaOgBog.observatører.remove(this);
    super.onDestroyView();
  }

  @Override
  public void onPageSelected(int position) {
    Log.d("onPageSelected( " + position);
    // Husk foretrukken getKanal
    App.prefs.edit().putInt(INDEX, position).commit();
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    Log.d("onPageScrollStateChanged( " + state);
    viewPagerScrollState = state;
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    if (App.fejlsøgning) Log.d("onPageScrolled( " + position + " " + positionOffset + " " + positionOffsetPixels);
    // Hvis vi er på 0'te side og der trækkes mod højre kan viewpageren ikke komme længere og offsetPixels vil være 0,
    if (position == 0 && positionOffsetPixels == 0 && viewPagerScrollState == ViewPager.SCROLL_STATE_DRAGGING) {
      venstremenuFrag.visMenu();
    }
  }

  public class KanalAdapter extends FragmentPagerAdapter {
    //public class KanalAdapter extends FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

    public KanalAdapter(FragmentManager fm) {
      super(fm);
    }

    //@Override
    //public float getPageWidth(int position) { return(0.9f); }

    @Override
    public Basisfragment getItem(int position) {
      Basisfragment f = new Programserie_frag();
      Bundle b = new Bundle();
      b.putString(DRJson.SeriesSlug.name(), liste.get(position).slug);
      f.setArguments(b);
      return f;
    }

    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return liste.get(position).titel;
    }

/*
    @Override
    public int getPageIconResId(int position) {
      return 0; //kanaler.get(position).kanallogo_resid;
    }
    @Override
    public String getPageContentDescription(int position) {
      return kanaler.get(position).titel;
    }
*/
  }
}

