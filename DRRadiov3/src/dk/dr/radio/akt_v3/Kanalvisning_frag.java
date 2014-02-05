package dk.dr.radio.akt_v3;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanalvisning_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {

  public static String P_kode = "kanal.kode";
  private ListView listView;
  private String url;
  private ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
  private Date nu = new Date();
  private int aktuelUdsendelseIndex;
  private Kanal kanal;
  protected AQuery aq;
  protected View rod;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //setRetainInstance(true);

    kanal = DRData.instans.stamdata.kanalFraKode.get(getArguments().getString(P_kode));
    url = "http://www.dr.dk/tjenester/mu-apps/schedule/" + kanal.kode;  // svarer til v3_kanalside__p3.json
    Log.d("XXX url=" + url);
    App.sætErIGang(true);
    //new AQuery(getActivity()).ajax(url, String.class, 60000, new AjaxCallback<String>() {
    new AQuery(getActivity()).ajax(url, String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
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
    Log.d("Viser fragment " + this);
    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).getView());
    return rod;
  }


  @Override
  public void onResume() {
    super.onResume();
    ((Hovedaktivitet) getActivity()).sætTitel(getArguments().getString(P_kode));
    App.forgrundstråd.postDelayed(this, 50);
  }

  @Override
  public void onPause() {
    super.onPause();
    App.forgrundstråd.removeCallbacks(this);
  }

  @Override
  public void run() {
    App.forgrundstråd.postDelayed(this, 5000);
    opdaterAktuelUdsendelse();
  }


  private void opdaterListe(JSONArray json) {
    try {
      //Log.d("opdaterListe " + json.toString(2));
      nu.setTime(System.currentTimeMillis()); // TODO kompenser for forskelle mellem telefonens ur og serverens ur
      Log.d("XXXXXXX " + kanal.kode + "  nu=" + nu);
      aktuelUdsendelseIndex = -1;
      String nuDatoStr = datoformat.format(nu);
      for (int n = 0; n < json.length(); n++) {
        JSONObject o = json.getJSONObject(n);
        Udsendelse u = new Udsendelse();
        u.json = o;
        u.startTid = DRJson.servertidsformat.parse(o.optString(DRJson.StartTime.name()));
        u.startTidKl = klokkenformat.format(u.startTid);
        u.slutTid = DRJson.servertidsformat.parse(o.optString(DRJson.EndTime.name()));

        u.slutTidKl = klokkenformat.format(u.slutTid);
        String datoStr = datoformat.format(u.startTid);
        if (!datoStr.equals(nuDatoStr)) u.startTidKl += " - " + datoStr;


        u.titel = o.optString(DRJson.Title.name());
        u.beskrivelse = o.optString(DRJson.Description.name());
        u.slug = o.optString(DRJson.Slug.name());
        u.programserieSlug = o.optString(DRJson.SeriesSlug.name());
        u.urn = o.optString(DRJson.Urn.name());
        Log.d(n + " XXXXXXX " + kanal.kode + u.startTid.before(nu) + nu.before(u.slutTid) + "  " + u);
        //if (u.startTid.before(nu) && nu.before(u.slutTid)) aktuelUdsendelseIndex = n;
        if (u.startTid.before(nu)) aktuelUdsendelseIndex = n;
        uliste.add(u);
      }
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    Log.d("XXXXXXX " + kanal.kode + "  aktuelUdsendelseIndex=" + aktuelUdsendelseIndex);
    adapter.notifyDataSetChanged();
    visAktuelUdsendelse();
  }

  private void visAktuelUdsendelse() {
    if (listView == null) return;
    if (adapter == null) return;
    int topmargen = getResources().getDimensionPixelOffset(R.dimen.kanalvisning_aktuelUdsendelse_topmargen);
    listView.setSelectionFromTop(aktuelUdsendelseIndex, topmargen);
  }


  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView startid;
    public Udsendelse udsendelse;
    public TextView sluttid;
    public View starttidbjælke;
    public View slutttidbjælke;
  }

  private Viewholder aktuelUdsendelseViewholder;

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
      //if (position == 0) return 1;
      if (position == aktuelUdsendelseIndex) return AKTUEL;
      return NORMAL;
    }

    public static final int NORMAL = 0;
    public static final int AKTUEL = 2;

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Viewholder vh;
      AQuery a;
      int type = getItemViewType(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(type == NORMAL ? R.layout.listeelement_tid_titel_kunstner : R.layout.listeelement_kanal_lige_nu, parent, false);
        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.titel = a.id(R.id.titel).typeface(App.skrift_fed).getTextView();
        vh.startid = a.id(R.id.startid).typeface(App.skrift_normal).getTextView();
        vh.sluttid = a.id(R.id.slutttid).typeface(App.skrift_normal).getTextView();
        vh.starttidbjælke = a.id(R.id.starttidbjælke).getView();
        vh.slutttidbjælke = a.id(R.id.slutttidbjælke).getView();
        a.id(R.id.højttalerikon).clicked(new UdsendelseClickListener(vh));
        a.id(R.id.kunstner).text("");
        v.setTag(vh);
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
      }
      Udsendelse u = uliste.get(position);
      // Opdatér viewholderens data
      vh.udsendelse = u;
      vh.titel.setText(u.titel);
      vh.startid.setText(u.startTidKl);

      if (getItemViewType(position) == AKTUEL) {
        aktuelUdsendelseViewholder = vh;
        //a.id(R.id.billede).image("http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/radioavis-24907&w=300&h=169&scaleAfter=crop");
        a.id(R.id.billede).image("http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/" + u.slug + "&w=300&h=169&scaleAfter=crop");
        v.setBackgroundColor(getResources().getColor(R.color.hvid));
      }

      // Til udvikling
      a.id(R.id.beskrivelse).text(u.beskrivelse);
      if (App.udvikling) {
        try {
          Log.d(u.json.toString(2));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
      return v;
    }
  };

  private void opdaterAktuelUdsendelse() {
    if (aktuelUdsendelseViewholder == null) return;
    try {
      Viewholder vh = aktuelUdsendelseViewholder;
      Udsendelse u = vh.udsendelse;
      long passeret = nu.getTime() - u.startTid.getTime();
      long længde = u.slutTid.getTime() - u.startTid.getTime();
      int passeretPct = længde > 0 ? (int) (passeret * 100 / længde) : 0;
      Log.d(kanal.kode + " passeretPct=" + passeretPct + " af længde=" + længde);
      AQuery a = aktuelUdsendelseViewholder.aq;
      LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) vh.starttidbjælke.getLayoutParams();
      lp.weight = passeretPct;
      vh.starttidbjælke.setLayoutParams(lp);

      lp = (LinearLayout.LayoutParams) vh.slutttidbjælke.getLayoutParams();
      lp.weight = 100 - passeretPct;
      vh.slutttidbjælke.setLayoutParams(lp);
    } catch (Exception e) {
      Log.rapporterFejl(e); }
  }

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

  private class UdsendelseClickListener implements View.OnClickListener {

    private final Viewholder viewHolder;

    public UdsendelseClickListener(Viewholder vh) {
      viewHolder = vh;
    }

    @Override
    public void onClick(View v) {
      if (aktuelUdsendelseViewholder == viewHolder) {
        DRData.instans.aktuelKanal = kanal;
        DRData.instans.afspiller.setUrl(kanal.lydUrl.get(null));
      } else {
        url = "http://www.dr.dk/tjenester/mu-apps/program/" + viewHolder.udsendelse.slug + "?type=radio&includeStreams=true";

      }
      DRData.instans.afspiller.startAfspilning();
    }
  }
}

