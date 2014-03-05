/**
 DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
 Some parts of the code are loosely based on Sveriges Radio Play for Android.

 DR Radio 2 for Android is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2 as published by
 the Free Software Foundation.

 DR Radio 2 for Android is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

 */

package dk.dr.radio.akt;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ImageViewTilBlinde;
import dk.dr.radio.v3.R;

public class Kanalvalg_v2_akt extends ListActivity {

  private KanalAdapter kanaladapter;
  private View[] listeElementer;
  private List<String> overordnedeKanalkoder;
  private int p4indeks;
  /**
   * Om P4-underlisten er åbnet.
   * static da det er en nem måde at få listen til at huske om den er åben 'næsten altid'
   */
  private static boolean p4erÅbnet;
  private List<String> p4koder;
  private List<String> alleKanalkoder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    overordnedeKanalkoder = DRData.instans.stamdata.kanalkoder;
    p4koder = DRData.instans.stamdata.p4koder;
    if (p4koder.get(0).equals(Kanal.P4kode)) p4koder.remove(0); // Selve P4 skal ikke vises som en del af underlisten
    p4indeks = overordnedeKanalkoder.indexOf(Kanal.P4kode);

    alleKanalkoder = new ArrayList<String>(overordnedeKanalkoder);
    alleKanalkoder.addAll(p4indeks + 1, p4koder); // P4's underkanaler ligger lige under P4-indgangen

    for (String k : alleKanalkoder)
      if (DRData.instans.stamdata.kanalFraKode.get(k) == null) {
        new IllegalStateException("Kanalkode mangler! Det her må ikke ske!").printStackTrace();
        DRData.instans.stamdata.kanalFraKode.put(k, new Kanal()); // reparér problemet :-(
      }


    // Da der er tale om et fast lille antal kanaler er der ikke grund til det store bogholderi
    // Så vi husker bare viewsne i er array
    listeElementer = new View[alleKanalkoder.size()];
    kanaladapter = new KanalAdapter();

    // Opbyg arrayet på forhånd for jævnere visning
    for (int pos = 0; pos < listeElementer.length; pos++) kanaladapter.bygListeelement(pos);

    setListAdapter(kanaladapter);
    // Sæt baggrunden. Normalt ville man gøre det fra XML eller med
    //getListView().setBackgroundResource(R.drawable.main_app_bg);

    ListView lv = getListView();
/*
    // Vi ønsker en mørkere udgave af baggrunden, så vi indlæser den
    // her og sætter et farvefilter.
    Drawable baggrund = getResources().getDrawable(R.drawable.main_app_bg);
    baggrund = baggrund.mutate();
    baggrund.setColorFilter(0xffa0a0a0, Mode.MULTIPLY);
*/

//    lv.setBackgroundColor( 0xffa0a0a0);
//    lv.setDivider(new ColorDrawable(0x80ffffff));
//    lv.setDividerHeight(2);

    // Sørg for at baggrunden bliver tegnet, også når listen scroller.
    // Se http://android-developers.blogspot.com/2009/01/why-is-my-list-black-android.html
    lv.setCacheColorHint(0x00000000);
    // Man kunne have en ensfarvet baggrund, det gør scroll mere flydende
    //getListView().setCacheColorHint(0xffe4e4e4);
  }


  private class KanalAdapter extends BaseAdapter {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    Resources res = getResources();

    private View bygListeelement(int position) {

      String kanalkode = alleKanalkoder.get(position);
      Kanal kanal = DRData.instans.stamdata.kanalFraKode.get(kanalkode);
      View view = mInflater.inflate(R.layout.kanalvalg_elem, null);
      ImageViewTilBlinde billede = (ImageViewTilBlinde) view.findViewById(R.id.billede);
      ImageViewTilBlinde ikon = (ImageViewTilBlinde) view.findViewById(R.id.ikon);
      TextView textView = (TextView) view.findViewById(R.id.tekst);
      //Log.d("billedebilledebilledebillede"+billede+ikon+textView);
      // Sæt åbne/luk-ikon for P4 og højttalerikon for kanal
      if (position == p4indeks) {
        sætP4åbnLukIkon(ikon);
      } else if (DRData.instans.aktuelKanal.kode.equals(kanalkode)) {
        ikon.setImageResource(R.drawable.kanalvalg_spiller);
        ikon.blindetekst = "Spiller nu";
      } else ikon.setVisibility(View.INVISIBLE);
      if (kanal.kanallogo_resid != 0) {
        // Element med billede
        billede.setVisibility(View.VISIBLE);
        billede.setImageResource(kanal.kanallogo_resid);
        billede.blindetekst = kanal.navn;
        textView.setVisibility(View.GONE);
      } else {
        // Element uden billede - P4
        billede.setVisibility(View.GONE);
        //billede.setVisibility(View.VISIBLE);
        //billede.setImageResource(R.drawable.kanalappendix_p4f);
        textView.setVisibility(View.VISIBLE);
        textView.setText(kanal.navn);
        textView.setTypeface(App.skrift_gibson_fed);
      }

      return view;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
      // Hop over p4's positioner i tilfælde af at P4 ikke er åbnet
      if (!p4erÅbnet && position > p4indeks) position += p4koder.size();

      View view = listeElementer[position];
      if (view != null) return view; // Elementet er allede konstrueret

      view = bygListeelement(position);
      listeElementer[position] = view; // husk til næste gang
      return view;
    }

    public int getCount() {
      return p4erÅbnet ? alleKanalkoder.size() : overordnedeKanalkoder.size();
    }

    public Object getItem(int position) {
      return null;
    }

    public long getItemId(int position) {
      return position;
    }
  }


  private void sætP4åbnLukIkon(ImageViewTilBlinde ikon) {
    ikon.setImageResource(p4erÅbnet ? R.drawable.kanalvalg_minus : R.drawable.kanalvalg_plus);
    ikon.blindetekst = (p4erÅbnet ? "Luk" : "Åben");
  }


  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    if (position == p4indeks) {
      p4erÅbnet = !p4erÅbnet;
      // Opdatér plus/minus på P4-kanal
      ImageViewTilBlinde åbneLukIkon = (ImageViewTilBlinde) listeElementer[p4indeks].findViewById(R.id.ikon);
      sætP4åbnLukIkon(åbneLukIkon);
      // Fortæl at antal elementer i listen er ændret
      kanaladapter.notifyDataSetChanged();
      return;
    }

    // Hop over p4's positioner i tilfælde af at P4 ikke er åbnet
    if (!p4erÅbnet && position > p4indeks) position += p4koder.size();
    String kanalkode = alleKanalkoder.get(position);


    Kanal kanal = DRData.instans.stamdata.kanalFraKode.get(kanalkode);
    if (kanal.p4underkanal) {
      App.prefs.edit().putString(App.P4_FORETRUKKEN_AF_BRUGER, kanalkode).commit();
    }
    //Toast.makeText(this, "Klik på "+position+" "+kanal.longName, Toast.LENGTH_LONG).show();

    if (kanalkode.equals(DRData.instans.aktuelKanal.kode)) setResult(RESULT_CANCELED);
    else setResult(RESULT_OK);  // Signalér til kalderen at der er skiftet kanal!!

    // Ny kanal valgt - send valg til afspiller (ændrer også drData.aktuelKanalkode)
//TODO    DRData.instans.skiftKanal(kanalkode);

    // Hop tilbage til kalderen (hovedskærmen)
    finish();
  }
}
