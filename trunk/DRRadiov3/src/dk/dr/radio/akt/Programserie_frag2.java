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
  private ArrayList<JSONObject> liste = new ArrayList<JSONObject>();
  private JSONObject data;
  private AQuery aq;
  private String programserieSlug;
  private Programserie programserie;
  private Kanal kanal;
  private View rod;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
    kanal = DRData.instans.stamdata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    Log.d(this + " viser " + programserieSlug);

    programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
    if (programserie == null) {
      // svarer til v3_programserie.json
      // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
      // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true
      String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true";
      Log.d("XXX url=" + url);
      App.sætErIGang(true);
      new AQuery(App.instans).ajax(url, String.class, 24 * 60 * 60 * 1000, new AjaxCallback<String>() {
        @Override
        public void callback(String url, String json, AjaxStatus status) {
          App.sætErIGang(false);
          Log.d("XXX url " + url + "   status=" + status.getCode());
          if (json != null && !"null".equals(json)) try {
            data = new JSONObject(json);
            programserie = DRJson.parsProgramserie(data);
            programserie.udsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
//            opdaterSkærm();
            adapter.notifyDataSetChanged();
            return;
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
            e.printStackTrace();
          }
          aq.id(R.id.tom).text(url + "   status=" + status.getCode() + "\njson=" + json);
        }
      });
    }
  }

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + programserie;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("onCreateView " + this);

    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    if (kanal == null) {
      afbrydManglerData();
      return rod;
    }
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_fed).getView());
    listView.setOnItemClickListener(this);

    udvikling_checkDrSkrifter(rod, this + " rod");
    setHasOptionsMenu(true);
    return rod;
  }


/*
  private ListView listView;
  private Kanal kanal;
  protected View rod;
  private Udsendelse udsendelse;
  private ArrayList<Playlisteelement> liste = new ArrayList<Playlisteelement>();
  private boolean streams;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    kanal = DRData.instans.stamdata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    udsendelse = DRData.instans.udsendelseFraSlug.get(getArguments().getString(DRJson.Slug.name()));
    Log.d("onCreateView " + this);

    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    if (kanal == null || udsendelse == null) {
      afbrydManglerData();
      return rod;
    }
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_fed).getView());
    listView.setOnItemClickListener(this);

    if (udsendelse.playliste != null) {
      liste = udsendelse.playliste;
    } else {
      String url = kanal.getPlaylisteUrl(udsendelse); // http://www.dr.dk/tjenester/mu-apps/playlist/monte-carlo-352/p3
      Log.d("Henter playliste " + url);
      App.sætErIGang(true);
      aq.ajax(url, String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
        @Override
        public void callback(String url, String json, AjaxStatus status) {
          App.sætErIGang(false);
          Log.d("XXX url " + url + "   status=" + status.getCode());
          if (json != null && !"null".equals(json)) try {
            udsendelse.playliste = DRJson.parsePlayliste(new JSONArray(json));
            adapter.notifyDataSetChanged();
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
            e.printStackTrace();
          }
        }
      });
    }
    streams = udsendelse.streams != null && udsendelse.streams.size() > 0;
    if (streams && DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
      DRData.instans.afspiller.setLydkilde(udsendelse);
    }

    if (udsendelse.streams == null) {
      App.sætErIGang(true);
      aq.ajax(udsendelse.getStreamsUrl(), String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
        @Override
        public void callback(String url, String json, AjaxStatus status) {
          App.sætErIGang(false);
          Log.d("XXX udsendelse.getStreamsUrl()= " + url + "   status=" + status.getCode());
          if (json != null && !"null".equals(json)) try {
            JSONObject o = new JSONObject(json);
            udsendelse.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
            streams = udsendelse.streams != null && udsendelse.streams.size() > 0;
            if (streams && DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
              DRData.instans.afspiller.setLydkilde(udsendelse);
            }
            adapter.notifyDataSetChanged();
            ActivityCompat.invalidateOptionsMenu(getActivity());
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
            e.printStackTrace();
          }
        }
      });
    }
    udvikling_checkDrSkrifter(rod, this + " rod");
    setHasOptionsMenu(true);
    return rod;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.udsendelse, menu);
    menu.findItem(R.id.hør).setVisible(streams);
    menu.findItem(R.id.hent).setVisible(App.hentning != null && streams);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.hør) {
      hør();
    } else if (item.getItemId() == R.id.hent) {
      hent();
    } else if (item.getItemId() == R.id.del) {
      del();
    } else return super.onOptionsItemSelected(item);
    return true;
  }
*/

  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView startid;
    public Udsendelse udsendelse;
  }

  static final int TOP = 0;
  static final int UDSENDELSE = 1;

  static final int[] layoutFraType = {
      R.layout.programserie_elem0_top,
      R.layout.udsendelse_elem2_tid_titel_kunstner};

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return programserie != null ? programserie.udsendelser.size() + 1 : 0;
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      if (position == 0) return TOP;
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
      AQuery aq;
      int type = getItemViewType(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(layoutFraType[type], parent, false);
        vh = new Viewholder();
        aq = vh.aq = new AQuery(v);
        v.setTag(vh);
        vh.startid = aq.id(R.id.startid).typeface(App.skrift_normal).getTextView();
        vh.titel = aq.id(R.id.titel).typeface(App.skrift_fed).getTextView();
        if (type == TOP) {
          int br = bestemBilledebredde(listView, (View) aq.id(R.id.billede).getView().getParent());
          int hø = br * højde9 / bredde16;
          String burl = skalérSlugBilledeUrl(programserie.slug, br, hø);
          aq.width(br, false).height(hø, false).image(burl, true, true, br, 0, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          aq.id(R.id.logo).image(kanal.kanallogo_resid);
          aq.id(R.id.titel).typeface(App.skrift_fed).text(programserie.titel);

          aq.id(R.id.beskrivelse).text(programserie.beskrivelse).typeface(App.skrift_normal);
          Linkify.addLinks(aq.getTextView(), Linkify.ALL);

        } else {
          vh.titel = aq.id(R.id.titel_og_kunstner).typeface(App.skrift_normal).getTextView();
        }
        aq.id(R.id.højttalerikon).visible().clicked(new UdsendelseClickListener(vh));
      } else {
        vh = (Viewholder) v.getTag();
        aq = vh.aq;
      }

      // Opdatér viewholderens data
      if (type != TOP) {
        Udsendelse u = programserie.udsendelser.get(position - 1);
        vh.udsendelse = u;
        vh.titel.setText(Html.fromHtml("<b>" + u.titel + "</b> &nbsp; | &nbsp;" + u.startTidKl));
        //vh.startid.setText(u.kanal().navn + ((u.slutTid.getTime() -  u.startTid.getTime()/1000/60)+" MIN"));
      }
      udvikling_checkDrSkrifter(v, this + " position " + position);
      return v;
    }
  };


  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    if (position == 0) return;
    //startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtras(getArguments())  // Kanalkode + slug
    //    .putExtra(VisFragment_akt.KLASSE, Programserie_frag.class.getName()).putExtra(DRJson.SeriesSlug.name(), udsendelse.programserieSlug));

    Udsendelse udsendelse = programserie.udsendelser.get(position - 1);
    Fragment f = new Udsendelse_frag();
    f.setArguments(new Intent()
        .putExtra(P_kode, kanal.kode)
        .putExtra(DRJson.Slug.name(), udsendelse.slug).getExtras());
    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
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

