package dk.dr.radio.akt;

import android.content.Intent;
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

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Favoritter;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Favoritprogrammer_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {

  private ListView listView;
  private ArrayList<Object> liste = new ArrayList<Object>(); // Indeholder både udsendelser og -serier
  protected View rod;
  Favoritter favoritter = DRData.instans.favoritter;
  private static long sidstOpdateretAntalNyeUdsendelser;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).text(
//        "Ingen favoritter\nGå ind på en programserie og tryk på hjertet for at gøre det til en favorit"
        "Du har endnu ikke tilføjet nogle favoritprogrammer.\n" +
            "Favoritprogrammer kan vælges ved at markere hjerte-ikonet ved de enkelte programserievisninger."
    ).getView());
    listView.setCacheColorHint(Color.WHITE);
    
    aq.id(R.id.overskrift).typeface(App.skrift_gibson_fed).text("Dine favoritprogrammer").getTextView();

    favoritter.observatører.add(this);
    run();
    if (favoritter.getAntalNyeUdsendelser()<0 || sidstOpdateretAntalNyeUdsendelser>System.currentTimeMillis()+1000*60*10) {
      // Opdatering af nye antal udsendelser er ikke sket endnu - eller det er mere end end ti minutter siden.
      DRData.instans.favoritter.startOpdaterAntalNyeUdsendelser.run();
      sidstOpdateretAntalNyeUdsendelser = System.currentTimeMillis();
    }
    udvikling_checkDrSkrifter(rod, this + " rod");
    return rod;
  }

  @Override
  public void onDestroyView() {
    favoritter.observatører.remove(this);
    super.onDestroyView();
  }


  @Override
  public void run() {
    liste.clear();
    if (favoritter.getAntalNyeUdsendelser()<0) {
      // Opdatering af nye antal udsendelser i favoritter i kø, til om 3 sekunder
      DRData.instans.favoritter.startOpdaterAntalNyeUdsendelser.run();
    }
    try {
      ArrayList<String> pss = new ArrayList<String>(favoritter.getProgramserieSlugSæt());
      Collections.sort(pss);
      Log.d(this + " psss = " + pss);
      for (String programserieSlug : pss) {
        Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
        if (programserie!=null) liste.add(programserie);
        else Log.d("programserieSlug gav ingen værdi: "+programserieSlug);
/* De enkelte prgramudsendelser er fjernet fra favoritlisten

        int antalNye = favoritter.getAntalNyeUdsendelser(programserieSlug);
        for (int n = 0; n<antalNye && n<programserie.udsendelser.size(); n++) {
          liste.add(programserie.udsendelser.get(n));
        }
*/
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
      try {
        if (v == null) v = getLayoutInflater(null).inflate(R.layout.elem_tid_titel_kunstner, parent, false);
        AQuery aq = new AQuery(v);

        Object obj = liste.get(position);
        if (obj instanceof Programserie) {
          Programserie ps = (Programserie) obj;
          aq.id(R.id.startid).text(ps.titel).typeface(App.skrift_gibson_fed).textColor(Color.BLACK);
            int n = favoritter.getAntalNyeUdsendelser(ps.slug);
            String txt = (n == 1 ? n + " ny udsendelse" : n + " nye udsendelser");
            aq.id(R.id.titel_og_kunstner).text(txt).typeface(App.skrift_gibson);
          aq.id(R.id.stiplet_linje).background(position==0?R.drawable.linje:R.drawable.stiplet_linje);
        } else {
          Udsendelse udsendelse = (Udsendelse) obj;
          aq.id(R.id.startid).text(DRJson.datoformat.format(udsendelse.startTid)).typeface(App.skrift_gibson);
          aq.id(R.id.titel_og_kunstner).text(udsendelse.titel).typeface(App.skrift_gibson);
          aq.id(R.id.stiplet_linje).background(R.drawable.stiplet_linje);
        }
        v.setBackgroundResource(0);


        udvikling_checkDrSkrifter(v, this.getClass() + " ");
      } catch (Exception e) { Log.rapporterFejl(e); }

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Object obj = liste.get(position);
    if (obj instanceof Programserie) {
      Programserie programserie = (Programserie) obj;
      Fragment f = new Programserie_frag();
      f.setArguments(new Intent()
          .putExtra(DRJson.SeriesSlug.name(), programserie.slug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();

    } else {
      Udsendelse udsendelse = (Udsendelse) obj;
      Fragment f = new Udsendelse_frag();
      f.setArguments(new Intent()
//        .putExtra(Udsendelse_frag.BLOKER_VIDERE_NAVIGERING, true)
//        .putExtra(P_kode, titel.kode)
          .putExtra(DRJson.Slug.name(), udsendelse.slug)
          .getExtras());
      getActivity().getSupportFragmentManager().beginTransaction()
          .replace(R.id.indhold_frag, f)
          .addToBackStack(null)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .commit();

    }

  }

}

