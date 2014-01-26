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

package dk.dr.radio.data.stamdata;

import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.dr.radio.data.JsonIndlaesning;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

public class Stamdata {
  /**
   * Grunddata
   */
  public JSONObject json;

  public List<String> kanalkoder = new ArrayList<String>();
  public List<String> p4koder = new ArrayList<String>();
  public List<Kanal> kanaler = new ArrayList<Kanal>();


  public HashMap<String, Kanal> kanalFraKode = new HashMap<String, Kanal>();
  public HashMap<String, Kanal> kanalFraSlug = new HashMap<String, Kanal>();
  /**
   * Liste over de kanaler der vises 'Spiller lige nu' med info om musiknummer på skærmen
   */
  public Set<String> kanalerDerSkalViseSpillerNu = new HashSet<String>();


  private void fjernKanalMedFejl(Kanal k) {
    kanaler.remove(k);
    kanalkoder.remove(k.kode);
  }


  /**
   * Henter stamdata (faste data)
   *
   * @throws java.io.IOException hvis der er et problem med netværk
   *                             eller parsning (dvs interne fejl af forskellig art som bør rapporteres til udvikler)
   */
  public static Stamdata parseStamdatafil(String str) throws JSONException {

    //Log.d("parseStamdatafil str=\n=============" + str + "\n==================");

    Stamdata d = new Stamdata();
    JSONObject json = d.json = new JSONObject(str);

    d.kanalkoder = JsonIndlaesning.jsonArrayTilArrayListString(json.getJSONArray("kanalkoder"));
    d.p4koder = JsonIndlaesning.jsonArrayTilArrayListString(json.getJSONArray("p4koder"));

    String pn = App.instans.getPackageName();
    Resources res = App.instans.getResources();

    JSONArray kanaler = json.getJSONArray("kanaler");
    int antal = kanaler.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = kanaler.getJSONObject(i);
      Kanal k = new Kanal();
      k.kode = j.getString("kode");
      k.navn = j.getString("navn");
      k.aacUrl = j.optString("aacUrl", "");
      k.rtspUrl = j.optString("rtspUrl", "");
      k.shoutcastUrl = j.optString("shoutcastUrl", "");
      k.slug = j.getString("Slug");
      k.kanalappendis_resid = res.getIdentifier("kanalappendix_" + k.kode.toLowerCase(), "drawable", pn);
      Log.d("kanalappendix_" + k.kode.toLowerCase() + "  resid=" + k.kanalappendis_resid);
      d.kanaler.add(k);
      d.kanalFraKode.put(k.kode, k);
      d.kanalFraSlug.put(k.slug, k);
    }

    d.kanalerDerSkalViseSpillerNu.addAll(JsonIndlaesning.jsonArrayTilArrayListString(json.getJSONArray("vis_spiller_nu")));

    return d;
  }


  public void parseAlleKanaler(String str) throws JSONException {
    //Log.d("parseAlleKanaler str=\n=============" + str + "\n==================");
    JSONArray alleKanaler = new JSONObject(str).getJSONArray("Data");
    int antal = alleKanaler.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = alleKanaler.getJSONObject(i);
      Kanal k = kanalFraSlug.get(j.optString("Slug"));
      if (k == null) continue; // Ignorer kanal
      try {
        k.json = j;
        Log.d(k.kode + " '" + k.navn + "' : slug=" + k.slug + "  '" + j.optString("Title"));
        JSONArray servere = j.optJSONArray("StreamingServers");
        if (servere != null) for (int n = 0; n < servere.length(); n++) {
          JSONObject s = servere.getJSONObject(n);
          String streamtype = s.optString("LinkType");
          k.lydUrl.put(streamtype, s);
          Log.d(k.kode + " '" + streamtype + "' : " + s);
        }
        k.kanalside = "http://www.dr.dk/tjenester/mu-apps/schedule/" + k.kode;
      } catch (Exception e) {
        Log.e(e);
        fjernKanalMedFejl(k);
      }
    }
  }
}
