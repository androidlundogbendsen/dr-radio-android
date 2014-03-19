package dk.dr.radio.akt;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Spannable;
import android.text.SpannableString;
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
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Senest_lyttede_frag extends Basisfragment implements AdapterView.OnItemClickListener, Runnable {

  private ListView listView;
  private ArrayList<Lydkilde> liste = new ArrayList<Lydkilde>();
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
	    public TextView metainformation;
	    public TextView titel;
	    public Lydkilde lydkilde;
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
        Lydkilde lydkilde = liste.get(position);
        
      if (v == null) {
        v = getLayoutInflater(null).inflate(R.layout.elem_tid_titel_kunstner, parent, false);
        v.setBackgroundResource(0);
        
        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.metainformation = a.id(R.id.startid).typeface(App.skrift_gibson_fed).textColor(Color.BLACK).getTextView();
        vh.titel = a.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson).getTextView();

        
        v.setTag(vh);
        
      }else {
          vh = (Viewholder) v.getTag();
          a = vh.aq;
        }
      
      vh.lydkilde = lydkilde;
      // Skjul stiplet linje over øverste listeelement
      vh.aq.id(R.id.stiplet_linje).background(position==0?R.drawable.linje:R.drawable.stiplet_linje);

      
//      TextView titel = (TextView) v.findViewById(R.id.startid);
//      TextView titel = (TextView) v.findViewById(R.id.titel_og_kunstner);

      Lydkilde k = liste.get(position);
      //titel.setText(k.titel().navn);
      //Spannable spannable = new SpannableString(k.kanal().navn);
      //spannable.setSpan(App.skrift_gibson_fed_span, 0, k.kanal().navn.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      vh.titel.setText(k.kanal().navn);

      String titelStr = "";// u.titel;
      if (k instanceof Kanal) {
        titelStr = ((Kanal) k).navn + " (Direkte)";
      } else {
        titelStr = ((Udsendelse) k).titel;
        //titel.setText(u.titel + " (" + DRJson.datoformat.format(u.startTid) + ")");
      }
      
      //spannable = new SpannableString(titelStr);
      //spannable.setSpan(App.skrift_gibson, 0, titelStr.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	    
      vh.metainformation.setText(titelStr);
      
      //vh.titel.setText(lydkilde.titel);
      //a.id(R.id.stiplet_linje).visibility(position == aktuelUdsendelseIndex + 1 ? View.INVISIBLE : View.VISIBLE);
      //a.id(R.id.hør).visibility(lydkilde.kanHøres ? View.VISIBLE : View.GONE);
      
      udvikling_checkDrSkrifter(v, this.getClass() + " ");

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {

    Fragment f = new Udsendelse_frag();
    Lydkilde k = liste.get(position);
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

