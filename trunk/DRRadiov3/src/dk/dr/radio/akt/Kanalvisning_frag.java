package dk.dr.radio.akt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.akt.diverse.VisFragment_akt;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanalvisning_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener, Runnable {

  public static String P_kode = "kanal.kode";
  private ListView listView;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  private int aktuelUdsendelseIndex = -1;
  private Kanal kanal;
  protected View rod;
  private boolean fragmentErSynligt;
  private boolean p4;
  private int antalHentedeSendeplaner;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    String kanalkode = getArguments().getString(P_kode);
    p4 = Kanal.P4kode.equals(kanalkode);
    rod = null;

    if (p4) {
      kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_AF_BRUGER, null);
      if (kanalkode == null) {
        kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, "KH4");
        kanal = DRData.instans.stamdata.kanalFraKode.get(kanalkode);
        if (kanal == null) {
          Log.e("P4 IKKE FUNDET kanalkode=" + kanalkode, null);
          kanalkode = DRData.instans.stamdata.p4koder.get(0);
          kanal = DRData.instans.stamdata.kanalFraKode.get(kanalkode);
        }
        rod = inflater.inflate(R.layout.kanalvisning_p4_frag, container, false);
        AQuery aq = new AQuery(rod);
        aq.id(R.id.p4_vi_gætter_på_tekst).typeface(App.skrift_normal);
        aq.id(R.id.p4_kanalnavn).text(kanal.navn).typeface(App.skrift_fed);
        aq.id(R.id.p4_skift_distrikt).clicked(this).typeface(App.skrift_normal);
        aq.id(R.id.p4_ok).clicked(this).typeface(App.skrift_normal);
      }
    }
    kanal = DRData.instans.stamdata.kanalFraKode.get(kanalkode);

    if (rod == null) rod = inflater.inflate(R.layout.kanalvisning_frag, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_fed).getView());


    hentSendeplanForDag(aq, new Date(), true);
    udvikling_checkDrSkrifter(rod, this + " rod");
    return rod;
  }

  public static DateFormat datoFormat = new SimpleDateFormat("yyyy-MM-dd");


  private void hentSendeplanForDag(final AQuery aq, Date dato1, final boolean idag) {
//    String url = kanal.getUdsendelserUrl() + "/" + dag;
    final String dato = datoFormat.format(dato1);

    String url = kanal.getUdsendelserUrl() + "/date/" + dato;
    Log.d("hentSendeplanForDag url=" + url);


    // Cache værdier i en time
    App.sætErIGang(true);
    aq.ajax(url, String.class, 1000 * 60 * 60, new AjaxCallback<String>() {
      @Override
      public void callback(String url1, String json, AjaxStatus status) {
        App.sætErIGang(false);
        Log.d("hentSendeplanForDag url " + url1 + "   status=" + status.getCode());
        if (json != null && !"null".equals(json)) try {

          if (idag) {
            kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(json)), dato);
            opdaterListe(kanal.udsendelser);
            scrollTilAktuelUdsendelse();
          } else {
            // Nu ændres der i listen for at vise en dag før eller efter - sørg for at det synlige indhold ikke rykker sig
            Udsendelse næstøversteSynlig = liste.get(listView.getFirstVisiblePosition() + 1);
            Log.d("næstøversteSynlig = " + næstøversteSynlig);
            View v = listView.getChildAt(1);
            int næstøversteSynligOffset = (v == null) ? 0 : v.getTop();

            kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(json)), dato);
            opdaterListe(kanal.udsendelser);

            int næstøversteSynligNytIndex = liste.indexOf(næstøversteSynlig);
            listView.setSelectionFromTop(næstøversteSynligNytIndex, næstøversteSynligOffset);
          }

          return;
        } catch (JSONException e) {
          Log.d("PXXXarsefejl: " + e + " for json=" + json);
          e.printStackTrace();
        } catch (Exception e) {
          Log.rapporterFejl(e);
        }
        aq.id(R.id.tom).text(url1 + "   status=" + status.getCode() + "\njson=" + json);
      }
    });
  }

  private void scrollTilAktuelUdsendelse() {
    if (aktuelUdsendelseIndex < 0) return;
    int topmargen = getResources().getDimensionPixelOffset(R.dimen.kanalvisning_aktuelUdsendelse_topmargen);
    listView.setSelectionFromTop(aktuelUdsendelseIndex, topmargen);
  }


  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    Log.d(kanal + " setUserVisibleHint " + isVisibleToUser + "  " + this);
    fragmentErSynligt = isVisibleToUser;
    if (fragmentErSynligt) scrollTilAktuelUdsendelse();
    super.setUserVisibleHint(isVisibleToUser);
  }

  @Override
  public void onResume() {
    super.onResume();
    run();
    //App.forgrundstråd.postDelayed(this, 50);
  }

  @Override
  public void onPause() {
    super.onPause();
    App.forgrundstråd.removeCallbacks(this);
  }

  @Override
  public void run() {
    App.forgrundstråd.postDelayed(this, 15000);
    if (aktuelUdsendelseViewholder == null) return;
    Viewholder vh = aktuelUdsendelseViewholder;
    if (!vh.starttidbjælke.isShown() || !fragmentErSynligt) {
      Log.d(kanal + " opdaterAktuelUdsendelse starttidbjælke ikke synlig");
      return;
    }
    opdaterAktuelUdsendelse(vh);
    //MediaPlayer mp = DRData.instans.afspiller.getMediaPlayer();
    //Log.d("mp pos="+mp.getCurrentPosition() + "  af "+mp.getDuration());
  }


  private void opdaterListe(ArrayList<Udsendelse> nyuliste) {
    try {
      Log.d(kanal + " opdaterListe " + nyuliste.size());
      Date nu = new Date(); // TODO kompenser for forskelle mellem telefonens ur og serverens ur
      Log.d("opdaterListe " + kanal.kode + "  nu=" + nu);
      aktuelUdsendelseIndex = -1;
      Udsendelse tidligere = new Udsendelse("Tidligere");
      Udsendelse senere = new Udsendelse("Senere");
      tidligere.startTid = new Date(nyuliste.get(0).startTid.getTime() - 12 * 60 * 60 * 1000); // Døgnet starter kl 5, så vi er på den sikre side med 12 timer
      senere.startTid = nyuliste.get(nyuliste.size() - 1).slutTid;

      liste.clear();
      liste.add(tidligere);
      liste.addAll(nyuliste);
      // Nicolai: "jeg løber listen igennem fra bunden og op,
      // og så finder jeg den første der har starttid >= nuværende tid + sluttid <= nuværende tid."
      for (int n = liste.size() - 1; n > 1; n--) {
        Udsendelse u = liste.get(n);
        Log.d(n + " " + nu.after(u.startTid) + u.slutTid.before(nu) + "  " + u);
        if (u.startTid.before(nu) && nu.before(u.slutTid)) {
          aktuelUdsendelseIndex = n;
          break;
        }
      }
      liste.add(senere);
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    Log.d("opdaterListe " + kanal.kode + "  aktuelUdsendelseIndex=" + aktuelUdsendelseIndex);
    adapter.notifyDataSetChanged();
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
        v = getLayoutInflater(null).inflate(type == AKTUEL ? R.layout.kanalvisning_aktuel : type == TIDLIGERE_SENERE ? R.layout.element_tidligere_senere : R.layout.element_tid_titel_kunstner, parent, false);
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

        if (type == AKTUEL) {
          int br = bestemBilledebredde(listView, (View) a.id(R.id.billede).getView().getParent());
          String burl = skalérSlugBilledeUrl(u.slug, br, br * højde9 / bredde16);
          a.id(R.id.billede).width(br, false).image(burl, true, true, br, 0, null, AQuery.FADE_IN, (float) højde9 / bredde16);
          a.id(R.id.senest_spillet_overskrift).typeface(App.skrift_normal);
          a.id(R.id.senest_spillet_titel_og_kunstner).typeface(App.skrift_normal);
          a.id(R.id.lige_nu).typeface(App.skrift_normal);
          a.id(R.id.hør_live).typeface(App.skrift_normal);
          v.setBackgroundColor(getResources().getColor(R.color.hvid));
        }
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
      }
      udvikling_checkDrSkrifter(v, this.getClass() + " type=" + type);

      // Opdatér viewholderens data
      vh.udsendelse = u;
      vh.titel.setText(u.titel);
      if (type == TIDLIGERE_SENERE) {

        if (antalHentedeSendeplaner++ < 20) {
          a.id(R.id.progressBar).visible();   // De første 20 henter vi bare for brugeren
          vh.titel.setVisibility(View.VISIBLE);
          hentSendeplanForDag(new AQuery(rod), u.startTid, false);
        } else {
          a.id(R.id.progressBar).invisible(); // Derefter er det nok noget ser looper og brugeren må manuelt gøre det
          vh.titel.setVisibility(View.VISIBLE);
        }

        return v;
      }
      vh.startid.setText(u.startTidKl);

      if (type == AKTUEL) {
        aktuelUdsendelseViewholder = vh;
        vh.titel.setText(u.titel.toUpperCase());
        opdaterAktuelUdsendelse(vh);
        opdaterSenestSpillet(a, u);
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
    Log.d(kanal.kode + " opdaterSenestSpillet " + u);

    if (u.playliste == null) {
      // optimering - brug kun final i enkelte tilfælde. Final forårsager at variabler lægges i heap i stedet for stakken) at garbage collectoren skal køre fordi final
      final Udsendelse u2 = u;
      final AQuery aq2 = aq;
      String url = kanal.getPlaylisteUrl(u); // http://www.dr.dk/tjenester/mu-apps/playlist/monte-carlo-352/p3
      Log.d("Henter playliste " + url);
      App.sætErIGang(true);
      // før aq.ajax(url, String.class, 1 * 60 * 60 * 1000, men det er p.t. nødvendigt at spørge hele tiden da vi kun får op til lige nu
      aq.ajax(url, String.class, 15 * 1000, new AjaxCallback<String>() {
        @Override
        public void callback(String url, String json, AjaxStatus status) {
          App.sætErIGang(false);
          Log.d("XXX url " + url + "   status=" + status.getCode());
          if (json != null && !"null".equals(json)) try {
            u2.playliste = DRJson.parsePlayliste(new JSONArray(json));
            Log.d(kanal.kode + " parsePlayliste gav " + u2.playliste.size() + " elemener");
            opdaterSenestSpillet(aq2, u2);
            return;
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
            e.printStackTrace();
          }
          aq2.id(R.id.senest_spillet_container).gone();
        }
      });
      return;
    }


    if (u.playliste.size() > 0) {
      aq.id(R.id.senest_spillet_container).visible();
      Playlisteelement elem = u.playliste.get(0);
      aq.id(R.id.senest_spillet_titel_og_kunstner).text(Html.fromHtml("<b>" + elem.titel + "</b> &nbsp; | &nbsp;" + elem.kunstner));

      ImageView b = aq.id(R.id.senest_spillet_kunstnerbillede).getImageView();
      if (elem.billedeUrl.length() == 0) {
        aq.gone();
      } else {
        aq.visible().image(skalérDiscoBilledeUrl(elem.billedeUrl, b.getWidth(), b.getHeight()));
      }
    } else {
      aq.id(R.id.senest_spillet_container).gone();
    }
  }

  private void opdaterAktuelUdsendelse(Viewholder vh) {
    try {
      Udsendelse u = vh.udsendelse;
      long passeret = System.currentTimeMillis() - u.startTid.getTime() + FilCache.serverkorrektionTilKlienttidMs;
      long længde = u.slutTid.getTime() - u.startTid.getTime();
      int passeretPct = længde > 0 ? (int) (passeret * 100 / længde) : 0;
      Log.d(kanal.kode + " passeretPct=" + passeretPct + " af længde=" + længde);
      LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) vh.starttidbjælke.getLayoutParams();
      lp.weight = passeretPct;
      vh.starttidbjælke.setLayoutParams(lp);

      lp = (LinearLayout.LayoutParams) vh.slutttidbjælke.getLayoutParams();
      lp.weight = 100 - passeretPct;
      vh.slutttidbjælke.setLayoutParams(lp);
      if (passeretPct >= 100) { // Hop til næste udsendelse
        opdaterListe(kanal.udsendelser);
        scrollTilAktuelUdsendelse();
      }
      if (u.playliste != null && u.playliste.size() > 0) {
        opdaterSenestSpillet(vh.aq, u);
      }

    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.p4_skift_distrikt) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      startActivity(new Intent(getActivity(), Kanalvalg_akt.class));
    } else if (v.getId() == R.id.p4_ok) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      App.prefs.edit().putString(App.P4_FORETRUKKEN_AF_BRUGER, kanal.kode).commit();
    } else if (kanal.streams == null) {
      Log.rapporterOgvisFejl(getActivity(), new IllegalStateException("kanal.streams er null"));
    } else {
      if (App.udvikling) App.kortToast("kanal.streams=" + kanal.streams);
      new AlertDialog.Builder(getActivity())
          .setAdapter(new ArrayAdapter(getActivity(), R.layout.skrald_vaelg_streamtype, kanal.streams), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              DRData.instans.aktuelKanal = kanal;
              DRData.instans.afspiller.setUrl(kanal.streams.get(which).url);
              DRData.instans.afspiller.startAfspilning();
            }
          }).show();
    }
  }

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Udsendelse u = liste.get(position);
    if (position == 0 || position == liste.size() - 1) {
      hentSendeplanForDag(new AQuery(rod), u.startTid, false);
      v.findViewById(R.id.titel).setVisibility(View.GONE);
      v.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    } else {
      startActivity(new Intent(getActivity(), VisFragment_akt.class)
          .putExtra(P_kode, kanal.kode)
          .putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName()).putExtra(DRJson.Slug.name(), u.slug)); // Udsenselses-ID
    }
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

