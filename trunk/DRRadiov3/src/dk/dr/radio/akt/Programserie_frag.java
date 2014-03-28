package dk.dr.radio.akt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.androidquery.AQuery;

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
import dk.dr.radio.v3.R;

public class Programserie_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener {

  private ListView listView;
  private String programserieSlug;
  private Programserie programserie;
  private Kanal kanal;
  private View rod;
  private int antalHentedeSendeplaner;
  private CheckBox favorit;
  private AQuery aq;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + programserie;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
    Log.d("onCreateView " + this + " viser " + programserieSlug);
    kanal = DRData.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));

    programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
    if (programserie == null) {
      hentUdsendelser(0); // hent kun en frisk udgave hvis vi ikke allerede har en
    }

    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);

    udvikling_checkDrSkrifter(rod, this + " rod");
    return rod;
  }

  @Override
  public void onDestroyView() {
    App.volleyRequestQueue.cancelAll(this);
    super.onDestroyView();
  }

  private void hentUdsendelser(final int offset) {
    // svarer til v3_programserie.json
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
    final String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
    Log.d("XXX url=" + url);

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (uændret) return;
        if (json != null && !"null".equals(json)) {
          JSONObject data = new JSONObject(json);
          if (offset == 0) {
            programserie = DRJson.parsProgramserie(data);
            programserie.udsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
          } else if (fraCache) {
            return; // TODO brug cache til de følgende udsendelser (lidt kompliceret)
          } else {
            ArrayList<Udsendelse> flereUdsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            programserie.udsendelser.addAll(flereUdsendelser);
          }
          adapter.notifyDataSetChanged();
          return;
        }
        new AQuery(rod).id(R.id.tom).text("Siden kunne ikke vises");
      }

      @Override
      protected void fikFejl(VolleyError error) {
        super.fikFejl(error);
        aq.id(R.id.tom).text("Siden kunne ikke vises");
      }
    }).setTag(this);
    App.volleyRequestQueue.add(req);
  }

  @Override
  public void onClick(View v) {
    DRData.instans.favoritter.sætFavorit(programserieSlug, programserie.antalUdsendelser, favorit.isChecked());
  }


  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public Udsendelse udsendelse;
    public AQuery aq;
    public View stiplet_linje;
    public TextView titel;
    public TextView varighed;
  }

  static final int TOP = 0;
  static final int UDSENDELSE = 1;
  static final int TIDLIGERE = 2;

  static final int[] layoutFraType = {
      R.layout.programserie_elem0_top,
      R.layout.programserie_elem1_udsendelse,
      R.layout.kanal_elem_tidligere_senere,
  };

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      if (programserie == null) return 0;
      if (programserie.antalUdsendelser == programserie.udsendelser.size()) return programserie.udsendelser.size() + 1;
      return programserie.udsendelser.size() + 2; // Vis 'tidligere'-listeelement
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public int getItemViewType(int position) {
      if (position == 0 || programserie == null) return TOP;
      if (position == programserie.udsendelser.size() + 1) return TIDLIGERE;
      return UDSENDELSE;
    }

    @Override
    public boolean isEnabled(int position) {
      return getItemViewType(position) > 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Viewholder vh;
      int type = getItemViewType(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(layoutFraType[type], parent, false);
        vh = new Viewholder();
        AQuery aq = vh.aq = new AQuery(v);
        v.setTag(vh);
        if (type == TOP) {
          int br = bestemBilledebredde(listView, (View) aq.id(R.id.billede).getView().getParent(), 50);
          int hø = br * højde9 / bredde16;
          String burl = skalérSlugBilledeUrl(programserie.slug, br, hø);
          aq.width(br, false).height(hø, false).image(burl, true, true, br, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          if (kanal == null) aq.id(R.id.logo).gone();
          else aq.id(R.id.logo).image(kanal.kanallogo_resid);
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.titel);
          String tekst = "ALLE UDSENDELSER";
          aq.id(R.id.alle_udsendelser).typeface(App.skrift_gibson).text(lavFedSkriftTil(tekst + " (" + programserie.antalUdsendelser + ")", tekst.length()));
          aq.id(R.id.beskrivelse).text(programserie.beskrivelse).typeface(App.skrift_georgia);
          Linkify.addLinks(aq.getTextView(), Linkify.WEB_URLS);
          favorit = aq.id(R.id.favorit).clicked(Programserie_frag.this).getCheckBox();
          favorit.setChecked(DRData.instans.favoritter.erFavorit(programserieSlug));

        } else { // if (type == UDSENDELSE eller TIDLIGERE) {
          vh.titel = aq.id(R.id.titel).typeface(App.skrift_gibson).getTextView();
          vh.varighed = aq.id(R.id.varighed).typeface(App.skrift_gibson).getTextView();
          vh.stiplet_linje = aq.id(R.id.stiplet_linje).getView();
        }
      } else {
        vh = (Viewholder) v.getTag();
      }

      // Opdatér viewholderens data
      if (type == UDSENDELSE) {
        Udsendelse u = programserie.udsendelser.get(position - 1);
        vh.udsendelse = u;
        //vh.stiplet_linje.setVisibility(position > 1 ? View.VISIBLE : View.INVISIBLE); // Første stiplede linje væk
        vh.stiplet_linje.setBackgroundResource(position > 1 ? R.drawable.stiplet_linje : R.drawable.linje); // Første stiplede linje er fuld

        //vh.titel.setText(Html.fromHtml("<b>" + u.titel + "</b>&nbsp; - " + DRJson.datoformat.format(u.startTid)));
        vh.titel.setText(lavFedSkriftTil(u.titel + " - " + DRJson.datoformat.format(u.startTid), u.titel.length()));
        //Log.d("DRJson.datoformat.format(u.startTid)=" + DRJson.datoformat.format(u.startTid));

        //String txt = u.getKanal().navn + ", " + ((u.slutTid.getTime() - u.startTid.getTime())/1000/60 + " MIN");
        String txt = ""; //u.getKanal().navn;
        int varighed = (int) ((u.slutTid.getTime() - u.startTid.getTime()) / 1000 / 60);
        if (varighed > 0) {
          //txt += ", ";
          int timer = varighed / 60;
          if (timer > 1) txt += timer + " TIMER";
          else if (timer == 1) txt += timer + " TIME";
          int min = varighed % 60;
          if (min > 0 && timer > 0) txt += " OG ";
          if (min > 1) txt += min + " MINUTTER";
          else if (min == 1) txt += timer + " MINUT";
        }
        //Log.d("txt=" + txt);
        vh.varighed.setText(txt);
        vh.varighed.setVisibility(txt.length() > 0 ? View.VISIBLE : View.GONE);
      } else if (type == TIDLIGERE) {
        if (antalHentedeSendeplaner++ < 7) {
          vh.aq.id(R.id.progressBar).visible();   // De første 7 henter vi bare for brugeren
          vh.titel.setVisibility(View.VISIBLE);
          hentUdsendelser(programserie.udsendelser.size());
        } else {
          vh.aq.id(R.id.progressBar).invisible(); // Derefter må brugeren gøre det manuelt
          vh.titel.setVisibility(View.VISIBLE);
        }

      }
      udvikling_checkDrSkrifter(v, this + " position " + position);
      return v;
    }
  };


  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    if (position == 0) return;
    if (position <= programserie.udsendelser.size()) {
      Udsendelse udsendelse = programserie.udsendelser.get(position - 1);
      Fragment f = new Udsendelse_frag();
      f.setArguments(new Intent()
          .putExtra(Udsendelse_frag.BLOKER_VIDERE_NAVIGERING, true)
          .putExtra(P_kode, kanal == null ? null : kanal.kode)
          .putExtra(DRJson.Slug.name(), udsendelse.slug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();
      return;
    }

    hentUdsendelser(programserie.udsendelser.size());
    v.findViewById(R.id.titel).setVisibility(View.GONE);
    v.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
  }
}

