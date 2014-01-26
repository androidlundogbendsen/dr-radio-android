package dk.dr.radio.akt_v3;

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

import dk.dr.radio.data.DrJsonNavne;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanal_frag extends Basisfragment implements AdapterView.OnItemClickListener {

  public static String P_kode = "kanalkode";
  private ListView listView;
  private ArrayList<JSONObject> liste = new ArrayList<JSONObject>();
  private String kanalkode;
  private String url;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //setRetainInstance(true);

    //String url = "http://www.dr.dk/mu/Bundle?BundleType=%22Channel%22&DrChannel=true&ChannelType=%22RADIO%22&limit=100";
    kanalkode = getArguments().getString(P_kode);
    url = "http://www.dr.dk/tjenester/mu-apps/schedule/" + kanalkode;  // svarer til v3_kanalside__p3.json
    Log.d("XXX url=" + url);
    App.sætErIGang(true);
    new AQuery(App.instans).ajax(url, String.class, 60000, new AjaxCallback<String>() {
      @Override
      public void callback(String url, String json, AjaxStatus status) {
        App.sætErIGang(false);
        Log.d("XXX url " + url + "   status=" + status.getCode());
        if (json != null && !"null".equals(json)) try {
          opdaterListe(new JSONArray(json));
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
    setContentView(R.layout.v3_liste_frag, inflater, container);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).getView());
    return rod;
  }

  @Override
  public void onResume() {
    super.onResume();
    ((Hovedaktivitet) getActivity()).sætTitel(getArguments().getString(P_kode));
  }

  private void opdaterListe(JSONArray json) {
    try {
      Log.d("opdaterListe " + json.toString(2));
      liste = new ArrayList<JSONObject>();
      JSONArray jliste = json;//.optJSONArray("Data");
//      Log.d(jliste.toString(2));
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
      "Urn" : "urn:dr:mu:programcard:52cc9abd6187a213f89addf1",
      "Surround" : false,
      "HasLatest" : true,
      "Title" : "VinterMorgen",
      "SeriesSlug" : "vintermorgen",
      "Rerun" : false,
      "FirstPartOid" : 245310732813,
      "StartTime" : "2014-01-16T06:04:00+01:00",
      "Slug" : "vintermorgen-16",
      "TransmissionOid" : 245310731812,
      "Watchable" : true,
      "Widescreen" : false,
      "HD" : false,
      "EndTime" : "2014-01-16T09:04:00+01:00",
      "Episode" : 0,
      "Description" : "- musik, nyheder, journalistik, satire og sport starter den nye dag.\nVært: Nicholas Kawamura."
   },
 */
@Override
    public View getView(int position, View v, ViewGroup parent) {
      if (v == null) v = getLayoutInflater(null).inflate(R.layout.v3_liste_celle, parent, false);
      AQuery a = new AQuery(v);
      JSONObject d = liste.get(position);
      a.id(R.id.titel).text(d.optString(DrJsonNavne.Title.name()));
  a.id(R.id.undertitel).gone();
  a.id(R.id.beskrivelse).text(d.optString(DrJsonNavne.Description.name()));
  a.id(R.id.slug).text(d.optString(DrJsonNavne.Slug.name()));
  a.id(R.id.serieslug).text(d.optString(DrJsonNavne.SeriesSlug.name()));
  if (App.udvikling) a.id(R.id.debug).text(d.toString());
  return v;
    }
  };


  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    JSONObject d = liste.get(position);
  }
}

