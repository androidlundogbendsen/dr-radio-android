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
import com.android.volley.VolleyError;
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
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.DrVolleyResonseListener;
import dk.dr.radio.diverse.DrVolleyStringRequest;
import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.diverse.Log;
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
  private boolean aktuelUdsendelsePåKanalen;

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
    aktuelUdsendelsePåKanalen = udsendelse.slug.equals(getArguments().getString(AKTUEL_UDSENDELSE_SLUG));
    blokerVidereNavigering = getArguments().getBoolean(BLOKER_VIDERE_NAVIGERING);

    Log.d("onCreateView " + this);

    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).getView());
    listView.setOnItemClickListener(this);

    tjekOmHentet(udsendelse);
    if (udsendelse.hentetStream == null) {
      Request<?> req = new DrVolleyStringRequest(udsendelse.getStreamsUrl(), new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache) throws Exception {
          Log.d("fikSvar(" + fraCache + " " + url);
          if (json != null && !"null".equals(json)) try {
            JSONObject o = new JSONObject(json);
            udsendelse.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
            udsendelse.kanHøres = streamsErKlar();
            if (fragmentErSynligt && udsendelse.kanHøres && afspiller.getAfspillerstatus() == Status.STOPPET) {
              afspiller.setLydkilde(udsendelse);
            }
            adapter.notifyDataSetChanged(); // Opdatér views
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
            e.printStackTrace();
          }
        }

        @Override
        protected void fikFejl(VolleyError error) {
          Log.e("error.networkResponse=" + error.networkResponse, error);
          App.kortToast("Netværksfejl, prøv igen senere");
        }
      }).setTag(this);
      App.volleyRequestQueue.add(req);
    }

//    if (streamsErKlar() && DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
//      DRData.instans.afspiller.setLydkilde(udsendelse);
//    }

    opdaterSpillelisteRunnable.run();

    setHasOptionsMenu(true);
    bygListe();

    afspiller.observatører.add(this);
    DRData.instans.hentedeUdsendelser.observatører.add(this);
    udvikling_checkDrSkrifter(rod, this + " rod");
    return rod;
  }

  private void startOpdaterSpilleliste() {
    //new Exception("startOpdaterSpilleliste() for "+this).printStackTrace();
    Request<?> req = new DrVolleyStringRequest(kanal.getPlaylisteUrl(udsendelse), new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache) throws Exception {
        Log.d("fikSvar playliste(" + fraCache + " " + url + "   " + this);
        if (fraCache && udsendelse.playliste != null)
          return; // Vi har allerede en liste, det må være den fra cachen
        if (json != null && !"null".equals(json)) try {
          udsendelse.playliste = DRJson.parsePlayliste(new JSONArray(json));
          bygListe();
        } catch (Exception e) {
          Log.d("Parsefejl: " + e + " for json=" + json);
          e.printStackTrace();
        }
      }
    }).setTag(this);
    App.volleyRequestQueue.add(req);
  }

  Runnable opdaterSpillelisteRunnable = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(opdaterSpillelisteRunnable);
      startOpdaterSpilleliste();
      if (aktuelUdsendelsePåKanalen && isResumed()) {
        App.forgrundstråd.postDelayed(opdaterSpillelisteRunnable, 15000);
      }
    }
  };

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    Log.d(" QQQ setUserVisibleHint " + isVisibleToUser + "  " + this);
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
        } else {
          Log.e(new IllegalStateException("Fil fandtes ikke alligevel??!"));
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

  private boolean streamsErKlar() {
    return udsendelse.hentetStream != null || udsendelse.streams != null && udsendelse.streams.size() > 0;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.udsendelse, menu);
    //menu.findItem(R.id.hør).setVisible(udsendelse.kanHøres).setEnabled(streamsErKlar());
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
  static final int PLAYLISTE_KAPITLER_INFO_OVERSKRIFT = 1;
  static final int SPILLER_NU = 2;
  static final int SPILLEDE = 3;
  static final int INFO = 4;
  static final int VIS_HELE_PLAYLISTEN = 5;
  static final int ALLE_UDS = 6;

  static final int[] layoutFraType = {
      R.layout.udsendelse_elem0_top,
      R.layout.udsendelse_elem1_playliste_kapitler_info_overskrift,
      R.layout.udsendelse_elem2_spiller_nu,
      R.layout.udsendelse_elem3_tid_titel_kunstner,
      R.layout.udsendelse_elem4_info,
      R.layout.udsendelse_elem5_vis_hele_playlisten,
      R.layout.udsendelse_elem6_alle_udsendelser};

  boolean visInfo = false;
  boolean visHelePlaylisten = false;

  void bygListe() {
    liste.clear();
    liste.add(TOP);
    if (visInfo) {
      liste.add(PLAYLISTE_KAPITLER_INFO_OVERSKRIFT);
      liste.add(INFO);
    } else {
      if (udsendelse.playliste != null && udsendelse.playliste.size() > 0) {
        liste.add(PLAYLISTE_KAPITLER_INFO_OVERSKRIFT);
        if (visHelePlaylisten) {
          if (aktuelUdsendelsePåKanalen) udsendelse.playliste.get(0).spillerNu = true;
          liste.addAll(udsendelse.playliste);
        } else {
          for (int i = 0; i < udsendelse.playliste.size(); i++) {
            Playlisteelement e = udsendelse.playliste.get(i);
            e.spillerNu = (i == 0 && aktuelUdsendelsePåKanalen);
            liste.add(e);
            if (i >= 4) {
              liste.add(VIS_HELE_PLAYLISTEN);
              break;
            }
          }
        }
      } else {
        liste.add(INFO);
      }
    }
    if (!blokerVidereNavigering) liste.add(ALLE_UDS);
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
      boolean denneUdsSpiller = udsendelse == afspiller.getLydkilde() && afspiller.getAfspillerstatus() == Status.SPILLER;
//      Log.d("ÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆ "+denneUdsSpiller+"  "+seekBarBetjenesAktivt);
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
      return pl.spillerNu ? SPILLER_NU : SPILLEDE;
    }

    @Override
    public boolean isEnabled(int position) {
      int type = getItemViewType(position);
      return type == SPILLER_NU || type == SPILLEDE || type == ALLE_UDS;
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
          Log.d("kanal JPER " +kanal.p4underkanal);
          if(kanal.p4underkanal){
              Log.d("kanal JPER1 " + kanal.slug.substring(0,2));
              aq.id(R.id.logo).image(R.drawable.kanalappendix_p4f);
              aq.id(R.id.p4navn).text(kanal.navn.replace("P4", "")).typeface(App.skrift_gibson_fed);
          }
          else{
              aq.id(R.id.logo).image(kanal.kanallogo_resid);
              aq.id(R.id.p4navn).text("");
          }

          aq.id(R.id.titel_og_tid).typeface(App.skrift_gibson).text(lavFedSkriftTil(udsendelse.titel + " - " + DRJson.datoformat.format(udsendelse.startTid), udsendelse.titel.length()));

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
        } else if (type == INFO) {
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
        } else if (type == SPILLER_NU || type == SPILLEDE) {
          vh.titel = aq.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson).getTextView();
          Playlisteelement u = (Playlisteelement) liste.get(position);
          aq.id(R.id.hør).visibility(udsendelse.kanHøres && u.offsetMs >= 0 ? View.VISIBLE : View.GONE);
        } else if (type == VIS_HELE_PLAYLISTEN) {
          aq.id(R.id.vis_hele_playlisten).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
        } else if (type == PLAYLISTE_KAPITLER_INFO_OVERSKRIFT) {
          aq.id(R.id.playliste).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
          aq.id(R.id.info).clicked(Udsendelse_frag.this).typeface(App.skrift_gibson);
        }
        //aq.id(R.id.højttalerikon).visible().clicked(new UdsendelseClickListener(vh));
      } else {
        vh = (Viewholder) v.getTag();
        aq = vh.aq;
      }

      // Opdatér viewholderens data
      if (type == TOP) {
        //aq.id(R.id.højttalerikon).visibility(streams ? View.VISIBLE : View.GONE);
        boolean streamsKlar = streamsErKlar();
        boolean denneUdsSpiller = udsendelse == afspiller.getLydkilde() && afspiller.getAfspillerstatus() == Status.SPILLER;
        boolean denneUdsForbinder = udsendelse == afspiller.getLydkilde() && afspiller.getAfspillerstatus() == Status.FORBINDER;
        seekBar.setVisibility(denneUdsSpiller ? View.VISIBLE : View.GONE);
        seekBarTekst.setVisibility(denneUdsSpiller ? View.VISIBLE : View.GONE);
        seekBarMaxTekst.setVisibility(denneUdsSpiller ? View.VISIBLE : View.GONE);
        opdaterSeekBar.run();
        aq.id(R.id.hør).enabled(aktuelUdsendelsePåKanalen || streamsKlar && !denneUdsForbinder).visibility(aktuelUdsendelsePåKanalen || udsendelse.kanHøres && !denneUdsSpiller ? View.VISIBLE : View.GONE);
        if (udsendelse.hentetStream != null) aq.text("HØR HENTET UDSENDELSE");
        if (aktuelUdsendelsePåKanalen) {
          boolean spillerDenneKanal = DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET && DRData.instans.afspiller.getLydkilde() == kanal;
          boolean online = App.netværk.erOnline();
          vh.aq.id(R.id.hør_live).enabled(!spillerDenneKanal && online).text(
              !online ? "Internetforbindelse mangler" :
                  (spillerDenneKanal ? " SPILLER " : " HØR ") + kanal.navn.toUpperCase() + " LIVE");
        }
        aq.id(R.id.hent).visibility(udsendelse.hentetStream == null && udsendelse.kanHøres && DRData.instans.hentedeUdsendelser.virker() ? View.VISIBLE : View.GONE);
        aq.textColorId(streamsKlar ? R.color.blå : R.color.grå40).getButton();

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

        aq.id(R.id.kan_endnu_ikke_hentes).visibility(!udsendelse.kanHøres ? View.VISIBLE : View.GONE);
      } else if (type == SPILLER_NU || type == SPILLEDE) {
        Playlisteelement u = (Playlisteelement) liste.get(position);
        vh.playlisteelement = u;
        //vh.titel.setText(Html.fromHtml("<b>" + u.titel + "</b> &nbsp; | &nbsp;" + u.kunstner));
        vh.titel.setText(lavFedSkriftTil(u.titel + " | " + u.kunstner, u.titel.length()));
        vh.startid.setText(u.startTidKl);
        if (type == SPILLER_NU) {
          ImageView im = aq.id(R.id.senest_spillet_kunstnerbillede).getImageView();
          aq.image(skalérDiscoBilledeUrl(u.billedeUrl, im.getWidth(), im.getHeight()));
        } else {
          //v.setBackgroundResource(R.drawable.knap_hvid_bg);
          v.setBackgroundResource(R.drawable.elem_hvid_bg);
        }
      } else if (type == PLAYLISTE_KAPITLER_INFO_OVERSKRIFT) {
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
        Spannable spannable = new SpannableString(forkortInfoStr);//null;
        if (udsendelse.beskrivelse.length() > 100) {
          forkortInfoStr = forkortInfoStr.substring(0, 100);
          forkortInfoStr += "...(læs mere)";

          spannable = new SpannableString(forkortInfoStr);
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
          "http://dr.dk/radio/ondemand/" + kanal.slug + "/" + udsendelse.slug
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
        // Fjern backstak - så vi starter forfra i 'roden'
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.indhold_frag, new Hentede_udsendelser_frag());
        ft.commit();
      } catch (Exception e1) {
        Log.rapporterFejl(e1);
      }

      return;
    }
    if (!streamsErKlar()) return;
    DRData.instans.hentedeUdsendelser.hent(udsendelse);
  }

  private void hør() {
    try {
      if (aktuelUdsendelsePåKanalen) {
        // Så skal man lytte til livestreamet
        Kanal_frag.hør(kanal, getActivity());
        return;
      }
      if (!streamsErKlar()) return;
      if (App.fejlsøgning) App.kortToast("kanal.streams=" + kanal.streams);
      if (!App.EMULATOR) {
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("kanal", kanal.kode);
        param.put("udsendelse", udsendelse.slug);
      }
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

    if (type == SPILLEDE || type == SPILLER_NU) {
      if (!udsendelse.kanHøres) return;
      // Det må være et playlisteelement
      final Playlisteelement pl = (Playlisteelement) liste.get(position);
      if (afspiller.getLydkilde() == udsendelse && afspiller.getAfspillerstatus() == Status.SPILLER) {
        afspiller.seekTo(pl.offsetMs);
      } else {
        afspiller.setLydkilde(udsendelse);
        afspiller.startAfspilning();
        afspiller.observatører.add(new Runnable() {
          @Override
          public void run() {
            if (afspiller.getLydkilde() == udsendelse && afspiller.getAfspillerstatus() == Status.SPILLER) {
              afspiller.seekTo(pl.offsetMs);
              seekBar.setProgress(pl.offsetMs);
              seekBarTekst_opdater(pl.offsetMs);
              afspiller.observatører.remove(this);
            }
          }
        });
      }
      seekBar.setProgress(pl.offsetMs);
    } else if (type == ALLE_UDS) {

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

