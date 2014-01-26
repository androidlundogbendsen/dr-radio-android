package dk.dr.radio.akt_v3;

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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.DrJson;
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
    setContentView(R.layout.kanal_frag, inflater, container);
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
        if (n == 1) {
          o.put("type", 1);
          o.put("procent_færdig", (int) (Math.random() * 80 + 10));
        }
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
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public int getItemViewType(int position) {
      return liste.get(position).optInt("type", 0);
    }

    /*
http://asset.dr.dk/imagescaler/?file=%2Fmu%2Fbar%2F52dfef5ca11f9d0980776171&w=300&h=169&scaleAfter=crop&server=www.dr.dk
http://www.dr.dk/mu/bar/4f9f92c8860d9a1804f0a119?width=200&height=200
http://www.dr.dk/mu/bar/52dfef5ca11f9d0980776171?width=200&height=200

http://www.dr.dk/mu/bar/4f3b88f8860d9a33ccfdaec9?width=200&height=200

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
                               2014-01-26T21:31:08+0100
                  "Episode" : 0,
                  "Description" : "- musik, nyheder, journalistik, satire og sport starter den nye dag.\nVært: Nicholas Kawamura."
               },
             */
    @Override
    public View getView(int position, View v, ViewGroup parent) {
      JSONObject d = liste.get(position);
      int type = d.optInt("type", 0);
      if (v == null)
        v = getLayoutInflater(null).inflate(App.udvikling ? R.layout.listeelement_udvikler : type == 0 ? R.layout.listeelement_tid_titel_kunstner : R.layout.listeelement_billede_med_titeloverlaegning, parent, false);
      AQuery a = new AQuery(v);
      a.id(R.id.titel).text(d.optString(DrJson.Title.name()));
      a.id(R.id.beskrivelse).text(d.optString(DrJson.Description.name()));


      try {
        Date startTid = DrJson.servertidsformat.parse(d.optString(DrJson.StartTime.name()));
        a.id(R.id.tid).visible().text(klokkenformat.format(startTid));
      } catch (ParseException e) {
        e.printStackTrace();
        a.id(R.id.tid).gone();
      }

      a.id(R.id.kunstner).gone();

      if (App.udvikling) {
        a.id(R.id.slug).text(d.optString(DrJson.Slug.name()));
        a.id(R.id.serieslug).text(d.optString(DrJson.SeriesSlug.name()));
        a.id(R.id.json).text(d.toString());
      }
      return v;
    }
  };

  public static final DateFormat klokkenformat = new SimpleDateFormat("HH:mm");

  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    JSONObject d = liste.get(position);
    String programserieSlug = d.optString(DrJson.SeriesSlug.name());
    if (programserieSlug.length() > 0) {
      startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtra(VisFragment_akt.KLASSE, Programserie_frag.class.getName()).putExtra(Programserie_frag.P_kode, programserieSlug));
    }
  }
}

