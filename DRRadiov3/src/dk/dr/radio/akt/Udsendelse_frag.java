package dk.dr.radio.akt;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.androidquery.AQuery;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;

public class Udsendelse_frag extends Basisfragment implements View.OnClickListener, AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener, Runnable {

  public static final String BLOKER_VIDERE_NAVIGERING = "BLOKER_VIDERE_NAVIGERING";
  public static final String AKTUEL_UDSENDELSE_SLUG = "AKTUEL_UDSENDELSE_SLUG";
  private ListView listView;
  private Kanal kanal;
  protected View rod;
  private Udsendelse udsendelse;
  private boolean blokerVidereNavigering;
  private ArrayList<Object> liste = new ArrayList<Object>();
  Afspiller afspiller = DRData.instans.afspiller;
  private TextView seekBarTekst;
  private TextView seekBarMaxTekst;
  private boolean seekBarBetjenesAktivt;
  private SeekBar seekBar;
  private boolean fragmentErSynligt;

  private static HashMap<Udsendelse, Long> streamsVarTom = new HashMap<Udsendelse, Long>();
  private int antalGangeForsøgtHentet;
  private Runnable hentStreams = new Runnable() {
    @Override
    public void run() {
      if (udsendelse.hentetStream == null && (udsendelse.streams==null || udsendelse.streams.size()==0) && antalGangeForsøgtHentet++<1) {
        Request<?> req = new DrVolleyStringRequest(udsendelse.getStreamsUrl(), new DrVolleyResonseListener() {
          @Override
          public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
            if (uændret) return;
            if (json != null && !"null".equals(json)) {
              JSONObject o = new JSONObject(json);
              udsendelse.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
              udsendelse.kanHøres = udsendelse.streamsKlar();
              if (udsendelse.streams.size()==0) {
                Log.d("SSSSS TOMME STREAMS ... men det passer måske ikke! for "+udsendelse.slug+" " +udsendelse.getStreamsUrl());
                streamsVarTom.put(udsendelse, System.currentTimeMillis());
                //App.volleyRequestQueue.getCache().remove(url);
                App.forgrundstråd.postDelayed(hentStreams, 5000);
              } else if (streamsVarTom.containsKey(udsendelse)) {
                long t0 = streamsVarTom.get(udsendelse);
                if (!App.PRODUKTION){
                  App.langToast("Serveren har ombestemt sig, nu er streams ikke mere tom for " + udsendelse.slug);
                  App.langToast("Tidsforskel mellem de to svar: " + (System.currentTimeMillis() - t0) / 1000 + " sek");
                }
                Log.rapporterFejl(new Exception("Server ombestemte sig, der var streams alligevel - for "+udsendelse.slug+"  dt="+(System.currentTimeMillis()-t0)));
                streamsVarTom.remove(udsendelse);
              }
              udsendelse.produktionsnummer = o.optString(DRJson.ProductionNumber.name());
                udsendelse.ShareLink = o.optString(DRJson.ShareLink.name());
              if (fragmentErSynligt && udsendelse.streamsKlar() && afspiller.getAfspillerstatus() == Status.STOPPET) {
                afspiller.setLydkilde(udsendelse);
              }
              adapter.notifyDataSetChanged(); // Opdatér views
            }
          }
        }).setTag(this);
        App.volleyRequestQueue.add(req);
      }
    }
  };

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + udsendelse;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    kanal = DRData.instans.grunddata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    udsendelse = DRData.instans.udsendelseFraSlug.get(getArguments().getString(DRJson.Slug.name()));
    if (udsendelse == null) {
      afbrydManglerData();
      return rod;
    }
    if (kanal == null) kanal = udsendelse.getKanal();
    if ("".equals(kanal.slug)) {
      Log.d("Kender ikke kanalen");
    }

    blokerVidereNavigering = getArguments().getBoolean(BLOKER_VIDERE_NAVIGERING);

    Log.d("onCreateView " + this);

    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);

    tjekOmHentet(udsendelse);
    hentStreams.run();

//    if (streamsKlar() && DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
//      DRData.instans.afspiller.setLydkilde(udsendelse);
//    }

    setHasOptionsMenu(true);
    bygListe();

    afspiller.observatører.add(this);
    DRData.instans.hentedeUdsendelser.observatører.add(this);
    udvikling_checkDrSkrifter(rod, this + " rod");
    return rod;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (aktuelUdsendelsePåKanalen() || udsendelse.playliste == null) opdaterSpillelisteRunnable.run();
  }

  private boolean aktuelUdsendelsePåKanalen() {
    boolean res = udsendelse.equals(udsendelse.getKanal().getUdsendelse());
    Log.d("aktuelUdsendelsePåKanalen()? "+res+" "+udsendelse+" "+udsendelse.getKanal()+":"+udsendelse.getKanal().getUdsendelse());
    return res;
  }

  private void startOpdaterSpilleliste() {
    if ("".equals(kanal.slug)) {
      Log.e(new Exception("Kender ikke kanalen"));
      return;
    }
    //new Exception("startOpdaterSpilleliste() for "+this).printStackTrace();
    Request<?> req = new DrVolleyStringRequest(kanal.getPlaylisteUrl(udsendelse), new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (App.fejlsøgning) Log.d("fikSvar playliste(" + fraCache + " " + url + "   " + this);
        if (uændret) return;
        if (json != null && !"null".equals(json)) {
          udsendelse.playliste = DRJson.parsePlayliste(new JSONArray(json));
          bygListe();
        }
      }
    }) {
      @Override
      public Priority getPriority() {
        return Priority.LOW; // Det vigtigste er at hente streams, spillelisten er knapt så vigtig
      }
    }.setTag(this);
    App.volleyRequestQueue.add(req);
  }

  Runnable opdaterSpillelisteRunnable = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(opdaterSpillelisteRunnable);
      startOpdaterSpilleliste();
      if (aktuelUdsendelsePåKanalen() && isResumed()) {
        App.forgrundstråd.postDelayed(opdaterSpillelisteRunnable, 15000);
      }
    }
  };

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    //Log.d(" QQQ setUserVisibleHint " + isVisibleToUser + "  " + this);
    fragmentErSynligt = isVisibleToUser;
    if (fragmentErSynligt) {
      App.forgrundstråd.post(new Runnable() {
        @Override
        public void run() {
          if (udsendelse.kanHøres && afspiller.getAfspillerstatus() == Status.STOPPET) {
            afspiller.setLydkilde(udsendelse);
          }
        }
      });
    }
    super.setUserVisibleHint(isVisibleToUser);
  }

  private static void tjekOmHentet(Udsendelse udsendelse) {
    if (udsendelse.hentetStream == null) {
      if (!DRData.instans.hentedeUdsendelser.virker()) return;
      Cursor c = DRData.instans.hentedeUdsendelser.getStatusCursor(udsendelse);
      if (c == null) return;
      try {
        Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
        Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
        Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
        Log.d(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
        Log.d(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
        Log.d(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));

        if (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL)
          return;
        String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
        File file = new File(URI.create(uri).getPath());
        if (file.exists()) {
          udsendelse.hentetStream = new Lydstream();
          udsendelse.hentetStream.url = uri;
          udsendelse.hentetStream.score = 500; // Rigtig god!
          udsendelse.kanHøres = true;
          Log.registrérTestet("Afspille hentet udsendelse", udsendelse.slug);
        } else {
//          Log.rapporterFejl(new IllegalStateException("Fil " + file + "  fandtes ikke alligevel??! for " + udsendelse));
          Log.rapporterFejl(new IllegalStateException("Fil " + file + "  fandtes ikke alligevel??!"));
        }
      } finally {
        c.close();
      }
    }
  }

  @Override
  public void onDestroyView() {
    App.volleyRequestQueue.cancelAll(this);
    afspiller.observatører.remove(this);
    DRData.instans.hentedeUdsendelser.observatører.add(this);
    super.onDestroyView();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.udsendelse, menu);
    //menu.findItem(R.id.hør).setVisible(udsendelse.kanHøres).setEnabled(streamsKlar());
    //menu.findItem(R.id.hent).setVisible(DRData.instans.hentedeUdsendelser.virker() && udsendelse.kanHøres && udsendelse.hentetStream==null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.hør) {
      hør();
    } else if (item.getItemId() == R.id.hent) {
      hent();
    } else if (item.getItemId() == R.id.del) {
      del();
    } else return super.onOptionsItemSelected(item);
    return true;
  }

  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView startid;
    public Playlisteelement playlisteelement;
  }

  static final int TOP = 0;
  static final int PLAYLISTE_OVERSKRIFT_PLAYLISTE_INFO = 1;
  static final int PLAYLISTEELEM_NU = 2;
  static final int PLAYLISTEELEM = 3;
  static final int INFOTEKST = 4;
  static final int VIS_HELE_PLAYLISTEN_KNAP = 5;
  static final int ALLE_UDSENDELSER = 6;

  static final int[] layoutFraType = {
      R.layout.udsendelse_elem0_top,
      R.layout.udsendelse_elem1_overskrift_playliste_info,
      R.layout.udsendelse_elem2_playlisteelem_nu,
      R.layout.udsendelse_elem3_playlisteelem,
      R.layout.udsendelse_elem4_infotekst,
      R.layout.udsendelse_elem5_vis_hele_playlisten_knap,
      R.layout.udsendelse_elem6_alle_udsendelser};

  boolean visInfo = false;
  boolean visHelePlaylisten = false;

  void bygListe() {
    liste.clear();
    liste.add(TOP);
    if (visInfo) {
      liste.add(PLAYLISTE_OVERSKRIFT_PLAYLISTE_INFO);
      liste.add(INFOTEKST);
    } else {
      if (udsendelse.playliste != null && udsendelse.playliste.size() > 0) {
        liste.add(PLAYLISTE_OVERSKRIFT_PLAYLISTE_INFO);
        boolean aktuelUdsendelsePåKanalen = aktuelUdsendelsePåKanalen();
        if (visHelePlaylisten) {
          if (aktuelUdsendelsePåKanalen) udsendelse.playliste.get(0).spillerNu = true;
          liste.addAll(udsendelse.playliste);
        } else {
          for (int i = 0; i < udsendelse.playliste.size(); i++) {
            Playlisteelement e = udsendelse.playliste.get(i);
            e.spillerNu = (i == 0 && aktuelUdsendelsePåKanalen);
            liste.add(e);
            if (i >= 4) {
              liste.add(VIS_HELE_PLAYLISTEN_KNAP);
              break;
            }
          }
        }
      } else {
        liste.add(INFOTEKST);
      }
    }
    if (!blokerVidereNavigering) liste.add(ALLE_UDSENDELSER);
    adapter.notifyDataSetChanged();
  }

  // Kaldes af afspiller og hentning
  @Override
  public void run() {
    tjekOmHentet(udsendelse);
    opdaterSeekBar.run();
    adapter.notifyDataSetChanged(); // Opdater knapper etc
  }

  Runnable opdaterSeekBar = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(this);
      boolean denneUdsSpiller = udsendelse.equals(afspiller.getLydkilde()) && afspiller.getAfspillerstatus() == Status.SPILLER;
      if (!denneUdsSpiller) return;
      try {
        if (!seekBarBetjenesAktivt) { // Kun hvis vi ikke er i gang med at søge i udsendelsen
          int længdeMs = afspiller.getDuration();
          if (længdeMs > 0) {
            seekBar.setMax(længdeMs);
            seekBarMaxTekst.setText(DateUtils.formatElapsedTime(længdeMs / 1000));
          } else {
            seekBarMaxTekst.setText("");
          }
          seekBarMaxTekst.setText(længdeMs > 0 ? DateUtils.formatElapsedTime(afspiller.getDuration() / 1000) : "");
          int pos = afspiller.getCurrentPosition();
          Log.d("   pos " + pos + "   " + afspiller.getDuration());
          seekBarTekst_opdater(pos);
          seekBar.setProgress(pos);
        }
        App.forgrundstråd.postDelayed(this, 1000);
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    }
  };

  @Override
  public void onProgressChanged(SeekBar seekBarx, int progress, boolean fromUser) {
    if (fromUser) {
      DRData.instans.afspiller.seekTo(progress);
      seekBarTekst_opdater(progress);
      Log.registrérTestet("Søgning i udsendelse", "ja");
    }
  }

  private void seekBarTekst_opdater(int progress) {
    seekBarTekst.setText(DateUtils.formatElapsedTime(progress / 1000));
    int to = seekBar.getThumbOffset();
    int x = (seekBar.getWidth() - to * 2) * progress / seekBar.getMax();
    seekBarTekst.setPadding(x, 0, 0, 0);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    seekBarBetjenesAktivt = true;
    //seekBarTekst.setVisibility(View.VISIBLE);
    App.forgrundstråd.removeCallbacks(opdaterSeekBar);
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    seekBarBetjenesAktivt = false;
    //seekBarTekst.setVisibility(View.INVISIBLE);
    App.forgrundstråd.postDelayed(opdaterSeekBar, 1000);
  }

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public int getViewTypeCount() {
      return 7;
    }

    @Override
    public int getItemViewType(int position) {
      Object obj = liste.get(position);
      if (obj instanceof Integer) return (Integer) obj;
      // Så må det være et playlisteelement
      Playlisteelement pl = (Playlisteelement) obj;
      return pl.spillerNu ? PLAYLISTEELEM_NU : PLAYLISTEELEM;
    }

    @Override
    public boolean isEnabled(int position) {
      int type = getItemViewType(position);
      return type == PLAYLISTEELEM_NU || type == PLAYLISTEELEM || type == ALLE_UDSENDELSER;
    }

    @Override
    public boolean areAllItemsEnabled() {
      return false;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      Viewholder vh;
      AQuery aq;
      int type = getItemViewType(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(layoutFraType[type], parent, false);
        vh = new Viewholder();
        aq = vh.aq = new AQuery(v);
        v.setTag(vh);
        vh.startid = aq.id(R.id.startid).typeface(App.skrift_gibson).getTextView();
        if (type == TOP) {
          int br = bestemBilledebredde(listView, (View) aq.id(R.id.billede).getView().getParent(), 100);
          int hø = br * højde9 / bredde16;
          String burl = skalérSlugBilledeUrl(udsendelse.slug, br, hø);
          aq.width(br, false).height(hø, false).image(burl, true, true, br, 0, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          aq.id(R.id.lige_nu).gone();
          aq.id(R.id.info).typeface(App.skrift_gibson);
          Log.d("kanal JPER " + kanal.p4underkanal);
          if (kanal.p4underkanal) {
            Log.d("kanal JPER1 " + kanal.slug.substring(0, 2));
            aq.id(R.id.logo).image(R.drawable.kanalappendix_p4f);
            aq.id(R.id.p4navn).text(kanal.navn.replace("P4", "")).typeface(App.skrift_gibson_fed);
          } else {
            aq.id(R.id.logo).image(kanal.kanallogo_resid);
            aq.id(R.id.p4navn).text("");
          }

          aq.id(R.id.titel_og_tid).typeface(App.skrift_gibson)
              .text(lavFedSkriftTil(udsendelse.titel + " - " + (udsendelse.startTid==null?"(ukendt)":DRJson.datoformat.format(udsendelse.startTid)), udsendelse.titel.length()));

          //aq.id(R.id.beskrivelse).text(udsendelse.beskrivelse).typeface(App.skrift_georgia);
          //Linkify.addLinks(aq.getTextView(), Linkify.WEB_URLS);

          vh.titel = aq.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
          vh.titel.setText(udsendelse.titel.toUpperCase());
          aq.id(R.id.hør).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
          seekBarTekst = aq.id(R.id.seekBarTekst).typeface(App.skrift_gibson).getTextView();
          seekBarMaxTekst = aq.id(R.id.seekBarMaxTekst).typeface(App.skrift_gibson).getTextView();
          seekBar = aq.id(R.id.seekBar).getSeekBar();
          seekBar.setOnSeekBarChangeListener(Udsendelse_frag.this);
          aq.id(R.id.hent).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
          aq.id(R.id.kan_endnu_ikke_hentes).typeface(App.skrift_gibson);
          if (!DRData.instans.hentedeUdsendelser.virker()) aq.gone(); // Understøttes ikke på Android 2.2
          aq.id(R.id.del).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == PLAYLISTE_OVERSKRIFT_PLAYLISTE_INFO) {
          aq.id(R.id.playliste).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
          aq.id(R.id.info).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == INFOTEKST) {
          String forkortInfoStr = udsendelse.beskrivelse;
          Spannable spannable = new SpannableString(forkortInfoStr);//null;
          if (udsendelse.beskrivelse.length() > 100) {
            forkortInfoStr = forkortInfoStr.substring(0, 100);
            forkortInfoStr += "...(læs mere)";

            spannable = new SpannableString(forkortInfoStr);
            spannable.setSpan(new ForegroundColorSpan(App.color.blå), forkortInfoStr.length() - "(læs mere)".length(), forkortInfoStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            forkortInfo = true;
          }
          aq.id(R.id.titel).clicked(Udsendelse_frag.this).text(spannable/*forkortInfoStr*/).typeface(App.skrift_georgia);
          Linkify.addLinks(aq.getTextView(), Linkify.WEB_URLS);
        } else if (type == PLAYLISTEELEM_NU || type == PLAYLISTEELEM) {
          vh.titel = aq.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson).getTextView();
        } else if (type == VIS_HELE_PLAYLISTEN_KNAP) {
          aq.id(R.id.vis_hele_playlisten).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == ALLE_UDSENDELSER) {
          aq.id(R.id.titel).typeface(App.skrift_gibson_fed);
        }
      } else {
        vh = (Viewholder) v.getTag();
        aq = vh.aq;
      }

      // Opdatér viewholderens data
      if (type == TOP) {
        //aq.id(R.id.højttalerikon).visibility(streams ? View.VISIBLE : View.GONE);
        boolean lydkildeErDenneUds = udsendelse.equals(afspiller.getLydkilde());
        boolean lydkildeErDenneKanal = kanal==afspiller.getLydkilde().getKanal();
        boolean erAktuelUdsendelsePåKanalen = aktuelUdsendelsePåKanalen();
        boolean spiller = afspiller.getAfspillerstatus()==Status.SPILLER;
        boolean forbinder = afspiller.getAfspillerstatus()==Status.FORBINDER;
        boolean erOnline = App.netværk.erOnline();
        seekBar.setVisibility( spiller && lydkildeErDenneUds ? View.VISIBLE : View.GONE);
        seekBarTekst.setVisibility(spiller && lydkildeErDenneUds ? View.VISIBLE : View.GONE);
        seekBarMaxTekst.setVisibility(spiller && lydkildeErDenneUds ? View.VISIBLE : View.GONE);
        if (spiller && lydkildeErDenneUds) opdaterSeekBar.run();

        aq.id(R.id.hør).visible().enabled(true);
        if (udsendelse.hentetStream != null)         // Hentede udsendelser
        {
          if (lydkildeErDenneUds && (spiller||forbinder)) aq.gone();
          else aq.text("HØR HENTET UDSENDELSE");
        }
        else                                        // On demand og direkte udsendelser
        {
          if (lydkildeErDenneUds && (spiller||forbinder)) aq.gone();
          else if (udsendelse.streamsKlar() && erOnline) aq.text("HØR UDSENDELSE");
          else if (udsendelse.streamsKlar() && !erOnline) aq.text("Internetforbindelse mangler").enabled(false);
          else if (erAktuelUdsendelsePåKanalen) {
            if (lydkildeErDenneKanal&&(spiller||forbinder)) aq.enabled(false).text("SPILLER "+ kanal.navn.toUpperCase() + " LIVE");
            else if (erOnline) aq.text("HØR "+ kanal.navn.toUpperCase() + " LIVE");
            else aq.enabled(false).text("Internetforbindelse mangler");
          }
          else aq.enabled(false);
        }


        aq.id(R.id.hent).visibility(
            DRData.instans.hentedeUdsendelser.virker() && udsendelse.hentetStream==null && udsendelse.streamsKlar() ? View.VISIBLE : View.GONE);
        aq.textColorId(udsendelse.streamsKlar() ? R.color.blå : R.color.grå40);
        Cursor c = DRData.instans.hentedeUdsendelser.getStatusCursor(udsendelse);
        if (c != null) {
          int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
          String statustekst = HentedeUdsendelser.getStatustekst(c);
          c.close();

          if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
            aq.id(R.id.hent).text(statustekst);
            App.forgrundstråd.removeCallbacks(Udsendelse_frag.this);
            App.forgrundstråd.postDelayed(Udsendelse_frag.this, 5000);
          }
          aq.textColorId(R.color.grå40);
        }

        aq.id(R.id.kan_endnu_ikke_hentes).visibility(
          DRData.instans.hentedeUdsendelser.virker() && !udsendelse.streamsKlar()  ? View.VISIBLE : View.GONE);
      } else if (type == PLAYLISTEELEM_NU || type == PLAYLISTEELEM) {
        Playlisteelement ple = (Playlisteelement) liste.get(position);
        vh.playlisteelement = ple;
        vh.titel.setText(lavFedSkriftTil(ple.titel + " | " + ple.kunstner, ple.titel.length()));
        vh.startid.setText(ple.startTidKl);
        if (type == PLAYLISTEELEM_NU) {
          ImageView im = aq.id(R.id.senest_spillet_kunstnerbillede).getImageView();
          aq.image(skalérDiscoBilledeUrl(ple.billedeUrl, im.getWidth(), im.getHeight()));
        } else {
          //v.setBackgroundResource(R.drawable.knap_hvid_bg);
          v.setBackgroundResource(R.drawable.elem_hvid_bg);
        }
        aq.id(R.id.hør).visibility(udsendelse.kanHøres && ple.offsetMs >= 0 ? View.VISIBLE : View.GONE);
      } else if (type == PLAYLISTE_OVERSKRIFT_PLAYLISTE_INFO) {
        aq.id(R.id.playliste).background(visInfo ? R.drawable.knap_graa40_bg : R.drawable.knap_sort_bg);
        aq.id(R.id.info).background(visInfo ? R.drawable.knap_sort_bg : R.drawable.knap_graa40_bg);
      }
      udvikling_checkDrSkrifter(v, this + " position " + position);
      return v;
    }
  };
  private boolean forkortInfo = false;


  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.del) {
      del();
    } else if (v.getId() == R.id.hør) {
      hør();
    } else if (v.getId() == R.id.hent) {
      hent();
    } else if (v.getId() == R.id.info) {
      visInfo = true;
      bygListe();
    } else if (v.getId() == R.id.titel) {
      TextView titel = (TextView) v;
      if (forkortInfo) {
        titel.setText(udsendelse.beskrivelse);
        forkortInfo = false;
      } else {
//    		String forkortInfoStr = udsendelse.beskrivelse;
//        	if (udsendelse.beskrivelse.length() > 100){
//        		forkortInfoStr = forkortInfoStr.substring(0, 100);
//        		forkortInfoStr += "...(læs mere)";
//        		forkortInfo = true;
//        	}

        String forkortInfoStr = udsendelse.beskrivelse;
        if (udsendelse.beskrivelse.length() > 100) {
          forkortInfoStr = forkortInfoStr.substring(0, 100);
          forkortInfoStr += "...(læs mere)";

          SpannableString spannable = new SpannableString(forkortInfoStr);
          spannable.setSpan(new ForegroundColorSpan(App.color.blå), forkortInfoStr.length() - "(læs mere)".length(), forkortInfoStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          forkortInfo = true;
        }
        titel.setText(forkortInfoStr);

      }
      bygListe();
    } else if (v.getId() == R.id.playliste) {
      visInfo = false;
      bygListe();
    } else if (v.getId() == R.id.vis_hele_playlisten) {
      visHelePlaylisten = true;
      bygListe();
    } else {
      App.langToast("fejl");
    }
  }


  private void del() {

    Log.d("Udsendelse_frag " + "Del med nogen");
    try {
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.putExtra(Intent.EXTRA_SUBJECT, udsendelse.titel);
      intent.putExtra(Intent.EXTRA_TEXT, udsendelse.titel + "\n\n"
          + udsendelse.beskrivelse + "\n\n" +
// http://www.dr.dk/radio/ondemand/p6beat/debut-65
// http://www.dr.dk/radio/ondemand/ramasjangradio/ramasjang-formiddag-44#!/00:03
              // "http://dr.dk/radio/ondemand/" + kanal.slug + "/" + udsendelse.slug
                      udsendelse.ShareLink
//          + "\n\n" + udsendelse.findBedsteStream(true).url
      );
//www.dr.dk/p1/mennesker-og-medier/mennesker-og-medier-100
      startActivity(intent);
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }

  private void hent() {
    Cursor c = DRData.instans.hentedeUdsendelser.getStatusCursor(udsendelse);
    if (c != null) {
      c.close();
      // Skift til Hentede_frag
      try {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        // Fjern IKKE backstak - vi skal kunne hoppe tilbage hertil
        //fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.indhold_frag, new Hentede_udsendelser_frag());
        ft.addToBackStack(null);
        ft.commit();
      } catch (Exception e1) {
        Log.rapporterFejl(e1);
      }

      return;
    }
    if (!udsendelse.streamsKlar()) return;
    DRData.instans.hentedeUdsendelser.hent(udsendelse);
  }

  private void hør() {
    try {
      if (!udsendelse.streamsKlar()) {
        if (aktuelUdsendelsePåKanalen()) {
          // Så skal man lytte til livestreamet
          Kanal_frag.hør(kanal, getActivity());
          Log.registrérTestet("Åbne aktuel udsendelse og høre den", kanal.kode);
        }
        return;
      }
      //if (App.fejlsøgning) App.kortToast("kanal.streams=" + kanal.streams);
      Log.registrérTestet("Afspilning af gammel udsendelse", udsendelse.slug);
      if (App.prefs.getBoolean("manuelStreamvalg", false)) {
        udsendelse.nulstilForetrukkenStream();
        final List<Lydstream> lydstreamList = udsendelse.findBedsteStreams(false);
        new AlertDialog.Builder(getActivity())
            .setAdapter(new ArrayAdapter(getActivity(), R.layout.skrald_vaelg_streamtype, lydstreamList), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                lydstreamList.get(which).foretrukken = true;
                DRData.instans.afspiller.setLydkilde(udsendelse);
                DRData.instans.afspiller.startAfspilning();
              }
            }).show();
      } else {
        DRData.instans.afspiller.setLydkilde(udsendelse);
        DRData.instans.afspiller.startAfspilning();
      }
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }


  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    if (position == 0) return;
    //startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtras(getArguments())  // Kanalkode + slug
    //    .putExtra(VisFragment_akt.KLASSE, Programserie_frag.class.getName()).putExtra(DRJson.SeriesSlug.name(), udsendelse.programserieSlug));

    int type = adapter.getItemViewType(position);

    if (type == PLAYLISTEELEM || type == PLAYLISTEELEM_NU) {
      if (!udsendelse.streamsKlar()) return;
      // Det må være et playlisteelement
      final Playlisteelement pl = (Playlisteelement) liste.get(position);
      if (udsendelse.equals(afspiller.getLydkilde()) && afspiller.getAfspillerstatus() == Status.SPILLER) {
        afspiller.seekTo(pl.offsetMs);
      } else {
        afspiller.setLydkilde(udsendelse);
        afspiller.startAfspilning();
        afspiller.observatører.add(new Runnable() {
          @Override
          public void run() {
            if (afspiller.getLydkilde() == udsendelse) {
              if (afspiller.getAfspillerstatus() != Status.SPILLER) return;
              afspiller.seekTo(pl.offsetMs);
              seekBar.setProgress(pl.offsetMs);
              seekBarTekst_opdater(pl.offsetMs);
            }
            afspiller.observatører.remove(this); // afregistrér
          }
        });
      }
      seekBar.setProgress(pl.offsetMs);
      Log.registrérTestet("Valg af playlisteelement", "ja");
    } else if (type == ALLE_UDSENDELSER) {

      Fragment f = new Programserie_frag();
      f.setArguments(new Intent()
          .putExtra(P_kode, kanal.kode)
          .putExtra(DRJson.Slug.name(), udsendelse.slug)
          .putExtra(DRJson.SeriesSlug.name(), udsendelse.programserieSlug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();
    }
  }
}

