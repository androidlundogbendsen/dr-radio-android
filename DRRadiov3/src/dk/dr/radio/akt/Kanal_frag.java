package dk.dr.radio.akt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.androidquery.AQuery;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanal_frag extends Basisfragment implements AdapterView.OnItemClickListener, View.OnClickListener, Runnable {

  private ListView listView;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  private int aktuelUdsendelseIndex = -1;
  private Kanal kanal;
  protected View rod;
  private boolean fragmentErSynligt;
  private boolean p4;
  private int antalHentedeSendeplaner;
  public static Kanal_frag senesteSynligeFragment;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(this + " onCreateView startet efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    String kanalkode = getArguments().getString(P_kode);
    p4 = Kanal.P4kode.equals(kanalkode);
    rod = null;

    if (p4) {
      kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_AF_BRUGER, null);
      if (kanalkode == null) {
        kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_GÆT_FRA_STEDPLACERING, "KH4");
        kanal = DRData.instans.grunddata.kanalFraKode.get(kanalkode);
        if (kanal == null) {
          Log.e("P4 IKKE FUNDET kanalkode=" + kanalkode, null);
          kanalkode = DRData.instans.grunddata.p4koder.get(0);
          kanal = DRData.instans.grunddata.kanalFraKode.get(kanalkode);
        }
        rod = inflater.inflate(R.layout.kanal_p4_frag, container, false);
        AQuery aq = new AQuery(rod);
        aq.id(R.id.p4_vi_gætter_på_tekst).typeface(App.skrift_gibson);
        aq.id(R.id.p4_kanalnavn).text(kanal.navn).typeface(App.skrift_gibson_fed);
        aq.id(R.id.p4_skift_distrikt).clicked(this).typeface(App.skrift_gibson);
        aq.id(R.id.p4_ok).clicked(this).typeface(App.skrift_gibson);
      }
    }
    kanal = DRData.instans.grunddata.kanalFraKode.get(kanalkode);
    Log.d(this + " onCreateView 2 efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    if (rod == null) rod = inflater.inflate(R.layout.kanal_frag, container, false);
    if (kanal == null) {
      afbrydManglerData();
      return rod;
    }

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());

    Log.d(this + " onCreateView 3 efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    // Hent sendeplan for den pågældende dag. Døgnskifte sker kl 5, så det kan være dagen før
    hentSendeplanForDag(new Date(App.serverCurrentTimeMillis() - 5 * 60 * 60 * 1000), true);
    Log.d(this + " onCreateView 4 efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    udvikling_checkDrSkrifter(rod, this + " rod");
    DRData.instans.afspiller.observatører.add(this);
    App.netværk.observatører.add(this);
    Log.d(this + " onCreateView færdig efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    DRData.instans.afspiller.observatører.remove(this);
    App.netværk.observatører.remove(this);
  }


  private void hentSendeplanForDag(Date dag, final boolean idag) {
    final String dato = DRJson.apiDatoFormat.format(dag);
    if (kanal.harUdsendelserForDag(dato)) {
      opdaterListe();
    }

    final String url = kanal.getUdsendelserUrl() + "/date/" + dato;
    Log.d("hentSendeplanForDag url=" + url + " efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        Log.d("fikSvar(" + fraCache + " " + url);
        if (getActivity() == null || uændret) return;
        Log.d("hentSendeplanForDag url " + url + " fraCache=" + fraCache + " efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
        if (json != null && !"null".equals(json)) {
          if (idag) {
            kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(json), kanal, DRData.instans), dato);
            opdaterListe();
            scrollTilAktuelUdsendelse();
          } else {
            // Nu ændres der i listen for at vise en dag før eller efter - sørg for at det synlige indhold ikke rykker sig
            Udsendelse næstøversteSynlig = liste.get(listView.getFirstVisiblePosition() + 1);
            Log.d("næstøversteSynlig = " + næstøversteSynlig);
            View v = listView.getChildAt(1);
            int næstøversteSynligOffset = (v == null) ? 0 : v.getTop();

            kanal.setUdsendelserForDag(DRJson.parseUdsendelserForKanal(new JSONArray(json), kanal, DRData.instans), dato);
            opdaterListe();

            int næstøversteSynligNytIndex = liste.indexOf(næstøversteSynlig);
            listView.setSelectionFromTop(næstøversteSynligNytIndex, næstøversteSynligOffset);
          }
          return;
        }
        new AQuery(rod).id(R.id.tom).text("Netværksfejl, prøv igen senere");
      }
    }) {
      public Priority getPriority() {
        return fragmentErSynligt ? Priority.NORMAL : Priority.LOW;
      }
    }.setTag(this);
    //Log.d("hentSendeplanForDag 2 " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    App.volleyRequestQueue.add(req);
  }

  public void scrollTilAktuelUdsendelse() {
    Log.d(this + " scrollTilAktuelUdsendelse()");
    if (aktuelUdsendelseIndex < 0) return;
    int topmargen = getResources().getDimensionPixelOffset(R.dimen.kanalvisning_aktuelUdsendelse_topmargen);
    listView.setSelectionFromTop(aktuelUdsendelseIndex, topmargen);
  }


  public void scrollTilAktuelUdsendelseBlødt() {
    Log.d(this + " scrollTilAktuelUdsendelseBlødt()");
    if (aktuelUdsendelseIndex < 0) return;
    int topmargen = getResources().getDimensionPixelOffset(R.dimen.kanalvisning_aktuelUdsendelse_topmargen);
    if (Build.VERSION.SDK_INT >= 11) listView.smoothScrollToPositionFromTop(aktuelUdsendelseIndex, topmargen);
    else listView.setSelectionFromTop(aktuelUdsendelseIndex, topmargen);
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    Log.d(kanal + " QQQ setUserVisibleHint " + isVisibleToUser + "  " + this);
    fragmentErSynligt = isVisibleToUser;
    if (fragmentErSynligt) {
      senesteSynligeFragment = this;
      App.forgrundstråd.post(this); // Opdatér lidt senere, efter onCreateView helt sikkert har kørt
      App.forgrundstråd.post(new Runnable() {
        @Override
        public void run() {
          //scrollTilAktuelUdsendelse();
          if (DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET && DRData.instans.afspiller.getLydkilde() != kanal) {
            DRData.instans.afspiller.setLydkilde(kanal);
          }
        }
      });
    } else {
      App.forgrundstråd.removeCallbacks(this);
      if (senesteSynligeFragment == this) senesteSynligeFragment = null;
    }
    super.setUserVisibleHint(isVisibleToUser);
  }

  @Override
  public void onResume() {
    super.onResume();
    //App.forgrundstråd.postDelayed(this, 50);
  }

  @Override
  public void onPause() {
    super.onPause();
    App.forgrundstråd.removeCallbacks(this);
    if (senesteSynligeFragment == this) senesteSynligeFragment = null;
    Log.d(this + " onPause() " + this);
  }

  @Override
  public void run() {
    Log.d(this+" run() synlig="+fragmentErSynligt);
    App.forgrundstråd.removeCallbacks(this);
    App.forgrundstråd.postDelayed(this, 15000);

    if (kanal.streams == null && App.erOnline()) {
      Request<?> req = new DrVolleyStringRequest(kanal.getStreamsUrl(), new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (uændret) return; // ingen grund til at parse det igen
          JSONObject o = new JSONObject(json);
          kanal.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
          Log.d("hentSupplerendeDataBg " + kanal.kode + " fraCache=" + fraCache + " => " + kanal.slug + " k.lydUrl=" + kanal.streams);
          run(); // Opdatér igen
        }
      }) {
        public Priority getPriority() {
          return fragmentErSynligt ? Priority.HIGH : Priority.NORMAL;
        }
      };
      App.volleyRequestQueue.add(req);
    }


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


  private void opdaterListe() {
    try {
      Udsendelse tidligere = new Udsendelse("Tidligere");
      Udsendelse senere = new Udsendelse("Senere");
      ArrayList<Udsendelse> nyuliste = kanal.udsendelser;
      Log.d(kanal + " opdaterListe " + nyuliste.size());
      tidligere.startTid = new Date(nyuliste.get(0).startTid.getTime() - 12 * 60 * 60 * 1000); // Døgnet starter kl 5, så vi er på den sikre side med 12 timer
      senere.startTid = nyuliste.get(nyuliste.size() - 1).slutTid;

      liste.clear();
      liste.add(tidligere);
      liste.addAll(nyuliste);
      liste.add(senere);
      aktuelUdsendelseIndex = kanal.getAktuelUdsendelseIndex() + 1;
      aktuelUdsendelseViewholder = null;
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
      if (position == 0 || position == liste.size() - 1) return TIDLIGERE_SENERE;
      if (position == aktuelUdsendelseIndex) return AKTUEL;
      return NORMAL;
    }

    static final int NORMAL = 0;
    static final int AKTUEL = 1;
    static final int TIDLIGERE_SENERE = 2;

    boolean TITELTEKST_KUN_SORT_LIGE_BAG_TEKST = App.prefs.getBoolean("kunSortLigeBagTekst", false);

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Viewholder vh;
      AQuery a;
      int type = getItemViewType(position);
      Udsendelse udsendelse = liste.get(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(
            type == AKTUEL ? R.layout.kanal_elem_aktuel :        // Visning af den aktuelle udsendelse
                type == NORMAL ? R.layout.elem_tid_titel_kunstner   // De andre udsendelser
                    : R.layout.kanal_elem_tidligere_senere, parent, false);
        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.startid = a.id(R.id.startid).typeface(App.skrift_gibson).getTextView();
        vh.starttidbjælke = a.id(R.id.starttidbjælke).getView();
        vh.slutttidbjælke = a.id(R.id.slutttidbjælke).getView();
        //a.id(R.id.højttalerikon).clicked(new UdsendelseClickListener(vh));
        a.id(R.id.slutttid).typeface(App.skrift_gibson);
        if (type == TIDLIGERE_SENERE) {
          vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
        } else if (type == AKTUEL) {
          vh.titel = a.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
          a.id(R.id.senest_spillet_overskrift).typeface(App.skrift_gibson);
          a.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson);
          a.id(R.id.lige_nu).typeface(App.skrift_gibson);
          a.id(R.id.hør_live).typeface(App.skrift_gibson).clicked(Kanal_frag.this);
          // Knappen er meget vigtig, og har derfor et udvidet område hvor det også er den man rammer
          // se http://developer.android.com/reference/android/view/TouchDelegate.html
          final int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);
          final View hør = a.id(R.id.hør_live).getView();
          hør.post(new Runnable() {
            @Override
            public void run() {
              Rect r = new Rect();
              hør.getHitRect(r);
              r.top -= udvid;
              r.bottom += udvid;
              r.right += udvid;
              r.left -= udvid;
              //Log.d("hør_udvidet_klikområde=" + r);
              ((View) hør.getParent()).setTouchDelegate(new TouchDelegate(r, hør));
            }
          });
          v.setBackgroundResource(R.drawable.knap_hvid_bg);
          a.id(R.id.senest_spillet_container).invisible(); // Start uden 'senest spillet, indtil vi har info
        } else {
          vh.titel = a.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson_fed).getTextView();
        }
        v.setTag(vh);
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
      }
      udvikling_checkDrSkrifter(v, this.getClass() + " type=" + type);

      // Opdatér viewholderens data
      vh.udsendelse = udsendelse;
      switch (type) {
        case AKTUEL:
          aktuelUdsendelseViewholder = vh;
          vh.startid.setText(udsendelse.startTidKl);
          a.id(R.id.slutttid).text(udsendelse.slutTidKl);
          vh.titel.setText(udsendelse.titel);

          int br = bestemBilledebredde(listView, (View) a.id(R.id.billede).getView().getParent(), 100);
          int hø = br * højde9 / bredde16;
          String burl = skalérSlugBilledeUrl(udsendelse.slug, br, hø);
          a.width(br, false).height(hø, false).image(burl, true, true, br, 0, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          if (TITELTEKST_KUN_SORT_LIGE_BAG_TEKST) {
            vh.titel.setBackgroundColor(0);
            Spannable spanna = new SpannableString(udsendelse.titel.toUpperCase());
            spanna.setSpan(new BackgroundColorSpan(0xFF000000), 0, udsendelse.titel.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            vh.titel.setText(spanna);
          } else {
            vh.titel.setText(udsendelse.titel.toUpperCase());
          }

          opdaterAktuelUdsendelse(aktuelUdsendelseViewholder);
          opdaterSenestSpillet2(a, udsendelse);
          break;
        case NORMAL:
          vh.startid.setText(udsendelse.startTidKl);
          vh.titel.setText(udsendelse.titel);
          a.id(R.id.stiplet_linje).visibility(position == aktuelUdsendelseIndex + 1 ? View.INVISIBLE : View.VISIBLE);
          vh.titel.setTextColor(udsendelse.kanHøres ? Color.BLACK : App.color.grå60);
          break;
        case TIDLIGERE_SENERE:
          vh.titel.setText(udsendelse.titel);

          if (antalHentedeSendeplaner++ < 7) {
            a.id(R.id.progressBar).visible();   // De første 7 henter vi bare for brugeren
            vh.titel.setVisibility(View.VISIBLE);
            hentSendeplanForDag(udsendelse.startTid, false);
          } else {
            a.id(R.id.progressBar).invisible(); // Derefter må brugeren gøre det manuelt
            vh.titel.setVisibility(View.VISIBLE);
          }
      }


      return v;
    }
  };


  private void opdaterSenestSpillet2(AQuery aq, Udsendelse u) {
    Log.d("DDDDD opdaterSenestSpillet2 "+u.playliste);
    if (u.playliste != null && u.playliste.size() > 0) {
      aq.id(R.id.senest_spillet_container).visible();
      Playlisteelement elem = u.playliste.get(0);
//      aq.id(R.id.titel_og_kunstner).text(Html.fromHtml("<b>" + elem.titel + "</b> &nbsp; | &nbsp;" + elem.kunstner));

      aq.id(R.id.titel_og_kunstner).text(lavFedSkriftTil(elem.titel + "  |  " + elem.kunstner, elem.titel.length()));

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
      long passeret = App.serverCurrentTimeMillis() - u.startTid.getTime();
      long længde = u.slutTid.getTime() - u.startTid.getTime();
      int passeretPct = længde > 0 ? (int) (passeret * 100 / længde) : 0;
      //Log.d(getKanal.kode + " passeretPct=" + passeretPct + " af længde=" + længde);
      LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) vh.starttidbjælke.getLayoutParams();
      lp.weight = passeretPct;
      vh.starttidbjælke.setLayoutParams(lp);

      lp = (LinearLayout.LayoutParams) vh.slutttidbjælke.getLayoutParams();
      lp.weight = 100 - passeretPct;
      vh.slutttidbjælke.setLayoutParams(lp);
      if (passeretPct >= 100) { // Hop til næste udsendelse
        opdaterListe();
        if (vh.starttidbjælke.isShown()) scrollTilAktuelUdsendelseBlødt();
      }

      boolean spillerDenneKanal = DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET && DRData.instans.afspiller.getLydkilde() == kanal;
      boolean online = App.netværk.erOnline();
      vh.aq.id(R.id.hør_live).enabled(!spillerDenneKanal && online && kanal.streams != null)
          .text(!online ? "Internetforbindelse mangler" :
              (spillerDenneKanal ? " SPILLER " : " HØR ") + kanal.navn.toUpperCase() + " LIVE");

      if (u.playliste == null) {
        // optimering - brug kun final i enkelte tilfælde. Final forårsager at variabler lægges i heap i stedet for stakken) at garbage collectoren skal køre fordi final
        final Udsendelse u2 = u;
        final AQuery aq2 = vh.aq;
        final String url = kanal.getPlaylisteUrl(u); // http://www.dr.dk/tjenester/mu-apps/playlist/monte-carlo-352/p3
        Log.d("Henter playliste " + url);
        Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
          @Override
          public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
            Log.d(kanal.kode + " opdaterSenestSpillet url " + url);
            if (getActivity() == null  || uændret) return;
            if (json != null && !"null".equals(json)) {
              u2.playliste = DRJson.parsePlayliste(new JSONArray(json));
              Log.d(kanal.kode + " parsePlayliste gav " + u2.playliste.size() + " elemener");
            }
            opdaterSenestSpillet2(aq2, u2);
          }
        }) {
          public Priority getPriority() {
            return fragmentErSynligt ? Priority.NORMAL : Priority.LOW;
          }
        }.setTag(this);
        App.volleyRequestQueue.add(req);
      }

    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.p4_skift_distrikt) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, new P4kanalvalg_frag())
          .commit();

    } else if (v.getId() == R.id.p4_ok) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      App.prefs.edit().putString(App.P4_FORETRUKKEN_AF_BRUGER, kanal.kode).commit();
    } else if (kanal.streams == null) {
      Log.rapporterOgvisFejl(getActivity(), new IllegalStateException("kanal.streams er null"));
    } else {
      // hør_udvidet_klikområde eller hør
      hør(kanal, getActivity());
      Log.registrérTestet("Afspilning af direkte udsendelse", kanal.kode);
    }
  }

  public static void hør(final Kanal kanal, Activity akt) {
    if (App.fejlsøgning) App.kortToast("kanal.streams=" + kanal.streams);
    if (App.prefs.getBoolean("manuelStreamvalg", false)) {
      kanal.nulstilForetrukkenStream();
      final List<Lydstream> lydstreamList = kanal.findBedsteStreams(false);
      new AlertDialog.Builder(akt)
          .setAdapter(new ArrayAdapter(akt, R.layout.skrald_vaelg_streamtype, lydstreamList), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              lydstreamList.get(which).foretrukken = true;
              DRData.instans.afspiller.setLydkilde(kanal);
              DRData.instans.afspiller.startAfspilning();
            }
          }).show();
    } else {
      DRData.instans.afspiller.setLydkilde(kanal);
      DRData.instans.afspiller.startAfspilning();
    }
  }

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Udsendelse u = liste.get(position);
    if (position == 0 || position == liste.size() - 1) {
      hentSendeplanForDag(u.startTid, false);
      v.findViewById(R.id.titel).setVisibility(View.GONE);
      v.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    } else {
      //startActivity(new Intent(getActivity(), VisFragment_akt.class)
      //    .putExtra(P_kode, getKanal.kode)
      //    .putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName()).putExtra(DRJson.Slug.name(), u.slug)); // Udsenselses-ID
      String aktuelUdsendelseSlug = aktuelUdsendelseIndex > 0 ? liste.get(aktuelUdsendelseIndex).slug : "";


      Fragment f =
          App.prefs.getBoolean("udsendelser_bladr_ikke", false) ? new Udsendelse_frag() :
              App.prefs.getBoolean("udsendelser_vandret_skift", false) ? new Udsendelser_vandret_skift_frag() :
                  new Udsendelser_lodret_skift_frag();
      f.setArguments(new Intent()
          .putExtra(P_kode, kanal.kode)
          .putExtra(Udsendelse_frag.AKTUEL_UDSENDELSE_SLUG, aktuelUdsendelseSlug)
          .putExtra(DRJson.Slug.name(), u.slug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();
    }
  }
}

