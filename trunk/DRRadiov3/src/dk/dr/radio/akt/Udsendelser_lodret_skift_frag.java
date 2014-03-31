package dk.dr.radio.akt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.Request;

import org.json.JSONObject;

import java.util.ArrayList;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.VerticalViewPager;

public class Udsendelser_lodret_skift_frag extends Basisfragment {

  private VerticalViewPager viewPager;

  private Udsendelse udsendelse;
  private Programserie programserie;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  private Kanal kanal;
  private UdsendelserAdapter adapter;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + programserie;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);

    kanal = DRData.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    udsendelse = DRData.instans.udsendelseFraSlug.get(getArguments().getString(DRJson.Slug.name()));
    programserie = DRData.instans.programserieFraSlug.get(udsendelse.programserieSlug);
    Log.d("onCreateView " + this + " viser " + programserie + " / "+udsendelse);

    viewPager = new VerticalViewPager(getActivity()) {
      @Override
      protected boolean canScroll(View v, boolean checkV, int dy, int x, int y) {
        if(v != this && v instanceof ListView) {
          ListView lv = (ListView) v;
          if (dy<0 && lv.getLastVisiblePosition() == lv.getAdapter().getCount() -1 &&
              lv.getChildAt(lv.getChildCount() - 1).getBottom() <= lv.getHeight())
          {
            //It is scrolled all the way down here
            return false;
          }
          if (dy>0 && lv.getFirstVisiblePosition() == 0 &&
              lv.getChildAt(0).getTop() >= 0)
          {
            //It is scrolled all the way up here
            return false;

          }
          return true;
        }
        return super.canScroll(v, checkV, dy, x, y);
      }
    };
    viewPager.setId(123);
    // Da ViewPager er indlejret i et fragment skal adapteren virke på den indlejrede (child)
    // fragmentmanageren - ikke på aktivitens (getFragmentManager)
    adapter = new UdsendelserAdapter(getChildFragmentManager());

    int n = programserie==null?-1:findUdsendelseIndexFraSlug(udsendelse.slug, programserie.getUdsendelser());

    Log.d("programserie.udsendelser.indexOf(udsendelse) = "+n);
    if (n>=0) {
      liste.addAll(programserie.getUdsendelser());
      viewPager.setAdapter(adapter);
      viewPager.setCurrentItem(n);
    } else {
      liste.add(udsendelse);
      viewPager.setAdapter(adapter);
      if (programserie==null) hentUdsendelser(0);
    }
    return viewPager;
  }

  private int findUdsendelseIndexFraSlug(String slug, ArrayList<Udsendelse> udsendelser) {
    int n = -1;
    if (udsendelser!=null) {
      for (int i=0; i<udsendelser.size(); i++) {
        if (slug.equals(udsendelser.get(i).slug)) n = i;
      }
    }
    return n;
  }


  private void opdaterListe() {
    if (viewPager==null) return;
    Udsendelse udsFør = liste.get(viewPager.getCurrentItem());

    liste.clear();
    liste.addAll(programserie.getUdsendelser());
    Log.d("programserie.udsendelser. = " + programserie.getUdsendelser());
    int nEft = findUdsendelseIndexFraSlug(udsFør.slug, liste);
    Log.d("programserie nEft== "+nEft);
    if (nEft>=0) {
      adapter.notifyDataSetChanged();
      viewPager.setCurrentItem(nEft, false);
    } else {
      liste.add(udsFør);
      adapter.notifyDataSetChanged();
      viewPager.setCurrentItem(liste.size() - 1, false);
      if (programserie.getUdsendelser().size() < programserie.antalUdsendelser) {
        hentUdsendelser(programserie.getUdsendelser().size());
      }
    }
  }

  @Override
  public void onDestroyView() {
    viewPager = null;
    adapter = null;
    super.onDestroyView();
  }

  private void hentUdsendelser(final int offset) {
    // svarer til v3_programserie.json
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
    final String url = "http://www.dr.dk/tjenester/mu-apps/series/" + udsendelse.programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
    Log.d("XXX url=" + url);

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        Log.d("fikSvar(" + fraCache + " " + url);
        if (json != null && !"null".equals(json)) {
          if (uændret) return;
          JSONObject data = new JSONObject(json);
          if (offset == 0) {
            programserie = DRJson.parsProgramserie(data, programserie);
            programserie.tilføjUdsendelser(DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans));
            DRData.instans.programserieFraSlug.put(udsendelse.programserieSlug, programserie);
          } else {
            ArrayList<Udsendelse> flereUdsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            if (flereUdsendelser.size()==0) return; // Ingen opdatering
            programserie.getUdsendelser().addAll(flereUdsendelser);
          }
          opdaterListe();
        }
      }
    }).setTag(this);
    App.volleyRequestQueue.add(req);
  }

  public class UdsendelserAdapter extends FragmentPagerAdapter {

    public UdsendelserAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Udsendelse_frag getItem(int position) {
      Udsendelse u = liste.get(position);
      Udsendelse_frag f = new Udsendelse_frag();
      f.setArguments(new Intent()
          .putExtra(Kanal_frag.P_kode, kanal.kode)
          .putExtra(Udsendelse_frag.AKTUEL_UDSENDELSE_SLUG, getArguments().getString(Udsendelse_frag.AKTUEL_UDSENDELSE_SLUG))
          .putExtra(DRJson.Slug.name(), u.slug)
          .getExtras());
      return f;
    }

    @Override
    public int getCount() {
      return liste.size();
    }
  }
}

