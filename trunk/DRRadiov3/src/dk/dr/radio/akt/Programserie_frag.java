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
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class Programserie_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener {

  private ListView listView;
  private ArrayList<Object> liste = new ArrayList<Object>();
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
    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    aq = new AQuery(rod);

    programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
    if (programserie == null) {
      hentUdsendelser(0); // hent kun en frisk udgave hvis vi ikke allerede har en
    }
    bygListe();

    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);

    Log.registrérTestet("Visning af programserie", "ja");
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
        JSONObject data = new JSONObject(json);
        if (offset == 0) {
          programserie = DRJson.parsProgramserie(data, programserie);
          DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
        }
        programserie.tilføjUdsendelser(offset, DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), kanal, DRData.instans));
        bygListe();
      }

      @Override
      protected void fikFejl(VolleyError error) {
        super.fikFejl(error);
        if (offset == 0) {
          aq.id(R.id.tom).text("Siden kunne ikke vises");
        } else {
          bygListe(); // for at fjerne evt progressBar
        }
      }
    }).setTag(this);
    App.volleyRequestQueue.add(req);
  }

  @Override
  public void onClick(View v) {
    DRData.instans.favoritter.sætFavorit(programserieSlug, favorit.isChecked());
    if (favorit.isChecked()) App.kortToast("Programserien er tilføjet til favoritter");
    Log.registrérTestet("Valg af favoritprogram", programserieSlug);
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
    public int itemViewType;
  }

  void bygListe() {
    liste.clear();
    if (programserie!=null) {
      liste.add(TOP);
      if (programserie.getUdsendelser()!=null) {
        liste.addAll(programserie.getUdsendelser());
        if (programserie.getUdsendelser().size()<programserie.antalUdsendelser) {
          Log.d("bygListe() viser TIDLIGERE: "+programserie.getUdsendelser().size()+" < "+programserie.antalUdsendelser);
          liste.add(TIDLIGERE);  // Vis 'tidligere'-listeelement
        }
      }
    }
    adapter.notifyDataSetChanged();
  }

  static final int TOP = 0;
  static final int UDSENDELSE = 1;
  static final int TIDLIGERE = 2;

  static final int[] layoutFraType = {
      R.layout.programserie_elem0_top,
      R.layout.programserie_elem1_udsendelse,
      R.layout.kanal_elem2_tidligere_senere,
  };

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public int getItemViewType(int position) {
      Object o = liste.get(position);
      if (o instanceof Integer) return (Integer) o;
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
        vh.itemViewType = type;
        AQuery aq = vh.aq = new AQuery(v);
        v.setTag(vh);
        if (type == TOP) {
          int br = bestemBilledebredde(listView, (View) aq.id(R.id.billede).getView().getParent(), 50);
          int hø = br * højde9 / bredde16;
          String burl = skalérSlugBilledeUrl(programserie.slug, br, hø);
          aq.width(br, false).height(hø, false).image(burl, true, true, br, AQuery.INVISIBLE, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          if (kanal == null) aq.id(R.id.logo).gone();
          else aq.id(R.id.logo).image(kanal.kanallogo_resid).getView().setContentDescription(kanal.navn);
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed).text(programserie.titel);
          String tekst = "ALLE UDSENDELSER";
          aq.id(R.id.alle_udsendelser).typeface(App.skrift_gibson)
              .text(lavFedSkriftTil(tekst + " (" + programserie.antalUdsendelser + ")", tekst.length()))
              .getView().setContentDescription(programserie.antalUdsendelser+" udsendelser");
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
        if (!App.PRODUKTION && vh.itemViewType!=type) throw new IllegalStateException("Liste ej konsistent, der er nok sket ændringer i den fra f.eks. getView()");
      }

      // Opdatér viewholderens data
      if (type == UDSENDELSE) {
        Udsendelse u = (Udsendelse) liste.get(position);
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
        vh.varighed.setContentDescription(txt.toLowerCase());
        vh.varighed.setVisibility(txt.length() > 0 ? View.VISIBLE : View.GONE);
      } else if (type == TIDLIGERE) {
        if (antalHentedeSendeplaner++ < 7) {
          vh.aq.id(R.id.progressBar).visible();   // De første 7 henter vi bare for brugeren
          vh.titel.setVisibility(View.VISIBLE);
          // skal ske lidt senere, når viewet er færdigt opdateret
          App.forgrundstråd.post(new Runnable() {
            @Override
            public void run() {
              hentUdsendelser(programserie.getUdsendelser().size());
            }
          });
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
    if (position <= programserie.getUdsendelser().size()) {
      Udsendelse udsendelse = programserie.getUdsendelser().get(position - 1);
      // Vis normalt et Udsendelser_vandret_skift_frag med flere udsendelser
      // Hvis tilgængelighed er slået til (eller bladring slået fra) vises blot ét Udsendelse_frag
      Fragment f =
          App.accessibilityManager.isEnabled() || !App.prefs.getBoolean("udsendelser_bladr", true) ? new Udsendelse_frag() :
              App.prefs.getBoolean("udsendelser_lodret_skift", false) ? new Udsendelser_lodret_skift_frag() :
                  new Udsendelser_vandret_skift_frag(); // standard
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

    hentUdsendelser(programserie.getUdsendelser().size());
    v.findViewById(R.id.titel).setVisibility(View.GONE);
    v.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
  }
}

