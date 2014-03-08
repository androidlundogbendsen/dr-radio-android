package dk.dr.radio.akt;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONObject;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Programserie_frag2 extends Basisfragment implements AdapterView.OnItemClickListener {

  private ListView listView;
  private String programserieSlug;
  private Programserie programserie;
  private Kanal kanal;
  private View rod;
  private int antalHentedeSendeplaner;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + programserie;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
    kanal = DRData.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    Log.d("onCreateView " + this + " viser " + programserieSlug);
    if (kanal == null) {
      afbrydManglerData();
      return rod;
    }

    programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
    if (programserie == null) {
      hentUdsendelser(0);
    }

    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson_fed).getView());
    listView.setOnItemClickListener(this);

    udvikling_checkDrSkrifter(rod, this + " rod");
    setHasOptionsMenu(true);
    return rod;
  }

  private void hentUdsendelser(final int offset) {
    // svarer til v3_programserie.json
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
    String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
    Log.d("XXX url=" + url);
    App.sætErIGang(true);
    new AQuery(App.instans).ajax(url, String.class, 24 * 60 * 60 * 1000, new AjaxCallback<String>() {
      @Override
      public void callback(String url, String json, AjaxStatus status) {
        App.sætErIGang(false);
        Log.d("XXX url " + url + "   status=" + status.getCode());
        if (json != null && !"null".equals(json)) try {
          JSONObject data = new JSONObject(json);
          if (offset == 0) {
            programserie = DRJson.parsProgramserie(data);
            programserie.udsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
          } else {
            ArrayList<Udsendelse> flereUdsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            programserie.udsendelser.addAll(flereUdsendelser);
          }
          adapter.notifyDataSetChanged();
          return;
        } catch (Exception e) {
          Log.d("Parsefejl: " + e + " for json=" + json);
          e.printStackTrace();
        }
        new AQuery(rod).id(R.id.tom).text(url + "   status=" + status.getCode() + "\njson=" + json);
      }
    });
  }


  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public Udsendelse udsendelse;
    public AQuery aq;
    public View stiplet_linje;
    public TextView titel;
    public TextView titel_og_dato;
    public TextView kanal_og_varighed;
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
          aq.width(br, false).height(hø, false).image(burl, true, true, br, 0, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          aq.id(R.id.logo).image(kanal.kanallogo_resid);
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.titel);
          aq.id(R.id.alle_udsendelser).typeface(App.skrift_gibson_fed).text(Html.fromHtml("<b>ALLE UDSENDELSER</b> (" + programserie.antalUdsendelser + ")"));

          aq.id(R.id.beskrivelse).text(programserie.beskrivelse).typeface(App.skrift_georgia);
          Linkify.addLinks(aq.getTextView(), Linkify.ALL);
        } else { // if (type == UDSENDELSE eller TIDLIGERE) {
          vh.titel = aq.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
          vh.titel_og_dato = aq.id(R.id.titel_og_dato).typeface(App.skrift_gibson).getTextView();
          vh.kanal_og_varighed = aq.id(R.id.kanal_og_varighed).typeface(App.skrift_gibson).getTextView();
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

        vh.titel_og_dato.setText(Html.fromHtml("<b>" + u.titel + "</b>&nbsp; - " + DRJson.datoformat.format(u.startTid)));
        Log.d("DRJson.datoformat.format(u.startTid)=" + DRJson.datoformat.format(u.startTid));

        //String txt = u.kanal().navn + ", " + ((u.slutTid.getTime() - u.startTid.getTime())/1000/60 + " MIN");
        String txt = u.kanal().navn;
        int varighed = (int) ((u.slutTid.getTime() - u.startTid.getTime()) / 1000 / 60);
        if (varighed > 0) {
          txt += ", ";
          int timer = varighed / 60;
          if (timer > 1) txt += timer + " TIMER";
          else if (timer == 1) txt += timer + " TIME";
          int min = varighed % 60;
          if (min > 0 && timer > 0) txt += " OG ";
          if (min > 1) txt += min + " MINUTTER";
          else if (min == 1) txt += timer + " MINUT";
        }
        Log.d("txt=" + txt);
        vh.kanal_og_varighed.setText(txt);
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
          .putExtra(P_kode, kanal.kode)
          .putExtra(DRJson.Slug.name(), udsendelse.slug).getExtras());
      getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
      return;
    }

    hentUdsendelser(programserie.udsendelser.size());
    v.findViewById(R.id.titel).setVisibility(View.GONE);
    v.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
  }

  private class UdsendelseClickListener implements View.OnClickListener {

    private final Viewholder viewHolder;

    public UdsendelseClickListener(Viewholder vh) {
      viewHolder = vh;
    }

    @Override
    public void onClick(View v) {
      App.langToast("fejl2");
    }
  }
}

