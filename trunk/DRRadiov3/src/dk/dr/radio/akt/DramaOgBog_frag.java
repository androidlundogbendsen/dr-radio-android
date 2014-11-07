package dk.dr.radio.akt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
  private KarruselAdapter adapter;
  private ArrayList<Programserie> liste = new ArrayList<Programserie>();
  private CirclePageIndicator indicator;


  @Override
  public void run() {
    liste.clear();
    if (DRData.instans.dramaOgBog.liste!=null) liste.addAll(DRData.instans.dramaOgBog.liste);
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
    adapter = new KarruselAdapter(getChildFragmentManager());
    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setAdapter(adapter);
    viewPager.getLayoutParams().height = billedeHø; // Viewpageren skal fylde præcist ét billede i højden
    indicator = (CirclePageIndicator)rod.findViewById(R.id.indicator);
    indicator.setViewPager(viewPager);
    final float density = getResources().getDisplayMetrics().density;
    indicator.setRadius(5 * density);
    indicator.setPageColor(Color.BLACK);
    indicator.setFillColor(App.color.blå);
    indicator.setStrokeColor(0);
    indicator.setStrokeWidth(0);
    if (savedInstanceState == null) {
      // gendan position fra sidste gang vi var herinde
      viewPager.setCurrentItem(App.prefs.getInt(INDEX, 0));
    }
    DRData.instans.dramaOgBog.observatører.add(this);
    viewPager.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction()==MotionEvent.ACTION_DOWN || event.getAction()==MotionEvent.ACTION_UP) {
          App.forgrundstråd.removeCallbacks(skiftTilNæsteIKarrusellen);
          App.forgrundstråd.postDelayed(skiftTilNæsteIKarrusellen, 10000);
        }
        return false;
      }
    });
    App.forgrundstråd.postDelayed(skiftTilNæsteIKarrusellen, 10000);

    return rod;
  }

  Runnable skiftTilNæsteIKarrusellen = new Runnable() {
    @Override
    public void run() {
      if (viewPager==null) return;
      int n = (viewPager.getCurrentItem() + 1) % liste.size();
      viewPager.setCurrentItem(n, true);
      App.forgrundstråd.removeCallbacks(skiftTilNæsteIKarrusellen);
      App.forgrundstråd.postDelayed(skiftTilNæsteIKarrusellen, 5000);
    }
  };

  @Override
  public void onDestroyView() {
    viewPager = null;
    adapter = null;
    indicator = null;
    DRData.instans.dramaOgBog.observatører.remove(this);
    super.onDestroyView();
  }

  public class KarruselAdapter extends FragmentPagerAdapter {
    public KarruselAdapter(FragmentManager fm) {
      super(fm);
    }
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public float getPageWidth(int position) {
      return halvbreddebilleder?0.5f : 1;
    }

    @Override
    public Basisfragment getItem(int position) {
      Basisfragment f = new KarruselFrag();
      Bundle b = new Bundle();
      b.putString(DRJson.SeriesSlug.name(), liste.get(position).slug);
      f.setArguments(b);
      return f;
    }
  }

  public static class KarruselFrag extends Basisfragment implements View.OnClickListener {
    private String programserieSlug;
    private Programserie programserie;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
      Log.d("onCreateView " + this + " viser " + programserieSlug);
      View rod = inflater.inflate(R.layout.kanal_elem0_inkl_billede_titel, container, false);
      programserie = DRData.instans.programserieFraSlug.get(programserieSlug);

      String burl = Basisfragment.skalérBillede(programserie);
      AQuery aq = new AQuery(rod);
      aq.clicked(this);
      aq.id(R.id.billede)
      //.width(billedeBr, false).height(billedeHø, false)
          .image(burl, true, true, billedeBr, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);

      aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.undertitel);
      aq.id(R.id.lige_nu).text(programserie.titel.toUpperCase()).typeface(App.skrift_gibson);

      Log.registrérTestet("Visning af "+INDEX, "ja");
      //udvikling_checkDrSkrifter(rod, this + " rod");
      return rod;
    }

    @Override
    public void onClick(View v) {
      Fragment f = new Programserie_frag();
      f.setArguments(new Intent()
          .putExtra(DRJson.SeriesSlug.name(), programserieSlug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();

    }
  }
}

