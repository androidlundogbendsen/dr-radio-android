package dk.dr.radio.akt;

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

import dk.dr.radio.data.DRJson;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.ui.Basisadapter;
import dk.dr.radio.diverse.ui.Basisfragment;
import dk.dr.radio.v3.R;

public class Programserie_frag extends Basisfragment implements AdapterView.OnItemClickListener {

  public static String P_kode = "kanalkode";
  private ListView listView;
  private ArrayList<JSONObject> liste = new ArrayList<JSONObject>();
  private String kanalkode;
  private String url;
  private JSONObject data;
  private AQuery aq;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //setRetainInstance(true);

    kanalkode = getArguments().getString(P_kode);
    // svarer til v3_programserie.json
    url = "http://www.dr.dk/tjenester/mu-apps/series/" + kanalkode + "?type=radio&includePrograms=true";
    Log.d("XXX url=" + url);
    App.sætErIGang(true);
    new AQuery(App.instans).ajax(url, String.class, 60000, new AjaxCallback<String>() {
      @Override
      public void callback(String url, String json, AjaxStatus status) {
        App.sætErIGang(false);
        Log.d("XXX url " + url + "   status=" + status.getCode());
        if (json != null && !"null".equals(json)) try {
          data = new JSONObject(json);
          opdaterSkærm();
          return;
        } catch (Exception e) {
          Log.d("Parsefejl: " + e + " for json=" + json);
        }
        aq.id(R.id.tom).text(url + "   status=" + status.getCode() + "\njson=" + json);
      }
    });
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    View rod = inflater.inflate(R.layout.kanalvisning_frag, container, false);
    aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).getView());
    return rod;
  }

  /*
    @Override
    public void onResume() {
      super.onResume();
      ((Hovedaktivitet) getActivity()).sætTitel(getArguments().getString(P_kode));
    }
  */
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

    /*
       {
       "Explicit" : true,
       "Subtitle" : "",
       "Urn" : "urn:dr:mu:bundle:4f3b8b29860d9a33ccfdb775",
       "TotalPrograms" : 313,
       "Channel" : "dr.dk/mas/whatson/channel/P3",
       "Title" : "Monte Carlo på P3",
       "Webpage" : "http://www.dr.dk/p3/programmer/monte-carlo",
       "Description" : "Nu kan du dagligt fra 14-16 komme en tur til  Monte Carlo, hvor Peter Falktoft og Esben Bjerre vil guide dig rundt.\r\nDu kan læne dig tilbage og nyde turen og være på en lytter, når Peter og Esben vender ugens store og små kulturelle begivenheder, kigger på ugens bedste tv og spørger hvad du har #HørtOverHækken.\r\n",
       "Images" : null,
       "Slug" : "monte-carlo",
       "ChannelType" : 0,
       },

          {
             "BroadcastStartTime" : "2013-12-20T14:04:00+01:00",
             "Subtitle" : "",
             "Urn" : "urn:dr:mu:programcard:52a901e46187a2197cc3d14f",
             "Title" : "Monte Carlo",
             "SeriesSlug" : "monte-carlo",
             "ProductionNumber" : "13331315515",
             "Streams" : null,
             "Slug" : "monte-carlo-355",
             "LatestBroadcast" : "2013-12-20T14:04:00+01:00",
             "Channel" : "dr.dk/mas/whatson/channel/P3",
             "FirstBroadcast" : "2013-12-20T14:04:00+01:00",
             "SpotTitle" : null,
             "Playlist" : null,
             "Broadcasts" : null,
             "PreviousProgramSlug" : "monte-carlo-354",
             "SpotTeaser" : null,
             "Episode" : 313,
             "Description" : "Peter Falktoft og Esben Bjerre vender ugens store og små begivenheder med et satirisk blik, kigger på ugens bedste tv og spørger lytterne hvad de har #HørtOverHækken.",
             "NextProgramSlug" : null
          },
     */
    @Override
    public View getView(int position, View v, ViewGroup parent) {
      if (v == null) v = getLayoutInflater(null).inflate(R.layout.listeelement_udvikler, parent, false);
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
    if (slug.length() > 0) {
//      startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtra(VisFragment_akt.KLASSE));
    }
  }
}

