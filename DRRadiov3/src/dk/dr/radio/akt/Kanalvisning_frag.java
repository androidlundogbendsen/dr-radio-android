package dk.dr.radio.akt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.ui.Basisadapter;
import dk.dr.radio.diverse.ui.Basisfragment;
import dk.dr.radio.diverse.ui.VisFragment_akt;
import dk.dr.radio.v3.R;

public class Kanalvisning_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener, Runnable {

  public static String P_kode = "kanal.kode";
  private ListView listView;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  private int aktuelUdsendelseIndex;
  private Kanal kanal;
  protected View rod;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    kanal = DRData.instans.stamdata.kanalFraKode.get(getArguments().getString(P_kode));
    rod = inflater.inflate(R.layout.kanalvisning_frag, container, false);
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).getView());
    hentSendeplanForDag(aq, 0);
    return rod;
  }

  private void hentSendeplanForDag(final AQuery aq, final int dag) {
    String url = kanal.getUdsendelserUrl() + "/" + dag;
    Log.d("XXX url=" + url);
    App.sætErIGang(true);
    aq.ajax(url, String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
      @Override
      public void callback(String url, String json, AjaxStatus status) {
        App.sætErIGang(false);
        Log.d("XXX url " + url + "   status=" + status.getCode());
        if (json != null && !"null".equals(json)) try {
          kanal.parsUdsendelser(new JSONArray(json), dag);
          opdaterListe(kanal.udsendelser);
          scrollTilAktuelUdsendelse();
          return;
        } catch (Exception e) {
          Log.d("Parsefejl: " + e + " for json=" + json);
        }
        aq.id(R.id.tom).text(url + "   status=" + status.getCode() + "\njson=" + json);
      }
    });
  }


  @Override
  public void onResume() {
    super.onResume();
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


  private void opdaterListe(ArrayList<Udsendelse> nyuliste) {
    try {
      //Log.d("opdaterListe " + json.toString(2));
      Date nu = new Date(); // TODO kompenser for forskelle mellem telefonens ur og serverens ur
      Log.d("XXXXXXX " + kanal.kode + "  nu=" + nu);
      aktuelUdsendelseIndex = -1;
      liste.clear();
      liste.add(new Udsendelse("Tidligere"));
      liste.addAll(nyuliste);
      for (int n = 1; n < liste.size(); n++) {
        Udsendelse u = liste.get(n);
        Log.d(n + " XXXXXXX " + kanal.kode + u.startTid.before(nu) + nu.before(u.slutTid) + "  " + u);
        //if (u.startTid.before(nu) && nu.before(u.slutTid)) aktuelUdsendelseIndex = n;
        if (u.startTid.before(nu)) aktuelUdsendelseIndex = n;
      }
      liste.add(new Udsendelse("Senere"));

    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    Log.d("XXXXXXX " + kanal.kode + "  aktuelUdsendelseIndex=" + aktuelUdsendelseIndex);
    adapter.notifyDataSetChanged();
  }


  private void scrollTilAktuelUdsendelse() {
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
    public View starttidbjælke;
    public View slutttidbjælke;
  }

  private Viewholder aktuelUdsendelseViewholder;

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
      //if (position == 0) return 1;
      if (position == aktuelUdsendelseIndex) return AKTUEL;
      if (position == 0 || position == liste.size() - 1) return TIDLIGERE_SENERE;
      return NORMAL;
    }

    public static final int NORMAL = 0;
    public static final int AKTUEL = 1;
    public static final int TIDLIGERE_SENERE = 2;

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Viewholder vh;
      AQuery a;
      int type = getItemViewType(position);
      Udsendelse u = liste.get(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(type == AKTUEL ? R.layout.kanalvisning_aktuel : R.layout.element_tid_titel_kunstner, parent, false);
        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.titel = a.id(R.id.titel).typeface(App.skrift_fed).getTextView();
        vh.startid = a.id(R.id.startid).typeface(App.skrift_normal).getTextView();
        vh.starttidbjælke = a.id(R.id.starttidbjælke).getView();
        vh.slutttidbjælke = a.id(R.id.slutttidbjælke).getView();
        //a.id(R.id.højttalerikon).clicked(new UdsendelseClickListener(vh));
        a.id(R.id.højttalerikon).gone();  // Bruges ikke mere i dette design
        a.id(R.id.hør_live).text(" HØR " + kanal.navn + " LIVE").clicked(Kanalvisning_frag.this);
        a.id(R.id.slutttid).typeface(App.skrift_normal).text(u.slutTidKl);
        a.id(R.id.kunstner).text(""); // ikke .gone() - skal skubbe højttalerikon ud til venstre
        v.setTag(vh);
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
      }
      // Opdatér viewholderens data
      vh.udsendelse = u;
      vh.titel.setText(u.titel);
      vh.startid.setText(u.startTidKl);

      if (getItemViewType(position) == AKTUEL) {
        aktuelUdsendelseViewholder = vh;

        a.id(R.id.billede).image("http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/" + u.slug + "&w=" + bredde + "&h=" + højde + "&scaleAfter=crop");
        opdaterSenestSpillet(a, u);
        a.id(R.id.senest_spillet_overskrift).typeface(App.skrift_normal); // ???
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


  private void opdaterSenestSpillet(AQuery aq, Udsendelse u) {

    if (u.playliste == null) {
      // optimering - brug kun final i enkelte tilfælde. Final forårsager at variabler lægges i heap i stedet for stakken) at garbage collectoren skal køre fordi final
      final Udsendelse u2 = u;
      final AQuery aq2 = aq;
      String url = kanal.getPlaylisteUrl(u); // http://www.dr.dk/tjenester/mu-apps/playlist/monte-carlo-352/p3
      Log.d("Henter playliste " + url);
      App.sætErIGang(true);
      aq.ajax(url, String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
        @Override
        public void callback(String url, String json, AjaxStatus status) {
          App.sætErIGang(false);
          Log.d("XXX url " + url + "   status=" + status.getCode());
          if (json != null && !"null".equals(json)) try {
            u2.playliste = DRJson.parsePlayliste(new JSONArray(json));
            opdaterSenestSpillet(aq2, u2);
            return;
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
          }
          aq2.id(R.id.senest_spillet_container).gone();
        }
      });
      return;
    }


    if (u.playliste.size() > 0) {
      aq.id(R.id.senest_spillet_container).visible();
      Playlisteelement elem = u.playliste.get(0);
      aq.id(R.id.senest_spillet_kunstner).text(elem.kunstner);
      aq.id(R.id.senest_spillet_titel).text(elem.titel);
      // "http://api.discogs.com/image/A-3062379-1371611467-2166.jpeg"
      aq.id(R.id.senest_spillet_kunstnerbillede).image(skalérBilledeUrl(elem.billedeUrl, firkant, firkant));
    } else {
      aq.id(R.id.senest_spillet_container).gone();
    }
  }

  private void opdaterAktuelUdsendelse() {
    if (aktuelUdsendelseViewholder == null) return;
    try {
      Viewholder vh = aktuelUdsendelseViewholder;
      Udsendelse u = vh.udsendelse;
      long passeret = System.currentTimeMillis() - u.startTid.getTime() + FilCache.serverkorrektionTilKlienttidMs;
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
      Log.rapporterFejl(e);
    }
  }

  @Override
  public void onClick(View v) {
    new AlertDialog.Builder(getActivity())
//        .setAdapter(new ArrayAdapter(getActivity(), android.R.layout.select_dialog_singlechoice, kanal.streams), new DialogInterface.OnClickListener() {
        .setAdapter(new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, kanal.streams), new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            DRData.instans.aktuelKanal = kanal;
            DRData.instans.afspiller.setUrl(kanal.streams.get(which).url);
            DRData.instans.afspiller.startAfspilning();
          }
        }).show();
    /*
    DRData.instans.aktuelKanal = kanal;
    DRData.instans.afspiller.setUrl(kanal.streams.get(0).url);
    DRData.instans.afspiller.startAfspilning();
    */
  }

  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    if (position == 0) hentSendeplanForDag(new AQuery(rod), kanal.udsendelserPerDag.firstKey() - 1);
    else if (position == liste.size() - 1) hentSendeplanForDag(new AQuery(rod), kanal.udsendelserPerDag.firstKey() + 1);
    else startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtras(getArguments())  // Kanalkode
          .putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName()).putExtra(DRJson.Slug.name(), liste.get(position).slug)); // Udsenselses-ID
  }
/*
  private class UdsendelseClickListener implements View.OnClickListener {

    private final Viewholder viewHolder;

    public UdsendelseClickListener(Viewholder vh) {
      viewHolder = vh;
    }

    @Override
    public void onClick(View v) {
      if (aktuelUdsendelseViewholder == viewHolder) {
        DRData.instans.aktuelKanal = kanal;
        DRData.instans.afspiller.setUrl(kanal.streams.get(0).url);
      } else {
        String url = "http://www.dr.dk/tjenester/mu-apps/program/" + viewHolder.udsendelse.slug + "?type=radio&includeStreams=true";

      }
      DRData.instans.afspiller.startAfspilning();
    }
  }
  */
}

