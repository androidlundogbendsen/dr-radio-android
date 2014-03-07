package dk.dr.radio.akt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
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

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.flurry.android.FlurryAgent;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
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

  @Override
  public String toString() {
    return super.toString() + "/" + kanal;
  }

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
        rod = inflater.inflate(R.layout.kanal_p4_frag, container, false);
        AQuery aq = new AQuery(rod);
        aq.id(R.id.p4_vi_gætter_på_tekst).typeface(App.skrift_gibson);
        aq.id(R.id.p4_kanalnavn).text(kanal.navn).typeface(App.skrift_gibson_fed);
        aq.id(R.id.p4_skift_distrikt).clicked(this).typeface(App.skrift_gibson);
        aq.id(R.id.p4_ok).clicked(this).typeface(App.skrift_gibson);
      }
    }
    kanal = DRData.instans.stamdata.kanalFraKode.get(kanalkode);
    Log.d("onCreateView " + this);

    if (rod == null) rod = inflater.inflate(R.layout.kanal_frag, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson_fed).getView());

    // Hent sendeplan for den pågældende dag. Døgnskifte sker kl 5, så det kan være dagen før
    hentSendeplanForDag(aq, new Date(System.currentTimeMillis() - 5 * 60 * 60 * 1000), true);
    udvikling_checkDrSkrifter(rod, this + " rod");
    setHasOptionsMenu(true);
    DRData.instans.afspiller.observatører.add(this);
    App.netværk.observatører.add(this);
    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    DRData.instans.afspiller.observatører.remove(this);
    App.netværk.observatører.remove(this);

  }

  public static DateFormat datoFormat = new SimpleDateFormat("yyyy-MM-dd");

  private void hentSendeplanForDag(final AQuery aq, Date dag, final boolean idag) {
    final String dato = datoFormat.format(dag);

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
    if (kanal == null) return;
    if (fragmentErSynligt) {
      scrollTilAktuelUdsendelse();
      run();
      if (DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET && DRData.instans.afspiller.getLydkilde() != kanal) {
        DRData.instans.afspiller.setLydkilde(kanal);
      }
    } else {
      App.forgrundstråd.removeCallbacks(this);
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
  }

  @Override
  public void run() {
    App.forgrundstråd.removeCallbacks(this);
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

      aktuelUdsendelseIndex = kanal.getAktuelUdsendelseIndex() + 1;
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
                type == NORMAL ? R.layout.udsendelse_elem2_tid_titel_kunstner   // De andre udsendelser
                    : R.layout.kanal_elem_tidligere_senere, parent, false);
        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.startid = a.id(R.id.startid).typeface(App.skrift_gibson).getTextView();
        vh.starttidbjælke = a.id(R.id.starttidbjælke).getView();
        vh.slutttidbjælke = a.id(R.id.slutttidbjælke).getView();
        //a.id(R.id.højttalerikon).clicked(new UdsendelseClickListener(vh));
        a.id(R.id.slutttid).typeface(App.skrift_gibson).text(udsendelse.slutTidKl);
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
          final View hør = a.id(R.id.hør_live).getView();
          hør.post(new Runnable() {
            @Override
            public void run() {
              Rect r = new Rect();
              hør.getHitRect(r);
              int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);
              r.top -= udvid;
              r.bottom += udvid;
              r.right += udvid;
              r.left -= udvid;
              Log.d("hør_udvidet_klikområde=" + r);
              ((View) hør.getParent()).setTouchDelegate(new TouchDelegate(r, hør));
            }
          });
          /*
          final View parent2 = (View) hør.getParent();
          parent2.post( new Runnable() {
            // Post in the parent's message queue to make sure the parent
            // lays out its children before we call getHitRect()
            public void run() {
              final Rect r = new Rect();
              hør.getHitRect(r);
              r.top -= 40;
              r.bottom += 40;
              parent2.setTouchDelegate( new TouchDelegate( r , hør));
            }
          });
    */
          v.setBackgroundResource(R.drawable.knap_hvid_bg);
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

          opdaterAktuelUdsendelse(vh);
          opdaterSenestSpillet(a, udsendelse);
          break;
        case NORMAL:
          vh.startid.setText(udsendelse.startTidKl);
          vh.titel.setText(udsendelse.titel);
          a.id(R.id.stiplet_linje).visibility(position == aktuelUdsendelseIndex + 1 ? View.INVISIBLE : View.VISIBLE);
          a.id(R.id.hør).visibility(udsendelse.kanHøres ? View.VISIBLE : View.GONE);
          break;
        case TIDLIGERE_SENERE:
          vh.titel.setText(udsendelse.titel);

          if (antalHentedeSendeplaner++ < 7) {
            a.id(R.id.progressBar).visible();   // De første 7 henter vi bare for brugeren
            vh.titel.setVisibility(View.VISIBLE);
            hentSendeplanForDag(new AQuery(rod), udsendelse.startTid, false);
          } else {
            a.id(R.id.progressBar).invisible(); // Derefter må brugeren gøre det manuelt
            vh.titel.setVisibility(View.VISIBLE);
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
          Log.d(kanal.kode + " opdaterSenestSpillet url " + url + "   status=" + status.getCode());
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
      aq.id(R.id.titel_og_kunstner).text(Html.fromHtml("<b>" + elem.titel + "</b> &nbsp; | &nbsp;" + elem.kunstner));

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
        opdaterListe();
        scrollTilAktuelUdsendelse();
      }
      if (u.playliste != null && u.playliste.size() > 0) {
        opdaterSenestSpillet(vh.aq, u);
      }

      boolean spillerDenneKanal = DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET && DRData.instans.afspiller.getLydkilde() == kanal;
      boolean online = App.netværk.erOnline();
      vh.aq.id(R.id.hør_live).enabled(!spillerDenneKanal && online).text(
          !online ? "Internetforbindelse mangler" :
              (spillerDenneKanal ? " SPILLER " : " HØR ") + kanal.navn.toUpperCase() + " LIVE");

    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.p4_skift_distrikt) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      //startActivity(new Intent(getActivity(), Kanalvalg_akt.class));

      //startActivity(new Intent(getActivity(), VisFragment_akt.class)
      //    .putExtra(VisFragment_akt.KLASSE, KanalerP4_frag.class.getName()));

      //KanalerP4_frag f = new KanalerP4_frag();
      //FragmentManager fragmentManager = getFragmentManager();
      //fragmentManager.beginTransaction().replace( R.id.indhold_frag, f).commit();

    } else if (v.getId() == R.id.p4_ok) {
      rod.findViewById(R.id.p4_vi_gætter_på_dialog).setVisibility(View.GONE);
      App.prefs.edit().putString(App.P4_FORETRUKKEN_AF_BRUGER, kanal.kode).commit();
    } else if (kanal.streams == null) {
      Log.rapporterOgvisFejl(getActivity(), new IllegalStateException("kanal.streams er null"));
    } else {
      // hør_udvidet_klikområde eller hør
      hør();
    }
  }

  private void hør() {
    if (!App.EMULATOR) {
      HashMap<String, String> param = new HashMap<String, String>();
      param.put("kanal", kanal.kode);
      FlurryAgent.logEvent("hør live kanal", param);
    }

    if (App.udvikling) App.kortToast("kanal.streams=" + kanal.streams);
    if (App.prefs.getBoolean("manuelStreamvalg", false)) {
      new AlertDialog.Builder(getActivity())
          .setAdapter(new ArrayAdapter(getActivity(), R.layout.skrald_vaelg_streamtype, kanal.findBedsteStreams(false).toArray()), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              kanal.streams.get(which).foretrukken = true;
              DRData.instans.aktuelKanal = kanal;
              DRData.instans.afspiller.setLydkilde(kanal);
              DRData.instans.afspiller.startAfspilning();
            }
          }).show();
    } else {
      DRData.instans.aktuelKanal = kanal;
      DRData.instans.afspiller.setLydkilde(kanal);
      DRData.instans.afspiller.startAfspilning();
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
      //startActivity(new Intent(getActivity(), VisFragment_akt.class)
      //    .putExtra(P_kode, kanal.kode)
      //    .putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName()).putExtra(DRJson.Slug.name(), u.slug)); // Udsenselses-ID

      Fragment f = new Udsendelse_frag();
      f.setArguments(new Intent()
          .putExtra(P_kode, kanal.kode)
          .putExtra(DRJson.Slug.name(), u.slug).getExtras());
      //getFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
      //getChildFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
      getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
    }
  }
}

