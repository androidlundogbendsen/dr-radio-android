package dk.dr.radio.akt;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.HentedeUdsendelser;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Hentede_udsendelser_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable, View.OnClickListener {
  private ListView listView;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  protected View rod;
  HentedeUdsendelser hentedeUdsendelser = DRData.instans.hentedeUdsendelser;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
      listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).text
              (Html.fromHtml("<b>Du har ingen downloads</b><br><br>Du kan downloade udsendelser og lytte til dem her uden internetforbindelse.")


    ).getView());
    listView.setCacheColorHint(Color.WHITE);

    aq.id(R.id.overskrift).typeface(App.skrift_gibson_fed).text("DOWNLOADEDE UDSENDELSER").getTextView();


    hentedeUdsendelser.observatører.add(this);
    run();
    udvikling_checkDrSkrifter(rod, this + " rod");
    return rod;
  }

  @Override
  public void onDestroyView() {
    hentedeUdsendelser.observatører.remove(this);
    super.onDestroyView();
  }


  @Override
  public void run() {
    liste.clear();
    liste.addAll(hentedeUdsendelser.getUdsendelser());
    Collections.reverse(liste);
    adapter.notifyDataSetChanged();
  }


  private static View.OnTouchListener farvKnapNårDenErTrykketNed = new View.OnTouchListener() {
    public boolean onTouch(View view, MotionEvent me) {
      ImageView ib = (ImageView) view;
      if (me.getAction() == MotionEvent.ACTION_DOWN) {
        ib.setColorFilter(App.color.blå, PorterDuff.Mode.MULTIPLY);
      } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
      } else {
        ib.setColorFilter(null);
      }
      return false;
    }
  };

  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {

      Udsendelse udsendelse = liste.get(position);
      AQuery aq;
      if (v == null) {
        v = getLayoutInflater(null).inflate(R.layout.udsendelse_elem3_tid_titel_kunstner, parent, false);
        v.setBackgroundResource(0);
        aq = new AQuery(v);
        aq.id(R.id.hør).image(R.drawable.trash).clicked(Hentede_udsendelser_frag.this)
            .getView().setOnTouchListener(farvKnapNårDenErTrykketNed);
        aq.id(R.id.startid).typeface(App.skrift_gibson_fed);
        aq.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson);
      } else {
        aq = new AQuery(v);
      }
      if (udsendelse == null) {
        udsendelse = new Udsendelse("Indlæser...");
        // TODO baggrundsindlæsning
        aq.id(R.id.startid).text("");
      } else {
        Cursor c = hentedeUdsendelser.getStatusCursor(udsendelse);
        if (c == null) {
          aq.id(R.id.titel_og_kunstner).text("Ikke tilgængelig");
        } else {
          int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
          String statustekst = HentedeUdsendelser.getStatustekst(c);
          c.close();

          aq.id(R.id.titel_og_kunstner).text(DRJson.datoformat.format(udsendelse.startTid) + " - " + statustekst);

          if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
            App.forgrundstråd.removeCallbacks(Hentede_udsendelser_frag.this);
            App.forgrundstråd.postDelayed(Hentede_udsendelser_frag.this, 1000);
          }
        }
      }
      aq.id(R.id.startid).text(udsendelse.titel)
          .textColor(udsendelse.kanHøres ? Color.BLACK : App.color.grå60);
      // Skjul stiplet linje over øverste listeelement
      aq.id(R.id.stiplet_linje).background(position == 0 ? R.drawable.linje : R.drawable.stiplet_linje);

      udvikling_checkDrSkrifter(v, this.getClass() + " ");

      aq.id(R.id.hør).tag(udsendelse);

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Udsendelse udsendelse = liste.get(position);
    if (udsendelse == null) return;
    // Tjek om udsendelsen er i RAM, og put den ind hvis den ikke er
    if (!DRData.instans.udsendelseFraSlug.containsKey(udsendelse.slug)) {
      DRData.instans.udsendelseFraSlug.put(udsendelse.slug, udsendelse);
    }
    Fragment f = new Udsendelse_frag();
    f.setArguments(new Intent()
//        .putExtra(Udsendelse_frag.BLOKER_VIDERE_NAVIGERING, true)
//        .putExtra(P_kode, getKanal.kode)
        .putExtra(DRJson.Slug.name(), udsendelse.slug)
        .getExtras());
    getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();


  }

  @Override
  public void onClick(View v) {
    try {
      Udsendelse u = (Udsendelse) v.getTag();
      hentedeUdsendelser.annullér(u);
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }
}

