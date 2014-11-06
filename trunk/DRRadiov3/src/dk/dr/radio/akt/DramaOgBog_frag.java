package dk.dr.radio.akt;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;

import java.util.ArrayList;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.CirclePageIndicator;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class DramaOgBog_frag extends Basisfragment implements Runnable {

  private static final String INDEX = DramaOgBog_frag.class.getName();
  private ViewPager viewPager;
  private KanalAdapter adapter;
  private ArrayList<Programserie> liste = new ArrayList<Programserie>();
  private CirclePageIndicator mIndicator;


  @Override
  public void run() {
    liste.clear();
    if (DRData.instans.radiodrama.liste!=null) liste.addAll(DRData.instans.radiodrama.liste);
    if (adapter != null) {
      if (viewPager.getCurrentItem() >= liste.size()) {
        viewPager.setCurrentItem(0);
      }
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);
    View rod = inflater.inflate(R.layout.drama_og_bog_frag, container, false);

    run();
    adapter = new KanalAdapter(getChildFragmentManager());
    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(adapter);
    mIndicator = (CirclePageIndicator)rod.findViewById(R.id.indicator);
    mIndicator.setViewPager(viewPager);
    if (savedInstanceState == null) {
      // gendan position fra sidste gang vi var herinde
      viewPager.setCurrentItem(App.prefs.getInt(INDEX, 0));
    }
    DRData.instans.radiodrama.observatører.add(this);

    return rod;
  }

  @Override
  public void onDestroyView() {
    viewPager.setAdapter(null); // forårsager crash? - men nødvendig for at undgå https://mint.splunk.com/dashboard/project/cd78aa05/errors/2151178610
    viewPager = null;
    adapter = null;
    mIndicator = null;
    DRData.instans.radiodrama.observatører.remove(this);
    super.onDestroyView();
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
      Basisfragment f = new MitProgramserie_frag();
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
  }

  public static class MitProgramserie_frag extends Basisfragment {
    private String programserieSlug;
    private Programserie programserie;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
      Log.d("onCreateView " + this + " viser " + programserieSlug);
      View rod = inflater.inflate(R.layout.udsendelse_elem0_inkl_billede_titel, container, false);
      AQuery aq = new AQuery(rod);
      programserie = DRData.instans.programserieFraSlug.get(programserieSlug);

      int br = 400; //XXXbestemBilledebredde(listView, (View) aq.id(R.id.billede).getView().getParent(), 100);
      int hø = br * højde9 / bredde16;
      String burl = Basisfragment.skalérBillede(programserie, br, hø);
      aq.width(br, false).height(hø, false).image(burl, true, true, br, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);

      aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.titel);
      aq.id(R.id.beskrivelse).text(programserie.beskrivelse).typeface(App.skrift_georgia);

      Log.registrérTestet("Visning af "+INDEX, "ja");
      //udvikling_checkDrSkrifter(rod, this + " rod");
      return rod;
    }

  }
}

