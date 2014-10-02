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
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Collections;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.SenestLyttede;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Senest_lyttede_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {

  private ListView listView;
  private ArrayList<SenestLyttede.SenestLyttet> liste = new ArrayList<SenestLyttede.SenestLyttet>();
  protected View rod;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.senest_lyttede, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson_fed).text("Ingen senest lyttede").getView());
    opdaterListe();

    TextView overskrift = aq.id(R.id.overskrift).typeface(App.skrift_gibson_fed).text("Senest lyttede").getTextView();
    overskrift.setVisibility(View.VISIBLE);

    udvikling_checkDrSkrifter(rod, this + " rod");
    DRData.instans.afspiller.observatører.add(this);
    App.netværk.observatører.add(this);
    opdaterListe();
    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    DRData.instans.afspiller.observatører.remove(this);
    App.netværk.observatører.remove(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    opdaterListe();
  }

  @Override
  public void run() {
    opdaterListe();
  }


  private void opdaterListe() {
    try {
      liste.clear();
      liste.addAll(DRData.instans.senestLyttede.getListe());
      Collections.reverse(liste);
    } catch (Exception e1) {
      Log.rapporterFejl(e1);
    }
    adapter.notifyDataSetChanged();
  }

  private static class Viewholder {
    public AQuery aq;
    public TextView linje1;
    public TextView linje2;
  }


  private BaseAdapter adapter = new Basisadapter() {
    @Override
    public int getCount() {
      return liste.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {

      Viewholder vh;
      AQuery a;
      SenestLyttede.SenestLyttet sl = liste.get(position);

      if (v == null) {
        v = getLayoutInflater(null).inflate(R.layout.listeelem_2linjer, parent, false);
        v.setBackgroundResource(0);

        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.linje1 = a.id(R.id.linje1).typeface(App.skrift_gibson_fed).textColor(Color.BLACK).getTextView();
        vh.linje2 = a.id(R.id.linje2).typeface(App.skrift_gibson).getTextView();


        v.setTag(vh);

      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
      }

      // Skjul stiplet linje over øverste listeelement
      vh.aq.id(R.id.stiplet_linje).background(position == 0 ? R.drawable.linje : R.drawable.stiplet_linje);



      if (sl.lydkilde instanceof Kanal) {
        Kanal k = (Kanal) sl.lydkilde;
        vh.linje1.setText(k.navn + " (Direkte)");
        vh.linje2.setText(k.navn);
      } else if (sl.lydkilde instanceof Udsendelse) {
        Udsendelse u = (Udsendelse) sl.lydkilde;
        vh.linje1.setText(u.titel);
        Kanal k = u.getKanal();
        vh.linje2.setText((k==null?"":k.navn+" - ")+DRJson.getDagsbeskrivelse(u.startTid).toLowerCase()+" kl "+u.startTidKl);
      } else {
        Log.rapporterFejl(new Exception("forkert type"), sl.lydkilde);
      }

      udvikling_checkDrSkrifter(v, this.getClass() + " ");

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {

    Fragment f;
    Lydkilde k = liste.get(position).lydkilde;
    if (k instanceof Kanal) {
      f = new Kanal_frag();
      f.setArguments(new Intent()
          .putExtra(P_kode, ((Kanal) k).kode)
          .getExtras());
    } else {
      if (!DRData.instans.udsendelseFraSlug.containsKey(k.slug)) {
        DRData.instans.udsendelseFraSlug.put(k.slug, (Udsendelse) k);
      }
      f = new Udsendelse_frag();
      f.setArguments(new Intent()
          .putExtra(DRJson.Slug.name(), k.slug)
          .getExtras());
    }
    getActivity().getSupportFragmentManager().beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
  }
}

