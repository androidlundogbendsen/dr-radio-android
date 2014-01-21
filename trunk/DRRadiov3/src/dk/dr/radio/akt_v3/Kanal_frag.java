package dk.dr.radio.akt_v3;

import android.app.Activity;
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

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanal_frag extends BasisFragment implements AdapterView.OnItemClickListener {

  public static String P_url = "url";
  public static String P_navn = "navn";
  private ListView listView;
  private ArrayList<JSONObject> liste = new ArrayList<JSONObject>();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setContentView(R.layout.v3_liste_frag, inflater, container);
    //setRetainInstance(true);

    //String url = "http://www.dr.dk/mu/Bundle?BundleType=%22Channel%22&DrChannel=true&ChannelType=%22RADIO%22&limit=100";
    String url = getArguments().getString(P_url);
    //App.kortToast(url);

    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).getView());

    aq.ajax(url, String.class, new AjaxCallback<String>() {
      @Override
      public void callback(String url, String json, AjaxStatus status) {
        if (json != null) try {
          //successful ajax call, show status code and json content
          //App.langToast(status.getCode() + ": OK");
          opdaterListe(new JSONArray(json));
        } catch (Exception e) {
          Log.rapporterFejl(e);
        }
        else {
          App.langToast("Error:" + status.getCode());
        }
      }
    });


    return rod;
  }

  int n;

  private void opdaterListe(JSONArray json) {
    App.kortToast("opdaterListe " + n++);
    try {
      Log.d(json.toString(2));
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
    App.kortToast("jliste.length()=" + adapter.getCount());
  }


  private BaseAdapter adapter = new BasisAdapter() {

    @Override
    public int getCount() {
      return liste.size();
    }


    @Override
    public View getView(int position, View v, ViewGroup parent) {
      if (v == null) v = getLayoutInflater(null).inflate(R.layout.v3_liste_celle, parent, false);
      AQuery a = new AQuery(v);
      JSONObject d = liste.get(position);
      a.id(R.id.slug).text(d.optString(DrJsonNavne.Slug.name()));
      a.id(R.id.serieslug).text(d.optString(DrJsonNavne.SeriesSlug.name()));
      a.id(R.id.titel).text(d.optString(DrJsonNavne.Title.name()));
      a.id(R.id.beskrivelse).text(d.optString(DrJsonNavne.Description.name()));
      return v;
    }

  };


  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
  }


  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((Navigation_akt) activity).sætTitel(getArguments().getString(P_navn));
  }
}

