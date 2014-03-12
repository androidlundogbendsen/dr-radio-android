package dk.dr.radio.akt;

import android.content.Intent;
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
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONArray;

import java.util.ArrayList;

import dk.dr.radio.akt.diverse.Basisadapter;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Soeg_efter_program_frag extends Basisfragment implements AdapterView.OnItemClickListener {

  private ListView listView;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  protected View rod;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.kanal_frag, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this).getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson).text("Søg efter program").getView());

    udvikling_checkDrSkrifter(rod, this + " rod");
/*
Kald
http://www.dr.dk/tjenester/mu-apps/search/programs?q=monte&type=radio vil kun returnere radio programmer
http://www.dr.dk/tjenester/mu-apps/search/series?q=monte&type=radio vil kun returnere radio serier
 */
    String url = "http://www.dr.dk/tjenester/mu-apps/search/programs?q=monte&type=radio";
    new AQuery(App.instans).ajax(url, String.class, 1 * 60 * 60 * 1000, new AjaxCallback<String>() {
      @Override
      public void callback(String url, String json, AjaxStatus status) {
        App.sætErIGang(false);
        Log.d("XXX url " + url + "   status=" + status.getCode());
        if (json != null && !"null".equals(json)) try {
          JSONArray data = new JSONArray(json);
          Log.d("data = " + data.toString(2));
          liste = DRJson.parseUdsendelserForProgramserie(data, DRData.instans);
          Log.d("liste = " + liste);
          adapter.notifyDataSetChanged();
          return;
        } catch (Exception e) {
          Log.d("Parsefejl: " + e + " for json=" + json);
          e.printStackTrace();
        }
        new AQuery(rod).id(R.id.tom).text(url + "   status=" + status.getCode() + "\njson=" + json);
      }
    });


    return rod;
  }

  /**
   * Viewholder designmønster - hold direkte referencer til de views og objekter der bruges hele tiden
   */
  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView startid;
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
        v = getLayoutInflater(null).inflate(R.layout.udsendelse_elem2_tid_titel_kunstner, parent, false);
        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.startid = a.id(R.id.startid).typeface(App.skrift_gibson).getTextView();
        a.id(R.id.slutttid).gone();
        v.setTag(vh);
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
      }

      // Opdatér viewholderens data
      vh.lydkilde = lydkilde;
      vh.startid.setText("" + lydkilde);
      //vh.titel.setText(lydkilde.titel);
      //a.id(R.id.stiplet_linje).visibility(position == aktuelUdsendelseIndex + 1 ? View.INVISIBLE : View.VISIBLE);
      //a.id(R.id.hør).visibility(lydkilde.kanHøres ? View.VISIBLE : View.GONE);

      udvikling_checkDrSkrifter(v, this.getClass() + " ");

      return v;
    }
  };

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Lydkilde u = liste.get(position);
    //startActivity(new Intent(getActivity(), VisFragment_akt.class)
    //    .putExtra(P_kode, kanal.kode)
    //    .putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName()).putExtra(DRJson.Slug.name(), u.slug)); // Udsenselses-ID

    Fragment f = new Udsendelse_frag();
    f.setArguments(new Intent()
        .putExtra(P_kode, u.kanal().kode)
        .putExtra(DRJson.Slug.name(), u.slug).getExtras());
    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
  }
}

