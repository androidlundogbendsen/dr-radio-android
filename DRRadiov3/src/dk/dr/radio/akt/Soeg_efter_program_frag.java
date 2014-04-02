package dk.dr.radio.akt;

//import android.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.volley.Request;
import com.androidquery.AQuery;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.R;


public class Soeg_efter_program_frag extends Basisfragment implements
    OnClickListener, AdapterView.OnItemClickListener {

  private ListView listView;
  private EditText søgFelt;
  private ArrayList<Udsendelse> liste = new ArrayList<Udsendelse>();
  protected View rod;
  private ImageView søgKnap;
  private String url;
  private TextView tomStr;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rod = inflater.inflate(R.layout.soeg_efter_program_frag, container, false);

    AQuery aq = new AQuery(rod);
    listView = aq.id(R.id.listView).adapter(adapter).itemClicked(this)
        .getListView();
    listView.setEmptyView(aq.id(R.id.tom).typeface(App.skrift_gibson_fed)
            .text("").getView());

    søgFelt = aq.id(R.id.soegFelt).getEditText();
      søgFelt.setImeActionLabel("Søg", KeyEvent.KEYCODE_ENTER);

      //søgFelt.setBackgroundResource(android.R.drawable.editbox_background_normal);
    søgKnap = aq.id(R.id.soegKnap).clicked(this).getImageView();
      //søgKnap.setBackgroundResource(R.drawable.knap_graa10_bg);
      søgKnap.setVisibility(View.VISIBLE);
    tomStr = aq.id(R.id.tom).getTextView();

    søgFelt.addTextChangedListener(new TextWatcher() {

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
          Log.d("JPER text changed");
          if (søgFelt.getText().toString().length() > 0) {

              søgKnap.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);

          } else {
              søgKnap.setImageResource(R.drawable.dri_soeg_blaa);
          }

      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {

          if (søgFelt.getText().toString().length() > 0) {

          } else {
              tomStr.setText("");
          }


      }
    });

    /*Lytter efter enter key */
      søgFelt.setOnEditorActionListener(new OnEditorActionListener() {
          @Override
          public boolean onEditorAction(TextView v, int actionId,
                                        KeyEvent event) {
              boolean handled = false;
              if (actionId == KeyEvent.KEYCODE_ENTER) {
                  searchProgram();

                  handled = true;
              }
              return handled;
          }
      });

    udvikling_checkDrSkrifter(rod, this + " rod");
    /*
     * Kald
		 * http://www.dr.dk/tjenester/mu-apps/search/programs?q=monte&type=radio
		 * vil kun returnere radio programmer
		 * http://www.dr.dk/tjenester/mu-apps/search/series?q=monte&type=radio
		 * vil kun returnere radio serier
		 */

    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    App.volleyRequestQueue.cancelAll(this);
  }

  /**
   * Viewholder designmønster - hold direkte referencer til de views og
   * objekter der bruges hele tiden
   */
  private static class Viewholder {
    public AQuery aq;
    public TextView titel;
    public TextView startid;
    public Udsendelse udsendelse;
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
      Udsendelse udsendelse = liste.get(position);
      if (v == null) {
        v = getLayoutInflater(null).inflate(
            R.layout.elem_tid_titel_kunstner, parent,false);
          v.setBackgroundResource(0);
        vh = new Viewholder();
        a = vh.aq = new AQuery(v);
        vh.startid = a.id(R.id.startid).typeface(App.skrift_gibson).getTextView();
        a.id(R.id.slutttid).gone();

        vh.titel = a.id(R.id.titel_og_kunstner).typeface(App.skrift_gibson_fed).getTextView();
        v.setTag(vh);
      } else {
        vh = (Viewholder) v.getTag();
        a = vh.aq;
      }

      // Opdatér viewholderens data
      vh.udsendelse = udsendelse;

//      SimpleDateFormat ft = new SimpleDateFormat("HH:mm");
      Date startTid = udsendelse.getUdsendelse().startTid;

      vh.startid.setText(DRJson.datoformat.format(startTid)/*"" + ft.format(startTid)*/);

      String titel = udsendelse.getUdsendelse().titel;
      Spannable spannable = new SpannableString(titel);
      spannable.setSpan(App.skrift_gibson_fed_span, 0, titel.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      vh.titel.setText(spannable);

      // vh.titel.setText(udsendelse.titel);
      // a.id(R.id.stiplet_linje).visibility(position ==
      // aktuelUdsendelseIndex + 1 ? View.INVISIBLE : View.VISIBLE);
      // a.id(R.id.hør).visibility(udsendelse.kanHøres ? View.VISIBLE :
      // View.GONE);

      udvikling_checkDrSkrifter(v, this.getClass() + " ");

      return v;
    }
  };
  private String søgStr;

  @Override
  public void onItemClick(AdapterView<?> listView, View v, int position, long id) {
    Udsendelse u = liste.get(position);
    // startActivity(new Intent(getActivity(), VisFragment_akt.class)
    // .putExtra(P_kode, getKanal.kode)
    // .putExtra(VisFragment_akt.KLASSE,
    // Udsendelse_frag.class.getName()).putExtra(DRJson.Slug.name(),
    // u.slug)); // Udsenselses-ID

    Fragment f = new Udsendelse_frag();
    f.setArguments(new Intent().putExtra(P_kode, u.getKanal().kode)
        .putExtra(DRJson.Slug.name(), u.slug).getExtras());
    getActivity().getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.indhold_frag, f)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
  }

  @Override
  public void onClick(View v) {
      søgStr = søgFelt.getText().toString();
      if (søgStr.length() > 0) {
          søgFelt.setText("");
      } else {

      }


  }

    public void searchProgram() {
        Log.d("Liste " + liste);


        søgStr = søgFelt.getText().toString();

        url = "http://www.dr.dk/tjenester/mu-apps/search/programs?q=" + søgStr + "&type=radio";

        Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
            @Override
            public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
                if (json != null && !"null".equals(json) && !uændret) {
                    JSONArray data = new JSONArray(json);
                    Log.d("data = " + data.toString(2));
                    liste = DRJson.parseUdsendelserForProgramserie(data, DRData.instans);
                    Log.d("liste = " + liste);
                    adapter.notifyDataSetChanged();

                    if (liste.size() == 0) {
                        tomStr.setText("Søgningen gav intet resultat");
                    }
                    return;
                }
                Log.d("Slut søgning!");
            }
        }).setTag(this);
        App.volleyRequestQueue.add(req);
    }
}
