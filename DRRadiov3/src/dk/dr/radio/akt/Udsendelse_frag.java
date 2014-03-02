package dk.dr.radio.akt;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
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
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Udsendelse_frag extends Basisfragment implements View.OnClickListener, AdapterView.OnItemClickListener {

  private ListView listView;
  private Kanal kanal;
  protected View rod;
  private Udsendelse udsendelse;
  private ArrayList<Playlisteelement> liste = new ArrayList<Playlisteelement>();
  private boolean streams;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal + "/" + udsendelse;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    kanal = DRData.instans.stamdata.kanalFraKode.get(getArguments().getString(Kanal_frag.P_kode));
    udsendelse = DRData.instans.udsendelseFraSlug.get(getArguments().getString(DRJson.Slug.name()));
    Log.d("onCreateView " + this);

    rod = inflater.inflate(R.layout.kanal_frag, container, false);
    if (kanal == null || udsendelse == null) {
      afbrydManglerData();
      return rod;
    }
    final AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_fed).getView());
    listView.setOnItemClickListener(this);

    if (udsendelse.playliste != null) {
      liste = udsendelse.playliste;
    } else {
      String url = kanal.getPlaylisteUrl(udsendelse); // http://www.dr.dk/tjenester/mu-apps/playlist/monte-carlo-352/p3
      Log.d("Henter playliste " + url);
      App.sætErIGang(true);
      aq.ajax(url, String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
        @Override
        public void callback(String url, String json, AjaxStatus status) {
          App.sætErIGang(false);
          Log.d("XXX url " + url + "   status=" + status.getCode());
          if (json != null && !"null".equals(json)) try {
            udsendelse.playliste = DRJson.parsePlayliste(new JSONArray(json));
            adapter.notifyDataSetChanged();
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
            e.printStackTrace();
          }
        }
      });
    }
    streams = udsendelse.streams != null && udsendelse.streams.size() > 0;
    if (streams && DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
      DRData.instans.afspiller.setLydkilde(udsendelse);
    }

    if (udsendelse.streams == null) {
      App.sætErIGang(true);
      aq.ajax(udsendelse.getStreamsUrl(), String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
        @Override
        public void callback(String url, String json, AjaxStatus status) {
          App.sætErIGang(false);
          Log.d("XXX udsendelse.getStreamsUrl()= " + url + "   status=" + status.getCode());
          if (json != null && !"null".equals(json)) try {
            JSONObject o = new JSONObject(json);
            udsendelse.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
            streams = udsendelse.streams != null && udsendelse.streams.size() > 0;
            if (streams && DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
              DRData.instans.afspiller.setLydkilde(udsendelse);
            }
            adapter.notifyDataSetChanged();
            ActivityCompat.invalidateOptionsMenu(getActivity());
          } catch (Exception e) {
            Log.d("Parsefejl: " + e + " for json=" + json);
            e.printStackTrace();
          }
        }
      });
    }
    udvikling_checkDrSkrifter(rod, this + " rod");
    setHasOptionsMenu(true);
    return rod;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.udsendelse, menu);
    menu.findItem(R.id.hør).setVisible(streams);
    menu.findItem(R.id.hent).setVisible(App.hentning != null && streams);
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

  @Override
  public void onResume() {
    getActivity().setTitle(udsendelse.slug);
    super.onResume();
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
  static final int SPILLER_NU = 1;
  static final int SPILLEDE = 2;
  static final int ALLE_UDS = 3;

  static final int[] layoutFraType = {
      R.layout.udsendelse_elem0_top,
      R.layout.udsendelse_elem1_spiller_nu,
      R.layout.udsendelse_elem2_tid_titel_kunstner,
      R.layout.udsendelse_elem3_alle_udsendelser};

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size() + 2;
    }

    @Override
    public int getViewTypeCount() {
      return 4;
    }

    @Override
    public int getItemViewType(int position) {
      if (position == 0) return TOP;
      if (position > liste.size()) return ALLE_UDS;
      if (position == 1) return SPILLER_NU;
      return SPILLEDE;
    }

    @Override
    public boolean isEnabled(int position) {
      return getItemViewType(position) == ALLE_UDS;
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
        vh.startid = aq.id(R.id.startid).typeface(App.skrift_normal).getTextView();
        vh.titel = aq.id(R.id.titel).typeface(App.skrift_fed).getTextView();
        if (type == TOP) {
          int br = bestemBilledebredde(listView, (View) aq.id(R.id.billede).getView().getParent());
          int hø = br * højde9 / bredde16;
          String burl = skalérSlugBilledeUrl(udsendelse.slug, br, hø);
          aq.width(br, false).height(hø, false).image(burl, true, true, br, 0, null, AQuery.FADE_IN, (float) højde9 / bredde16);

          aq.id(R.id.lige_nu).gone();
          aq.id(R.id.playliste).typeface(App.skrift_normal).visibility(udsendelse.streams != null && udsendelse.streams.size() > 0 ? View.VISIBLE : View.INVISIBLE);
          aq.id(R.id.info).typeface(App.skrift_normal);
          vh.titel.setText(udsendelse.titel.toUpperCase());
          aq.id(R.id.logo).image(kanal.kanallogo_resid);
          aq.id(R.id.titel2).typeface(App.skrift_fed).text(udsendelse.titel);
          aq.id(R.id.dato).typeface(App.skrift_normal).text(" - " + DRJson.datoformat.format(udsendelse.startTid));

          aq.id(R.id.beskrivelse).text(udsendelse.beskrivelse).typeface(App.skrift_normal);
          Linkify.addLinks(aq.getTextView(), Linkify.ALL);

          aq.id(R.id.hør).clicked(Udsendelse_frag.this).typeface(App.skrift_normal);
          aq.id(R.id.hent).clicked(Udsendelse_frag.this).typeface(App.skrift_normal);
          if (App.hentning == null) aq.gone(); // Understøttes ikke på Android 2.2
          aq.id(R.id.del).clicked(Udsendelse_frag.this).typeface(App.skrift_normal);
        } else if (type != ALLE_UDS) {
          vh.titel = aq.id(R.id.titel_og_kunstner).typeface(App.skrift_normal).getTextView();
        }
        aq.id(R.id.højttalerikon).visible().clicked(new UdsendelseClickListener(vh));
      } else {
        vh = (Viewholder) v.getTag();
        aq = vh.aq;
      }

      // Opdatér viewholderens data
      if (type == TOP) {
        boolean streams = udsendelse.streams != null && udsendelse.streams.size() > 0;
        aq.id(R.id.højttalerikon).visibility(streams ? View.VISIBLE : View.GONE);
        aq.id(R.id.hør).visibility(streams ? View.VISIBLE : View.GONE);
        aq.id(R.id.hent).visibility(streams && App.hentning != null ? View.VISIBLE : View.GONE);
        aq.id(R.id.kan_endnu_ikke_hentes).visibility(!streams ? View.VISIBLE : View.GONE);
      } else if (type != ALLE_UDS) {
        Playlisteelement u = liste.get(position - 1);
        vh.playlisteelement = u;
        vh.titel.setText(Html.fromHtml("<b>" + u.titel + "</b> &nbsp; | &nbsp;" + u.kunstner));
        vh.startid.setText(u.startTidKl);
        if (type == SPILLER_NU) {
          ImageView im = aq.id(R.id.senest_spillet_kunstnerbillede).getImageView();
          aq.image(skalérDiscoBilledeUrl(u.billedeUrl, im.getWidth(), im.getHeight()));
        } else {
          //v.setBackgroundResource(R.drawable.knap_hvid_bg);
          v.setBackgroundResource(R.color.hvid);
        }
      }
      udvikling_checkDrSkrifter(v, this + " position " + position);
      return v;
    }
  };


  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.del) {
      del();
    } else if (v.getId() == R.id.hør) {
      hør();
    } else if (v.getId() == R.id.hent) {
      hent();
    } else {
      App.langToast("fejl");
    }
  }

  private void del() {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/plain");
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    intent.putExtra(Intent.EXTRA_SUBJECT, udsendelse.titel);
    intent.putExtra(Intent.EXTRA_TEXT, udsendelse.titel + "\n\n"
        + udsendelse.beskrivelse + "\n\n" +
        "http://dr.dk/" + kanal.slug + "/" + udsendelse.programserieSlug + "/" + udsendelse.slug + "\n\n" +
        kanal.findBedsteStream().url
    );
//www.dr.dk/p1/mennesker-og-medier/mennesker-og-medier-100
    startActivity(intent);
  }

  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
  private void hent() {
    if (udsendelse.streams == null || udsendelse.streams.size() == 0) return;
    Uri uri = Uri.parse(udsendelse.findBedsteStream().url);
    Log.d("uri=" + uri);

    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
    dir.mkdirs();
    DownloadManager.Request req = new DownloadManager.Request(uri);

    req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
        .setAllowedOverRoaming(false)
        .setTitle(udsendelse.titel)
        .setDescription(udsendelse.beskrivelse)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PODCASTS, udsendelse.slug + ".mp3");
    if (Build.VERSION.SDK_INT >= 11) req.allowScanningByMediaScanner();


    long downloadId = App.hentning.downloadService.enqueue(req);
    App.langToast("downloadId=" + downloadId + "\n" + dir);
  }

  private void hør() {
    if (udsendelse.streams == null || udsendelse.streams.size() == 0) return;
    if (App.udvikling) App.kortToast("kanal.streams=" + kanal.streams);
    if (App.prefs.getBoolean("manuelStreamvalg", false)) {
      new AlertDialog.Builder(getActivity())
          .setAdapter(new ArrayAdapter(getActivity(), R.layout.skrald_vaelg_streamtype, kanal.streams), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              udsendelse.streams.get(which).foretrukken = true;
              DRData.instans.aktuelKanal = kanal;
              DRData.instans.afspiller.setLydkilde(kanal);
              DRData.instans.afspiller.startAfspilning();
            }
          }).show();
    } else {
      DRData.instans.aktuelKanal = kanal;
      DRData.instans.afspiller.setLydkilde(udsendelse);
      DRData.instans.afspiller.startAfspilning();
    }
  }


  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    if (position == 0) return;
    //startActivity(new Intent(getActivity(), VisFragment_akt.class).putExtras(getArguments())  // Kanalkode + slug
    //    .putExtra(VisFragment_akt.KLASSE, Programserie_frag.class.getName()).putExtra(DRJson.SeriesSlug.name(), udsendelse.programserieSlug));

    if (adapter.getItemViewType(position) == ALLE_UDS) {

      Fragment f = new Programserie_frag();
      f.setArguments(new Intent()
          .putExtra(P_kode, kanal.kode)
          .putExtra(DRJson.Slug.name(), udsendelse.slug)
          .putExtra(DRJson.SeriesSlug.name(), udsendelse.programserieSlug).getExtras());
      getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
    }
  }

  private class UdsendelseClickListener implements View.OnClickListener {

    private final Viewholder viewHolder;

    public UdsendelseClickListener(Viewholder vh) {
      viewHolder = vh;
    }

    @Override
    public void onClick(View v) {
      App.langToast("fejl2");
    }
  }
}

