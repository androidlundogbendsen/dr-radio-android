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
  private String kanalkode;
  private String url;
  private ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
  private Date nu = new Date();
  private int aktuelUdsendelseIndex;

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
      //Log.d("opdaterListe " + json.toString(2));
      nu.setTime(System.currentTimeMillis()); // TODO kompenser for forskelle mellem telefonens ur og serverens ur
      aktuelUdsendelseIndex = -1;
      String nuDatoStr = datoformat.format(nu);
      for (int n = 0; n < json.length(); n++) {
        JSONObject o = json.getJSONObject(n);
        Udsendelse u = new Udsendelse();
        u.json = o;
        u.startTid = DrJson.servertidsformat.parse(o.optString(DrJson.StartTime.name()));
        u.startTidKl = klokkenformat.format(u.startTid);
        String datoStr = datoformat.format(u.startTid);
        if (!datoStr.equals(nuDatoStr)) u.startTidKl += " - " + datoStr;
        u.slutTid = DrJson.servertidsformat.parse(o.optString(DrJson.EndTime.name()));
        u.titel = o.optString(DrJson.Title.name());
        u.beskrivelse = o.optString(DrJson.Description.name());
        u.slug = o.optString(DrJson.Slug.name());
        u.programserieSlug = o.optString(DrJson.SeriesSlug.name());
        u.urn = o.optString(DrJson.Urn.name());
        Log.d("XXXXXXX " + u.startTid.before(nu) + nu.before(u.slutTid) + "  " + u);
        //if (u.startTid.before(nu) && nu.before(u.slutTid)) aktuelUdsendelseIndex = n;
        if (u.startTid.before(nu)) aktuelUdsendelseIndex = n;
        uliste.add(u);
      }
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    App.kortToast("aktuelUdsendelseIndex=" + aktuelUdsendelseIndex);
    adapter.notifyDataSetChanged();
  }


  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return uliste.size();
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }


    @Override
    public int getItemViewType(int position) {
      if (position == 0) return 1;
      if (position == aktuelUdsendelseIndex) return 2;
      return 0;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Udsendelse u = uliste.get(position);
      int type = getItemViewType(position);
      if (v == null)
        v = getLayoutInflater(null).inflate(App.udvikling ? R.layout.listeelement_udvikler : type == 0 ? R.layout.listeelement_tid_titel_kunstner : R.layout.listeelement_billede_med_titeloverlaegning, parent, false);
      AQuery a = new AQuery(v);
      a.id(R.id.titel).text(u.titel).typeface(App.skrift_fed);
      a.id(R.id.beskrivelse).text(u.beskrivelse);
      a.id(R.id.tid).visible().text(u.startTidKl).typeface(App.skrift_normal);
      a.id(R.id.kunstner).gone();

      if (App.udvikling) {
        a.id(R.id.json).text(u.json.toString());
      }
      return v;
    }
  };

  public static final DateFormat klokkenformat = new SimpleDateFormat("HH:mm");
  public static final DateFormat datoformat = new SimpleDateFormat("d. LLL. yyyy");

  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    Udsendelse u = uliste.get(position);
    if (u.programserieSlug.length() > 0) {
      startActivity(new Intent(getActivity(), VisFragment_akt.class).
          putExtra(VisFragment_akt.KLASSE, Programserie_frag.class.getName()).putExtra(Programserie_frag.P_kode, u.programserieSlug));
    }
  }
}

