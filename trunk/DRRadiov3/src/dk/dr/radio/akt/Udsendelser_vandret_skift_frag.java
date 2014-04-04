package dk.dr.radio.akt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;

import org.json.JSONObject;

import java.util.ArrayList;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class Udsendelser_vandret_skift_frag extends Basisfragment implements ViewPager.OnPageChangeListener {

  private ViewPager viewPager;

  private Udsendelse startudsendelse;
  private Programserie programserie;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  private Kanal kanal;
  private UdsendelserAdapter adapter;
  private int antalHentedeSendeplaner;
  private View pager_title_strip;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + programserie;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);

    kanal = DRData.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    startudsendelse = DRData.instans.udsendelseFraSlug.get(getArguments().getString(DRJson.Slug.name()));
    programserie = DRData.instans.programserieFraSlug.get(startudsendelse.programserieSlug);
    Log.d("onCreateView " + this + " viser " + programserie + " / " + startudsendelse);

    View rod = inflater.inflate(R.layout.udsendelser_vandret_skift_frag, container, false);

    viewPager = (ViewPager) rod.findViewById(R.id.pager);
    viewPager.setId(123);
    pager_title_strip = rod.findViewById(R.id.pager_title_strip);
    // Da ViewPager er indlejret i et fragment skal adapteren virke på den indlejrede (child)
    // fragmentmanageren - ikke på aktivitens (getFragmentManager)
    adapter = new UdsendelserAdapter(getChildFragmentManager());
    DRJson.opdateriDagIMorgenIGårDatoStr(App.serverCurrentTimeMillis());

    liste.clear(); // undgå dubletter i tilfælde af at onCreateView kaldes flere gange
    if (programserie==null) {
      liste.add(startudsendelse);
      viewPager.setAdapter(adapter);
      hentUdsendelser(0);
    } else {
      int n = programserie.findUdsendelseIndexFraSlug(startudsendelse.slug);
      if (n<0) {
        liste.add(startudsendelse);
        viewPager.setAdapter(adapter);
        hentUdsendelser(0);
      } else {
        liste.addAll(programserie.getUdsendelser());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(n);
      }
    }

/*    int n = programserie == null ? -1 : programserie.findUdsendelseIndexFraSlug(startudsendelse.slug);

    Log.d("programserie.udsendelser.indexOf(startudsendelse) = " + n);
    if (n >= 0) {
      liste.addAll(programserie.getUdsendelser());
      viewPager.setAdapter(adapter);
      viewPager.setCurrentItem(n);
    } else {
      liste.add(startudsendelse);
      viewPager.setAdapter(adapter);
      if (programserie == null) hentUdsendelser(0);
    }
    */
    vispager_title_strip();
    viewPager.setOnPageChangeListener(this);
    return rod;
  }

  private void vispager_title_strip() {
    pager_title_strip.setVisibility(
        !App.prefs.getBoolean("vispager_title_strip",false)?View.GONE :
        liste.size() > 1 ? View.VISIBLE : View.INVISIBLE);
  }


  private void opdaterListe() {
    if (viewPager == null) return;
    Udsendelse udsFør = liste.get(viewPager.getCurrentItem());
    liste.clear();
    liste.addAll(programserie.getUdsendelser());
    if (programserie.findUdsendelseIndexFraSlug(startudsendelse.slug)<0) {
      liste.add(startudsendelse);
      // hvis startudsendelse ikke er med i listen, så hent nogle flere, i håb om at komme hen til
      // startudsendelsen (hvis vi ikke allerede har forsøgt 7 gange)
      if (antalHentedeSendeplaner++ < 7) {
        hentUdsendelser(programserie.getUdsendelser().size());
      }
    }
    int nEft = programserie.findUdsendelseIndexFraSlug(udsFør.slug);
    if (nEft<0) nEft = liste.size()-1; // startudsendelsen
    adapter.notifyDataSetChanged();
    viewPager.setCurrentItem(nEft, false);
    vispager_title_strip();
/*
    if (programserie.getUdsendelser().size() < programserie.antalUdsendelser) {
      hentUdsendelser(programserie.getUdsendelser().size());
    }
    // hvis vi er sidst i listen og der er flere at hente, så hent nogle flere,
    // i håb om at komme hen til den aktuelle startudsendelse (hvis vi ikke allerede har forsøgt 7 gange)
    if (nEft==liste.size()-1 && nEft<programserie.antalUdsendelser-1 && antalHentedeSendeplaner++ < 7) {
      // da det element vi viser lige nu
      hentUdsendelser(programserie.getUdsendelser().size()-1);
    }
*/
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
    final String url = "http://www.dr.dk/tjenester/mu-apps/series/" + startudsendelse.programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
    Log.d("XXX url=" + url);

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (uændret) return;
        Log.d("fikSvar(" + fraCache + " " + url);
        if (json != null && !"null".equals(json)) {
          JSONObject data = new JSONObject(json);
          if (offset == 0) {
            programserie = DRJson.parsProgramserie(data, programserie);
            DRData.instans.programserieFraSlug.put(startudsendelse.programserieSlug, programserie);
          }
          programserie.tilføjUdsendelser(offset, DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans));
          //programserie.tilføjUdsendelser(Arrays.asList(startudsendelse));
          opdaterListe();
        }
      }
    }).setTag(this);
    App.volleyRequestQueue.add(req);
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
  }

  @Override
  public void onPageSelected(int position) {
    if (programserie!=null && position==liste.size()-1 && antalHentedeSendeplaner++ < 7) { // Hent flere udsendelser
      hentUdsendelser(programserie.getUdsendelser()==null?0:programserie.getUdsendelser().size());
    }
  }

  @Override
  public void onPageScrollStateChanged(int state) {
  }

//  public class UdsendelserAdapter extends FragmentPagerAdapter {
  public class UdsendelserAdapter extends FragmentStatePagerAdapter {

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

    @Override
    public CharSequence getPageTitle(int position) {
      Udsendelse u = liste.get(position);
      String dato = DRJson.datoformat.format(u.startTid);
      if (dato.equals(DRJson.iDagDatoStr)) dato = "i dag";
      else if (dato.equals(DRJson.iMorgenDatoStr)) dato = "i morgen";
      else if (dato.equals(DRJson.iGårDatoStr)) dato = "i går";
      return dato;
      //return DRJson.datoformat.format(u.startTid);
      //return ""+u.episodeIProgramserie+" "+u.slug;
    }
  }
}

