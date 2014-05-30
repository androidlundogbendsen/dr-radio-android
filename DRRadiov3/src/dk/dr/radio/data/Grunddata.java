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

package dk.dr.radio.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Observer;

import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;

public class Grunddata {
  /**
   * Grunddata
   */
  public JSONObject android_json;
  public JSONObject json;

  public List<String> p4koder = new ArrayList<String>();
  public List<Kanal> kanaler = new ArrayList<Kanal>();
  public Kanal forvalgtKanal;
  public ArrayList<Runnable> observatører = new ArrayList<Runnable>(); // Om grunddata/stamdata ændrer sig


  public HashMap<String, Kanal> kanalFraKode = new HashMap<String, Kanal>();
  public HashMap<String, Kanal> kanalFraSlug = new LinkedHashMap<String, Kanal>();
  public static final Kanal ukendtKanal = new Kanal();
  public long opdaterGrunddataEfterMs = 30*60*1000;

  public Grunddata() {
    ukendtKanal.navn = "";
    ukendtKanal.slug = "";
    ukendtKanal.kode = "";
    ukendtKanal.urn = "";
    kanalFraKode.put(null, ukendtKanal);
    kanalFraKode.put("", ukendtKanal);
    kanalFraSlug.put(null, ukendtKanal);
    kanalFraSlug.put("", ukendtKanal);
  }


  private void fjernKanalMedFejl(Kanal k) {
    kanaler.remove(k);
    p4koder.remove(k.kode);
    kanalFraKode.remove(k.kode);
    kanalFraSlug.remove(k.slug);
  }


  private void parseKanaler(JSONArray jsonArray, boolean parserP4underkanaler) throws JSONException {

    int antal = jsonArray.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = jsonArray.getJSONObject(i);
      String kanalkode = j.optString("scheduleIdent", "P4F");
      Kanal k = kanalFraKode.get(kanalkode);
      if (k==null) {
        k = new Kanal();
        k.kode = j.optString("scheduleIdent", "P4F");
        kanalFraKode.put(k.kode, k);
      }
      k.navn = j.getString("title");
      k.urn = j.getString("urn");
      k.slug = j.optString("slug", "p4");
      k.p4underkanal = parserP4underkanaler;
      kanaler.add(k);
      if (parserP4underkanaler) p4koder.add(k.kode);
      kanalFraSlug.put(k.slug, k);
      if (j.optBoolean("isDefault")) forvalgtKanal = k;

      JSONArray underkanaler = j.optJSONArray("channels");
      if (underkanaler != null) {
        if (!Kanal.P4kode.equals(k.kode)) Log.rapporterFejl(new IllegalStateException("Forkert P4-kode: "), k.kode);
        parseKanaler(underkanaler, true);
      }
    }
  }

  /**
   * Henter grunddata (faste data)
   *
   * @throws java.io.IOException hvis der er et problem med netværk
   *                             eller parsning (dvs interne fejl af forskellig art som bør rapporteres til udvikler)
   */
  public void parseFællesGrunddata(String str) throws JSONException {
    json = new JSONObject(str);

    try {
      DRData.instans.grunddata.opdaterGrunddataEfterMs = json.getJSONObject("intervals").getInt("settings");
    } catch (Exception e) { Log.e(e); } // Ikke kritisk

    kanaler.clear();
    p4koder.clear();
    parseKanaler(json.getJSONArray("channels"), false);
    Log.d("parseKanaler "+kanaler+" - P4:"+p4koder);
    android_json = json.getJSONObject("android");
    if (forvalgtKanal == null) forvalgtKanal = kanaler.get(2); // Det er nok P3 :-)
    for (Runnable r : new ArrayList<Runnable>(observatører)) r.run();
  }


  public void hentSupplerendeDataBg_KUN_TIL_UDVIKLING() {
    for (Kanal k : kanaler)
      try {
        String url = k.getStreamsUrl();
        String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
        JSONObject o = new JSONObject(data);
        //k.slug = o.getString(DRJson.Slug.name());
        //kanalFraSlug.put(k.slug, k);
        k.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
        //Log.d(k.kode + " k.lydUrl=" + k.streams);
      } catch (Exception e) {
        Log.e(e);
      }
    ;
    Log.d("DRData.instans.grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug);
  }
}
