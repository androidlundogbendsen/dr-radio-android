package dk.dr.radio.akt;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Hentning;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Hentede_udsendelser_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {
  private ListView listView;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  protected View rod;
  Hentning hentede = App.hentning;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).text(
        "Du har endnu ikke hentet nogen udsendelser."
    ).getView());
    listView.setCacheColorHint(Color.WHITE);
    
    aq.id(R.id.overskrift).typeface(App.skrift_gibson).text("Downloadede udsendelser").getTextView();


    hentede.observatører.add(this);
    run();
    udvikling_checkDrSkrifter(rod, this + " rod");
    return rod;
  }

  @Override
  public void onDestroyView() {
    hentede.observatører.remove(this);
    super.onDestroyView();
  }


  @Override
  public void run() {
    liste.clear();
    try {
      ArrayList<String> slugListe = new ArrayList<String>(hentede.getUdsendelseSlugSæt());
      Collections.sort(slugListe);
      Log.d(this + " psss = " + slugListe);
      for (String slug : slugListe) {
        Udsendelse u = DRData.instans.udsendelseFraSlug.get(slug);
        liste.add(u);
      }
      Log.d(this + " liste = " + liste);
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    adapter.notifyDataSetChanged();
  }


  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {

      Udsendelse udsendelse = liste.get(position);
      if (v == null) v = getLayoutInflater(null).inflate(R.layout.elem_tid_titel_kunstner, parent, false);
      v.setBackgroundResource(0);
      AQuery aq = new AQuery(v);
      aq.id(R.id.startid).typeface(App.skrift_gibson);
      aq.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson);
      if (udsendelse==null) {
        udsendelse = new Udsendelse("Indlæser...");
        // TODO baggrundsindlæsning
        aq.id(R.id.startid).text("");
      } else {
        String txt = "";
        Cursor c = hentede.getStatus(udsendelse);
        if (c==null) {
          aq.id(R.id.startid).text("Ikke tilgængelig");
        } else {
          int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
          if (status==DownloadManager.STATUS_SUCCESSFUL) {
            txt = " - Klar";
          } else if (status==DownloadManager.STATUS_FAILED) {
            txt = " - Hentning mislykkedes";
          } else {
            txt = " - I gang...";
          }
          Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
          Log.d(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
          Log.d(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
          Log.d(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
          Log.d(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
          c.close();

          aq.id(R.id.startid).text(DRJson.datoformat.format(udsendelse.startTid) + txt);
        }
      }
      aq.id(R.id.titel_og_kunstner).text(udsendelse.titel)
          .textColor(udsendelse.kanHøres ? Color.BLACK : App.color.grå60);
      // Skjul stiplet linje over øverste listeelement
      aq.id(R.id.stiplet_linje).visibility(position==0?View.INVISIBLE:View.VISIBLE);

      udvikling_checkDrSkrifter(v, this.getClass() + " ");

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Udsendelse udsendelse = liste.get(position);
    if (udsendelse==null) return;
    Fragment f = new Udsendelse_frag();
    f.setArguments(new Intent()
//        .putExtra(Udsendelse_frag.BLOKER_VIDERE_NAVIGERING, true)
//        .putExtra(P_kode, kanal.kode)
        .putExtra(DRJson.Slug.name(), udsendelse.slug)
        .getExtras());
    getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();


  }

}

