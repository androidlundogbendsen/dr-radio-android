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

package dk.dr.radio;

import dk.dr.radio.data.DRData;
import java.util.List;


import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import dk.dr.radio.data.json.stamdata.Kanal;
import dk.dr.radio.util.Log;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

public class Kanalvalg_akt extends ListActivity {

	private DRData drData;
	private CustomChannelAdapter adapter;
  private View[] listeElementer;
  //private HashMap<String, Kanal> kanalkodeTilKanal;
  private Typeface skrift_DRiBold;
  private List<String> overordnedeKanalkoder;
  private int p4indeks;
  /** Om P4-underlisten er åbnet. static da det er en nem måde at få listen til at huske om den er åben 'næsten altid' */
  private static boolean p4erÅbnet;
  private List<String> p4koder;
  private List<String> alleKanalkoder;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new CustomChannelAdapter();
    try {
      drData = DRData.tjekInstansIndlæst(this);
    } catch (Exception ex) {
      Log.e(ex);
      finish(); // Hop ud!
      return;
    }
		overordnedeKanalkoder = drData.stamdata.all;
		p4koder = drData.stamdata.p4;
    if (p4koder.get(0).equals("P4")) p4koder.remove(0); // Selve P4 skal ikke vises som en del af underlisten
    p4indeks = overordnedeKanalkoder.indexOf("P4");

    alleKanalkoder = new ArrayList<String>(overordnedeKanalkoder);
    alleKanalkoder.addAll(p4indeks+1, p4koder); // P4's underkanaler ligger lige under P4-indgangen

    for (String k : alleKanalkoder) if (drData.stamdata.kanalkodeTilKanal.get(k)==null) {
      new IllegalStateException("Kanalkode mangler! Det her må ikke ske!").printStackTrace();
      drData.stamdata.kanalkodeTilKanal.put(k, new Kanal()); // reparér problemet :-(
    }



    // Da der er tale om et fast lille antal kanaler er der ikke grund til det store bogholderi
    // Så vi husker bare viewsne i er array
    listeElementer = new View[alleKanalkoder.size()];

    setListAdapter(adapter);
    getListView().setBackgroundResource(R.drawable.main_app_bg);

    try { // DRs skrifttyper er ikke offentliggjort i SVN, derfor kan følgende fejle:
      skrift_DRiBold = Typeface.createFromAsset(getAssets(),"DRiBold.otf");
    } catch (Exception e) {
      Log.e("DRs skrifttyper er ikke tilgængelige", e);
    }
	}


	private class CustomChannelAdapter extends BaseAdapter {

		public View getView(int position, View convertView, ViewGroup parent) {
      // Hop over p4's positioner i tilfælde af at P4 ikke er åbnet
      if (!p4erÅbnet && position>p4indeks) position += p4koder.size();

      View view = listeElementer[position];

      if (view != null) return view; // Elementet er allede konstrueret

      String kanalkode = alleKanalkoder.get(position);
      Kanal kanal = drData.stamdata.kanalkodeTilKanal.get(kanalkode);
      // tjek om der er et billede i 'drawable' med det navn filnavn
      int id = res.getIdentifier("kanal_"+kanalkode.toLowerCase(), "drawable", getPackageName());

			//System.out.println("getView " + position + " kanal_" + kanalkode.toLowerCase() + " type = " + id);
      view = mInflater.inflate(R.layout.kanalvalg_element, null);
      ImageView billede = (ImageView) view.findViewById(R.id.billede);
      ImageView ikon = (ImageView)view.findViewById(R.id.ikon);
      TextView textView = (TextView)view.findViewById(R.id.tekst);

      // Sæt åbne/luk-ikon for P4 og højttalerikon for kanal
      if (position == p4indeks)
        ikon.setImageResource( p4erÅbnet? R.drawable.icon_minus : R.drawable.icon_plus);
      else if (drData.aktuelKanalkode.equals(kanalkode))
        ikon.setImageResource(R.drawable.icon_playing);
      else
        ikon.setVisibility(View.INVISIBLE);

      //Log.d("billedebilledebilledebillede"+billede+ikon+textView);

      if (id != 0) {
        // Element med billede
        billede.setVisibility(View.VISIBLE);
        billede.setImageResource(id);
        textView.setVisibility(View.GONE);
      } else {
        // Element uden billede
        billede.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);
        String visningsNavn = kanal.longName;
        textView.setText(visningsNavn);
        if (skrift_DRiBold!=null) textView.setTypeface(skrift_DRiBold);
      }

      listeElementer[position] = view; // husk til næste gang
			return view;
		}

		LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    Resources res = getResources();

		public int getCount() {
			return p4erÅbnet?alleKanalkoder.size():overordnedeKanalkoder.size();
		}

    public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}
	}

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    if (position == p4indeks) {
      p4erÅbnet = !p4erÅbnet;
      // Opdatér plus/minus på P4-kanal
      ImageView åbneLukIkon = (ImageView)listeElementer[p4indeks].findViewById(R.id.ikon);
      åbneLukIkon.setImageResource( p4erÅbnet? R.drawable.icon_minus : R.drawable.icon_plus);
      // Fortæl at antal elementer i listen er ændret
      adapter.notifyDataSetChanged();
      return;
    }

    // Hop over p4's positioner i tilfælde af at P4 ikke er åbnet
    if (!p4erÅbnet && position>p4indeks) position += p4koder.size();
    String kanalkode = alleKanalkoder.get(position);


    //Kanal kanal = drData.stamdata.kanalkodeTilKanal.get(kanalkode);
    //Toast.makeText(this, "Klik på "+position+" "+kanal.longName, Toast.LENGTH_LONG).show();

    if (kanalkode.equals(drData.aktuelKanalkode)) setResult(RESULT_CANCELED);
    else setResult(RESULT_OK);  // Signalér til kalderen at der er skiftet kanal!!

    // Ny kanal valgt - send valg til afspiller
    drData.skiftKanal(kanalkode);

    // Hop tilbage til kalderen (hovedskærmen)
    finish();
  }
}