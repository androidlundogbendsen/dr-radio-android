package dk.dr.radio.akt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.akt.diverse.VisFragment_akt;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Programserie_frag extends Basisfragment implements AdapterView.OnItemClickListener {

  private ListView listView;
  private ArrayList<JSONObject> liste = new ArrayList<JSONObject>();
  private JSONObject data;
  private AQuery aq;
  private String programserieSlug;
  private Programserie ps;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    programserieSlug = getArguments().getString(DRJson.SeriesSlug.name());
    Log.d(this + " viser " + programserieSlug);

    ps = DRData.instans.programserieFraSlug.get(programserieSlug);
    if (ps == null) {
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
            ps = DRJson.parsProgramserie(data);
            ps.udsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            DRData.instans.programserieFraSlug.put(programserieSlug, ps);
            opdaterSkærm();
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
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    View rod = inflater.inflate(R.layout.kanal_frag, container, false);
    aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_fed).getView());
    return rod;
  }

  private void opdaterSkærm() {
    try {
      Log.d("opdaterSkærm " + data.toString(2));
      liste = new ArrayList<JSONObject>();
      liste.add(data); // nul'te element i listen er beskrivelsen af programserien
      JSONArray jliste = data.getJSONArray("Programs");
      for (int n = 0; n < jliste.length(); n++) {
        JSONObject o = jliste.getJSONObject(n);
        liste.add(o);
      }

    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    adapter.notifyDataSetChanged();
  }


  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      if (v == null) v = getLayoutInflater(null).inflate(R.layout.elem_udvikler, parent, false);
      AQuery a = new AQuery(v);
      JSONObject d = liste.get(position);
      a.id(R.id.titel).text(d.optString(DRJson.Title.name()));
      a.id(R.id.beskrivelse).text(d.optString(DRJson.Description.name()));
      a.id(R.id.slug).text(d.optString(DRJson.Slug.name()));
      a.id(R.id.serieslug).text(d.optString(DRJson.SeriesSlug.name()));
      if (App.udvikling) a.id(R.id.json).text(d.toString());
      return v;
    }
  };


  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    JSONObject d = liste.get(position);
    String slug = d.optString(DRJson.Slug.name());
    Kanal k = DRData.instans.stamdata.kanalFraSlug.get(d.optString(DRJson.ChannelSlug.name()));
    if (k != null && slug.length() > 0) {
      startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName()).putExtra(Kanal_frag.P_kode, k.kode) // Kanalkode
          .putExtra(DRJson.Slug.name(), slug)); // Udsenselses-ID
    }
  }
}

