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

import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;

public class Grunddata {
  /**
   * Grunddata
   */
  public JSONObject android_json;
  public JSONObject json;

  public List<String> kanalkoder = new ArrayList<String>();
  public List<String> p4koder = new ArrayList<String>();
  public List<Kanal> kanaler = new ArrayList<Kanal>();
  public Kanal forvalgtKanal;


  public HashMap<String, Kanal> kanalFraKode = new HashMap<String, Kanal>();
  public HashMap<String, Kanal> kanalFraSlug = new LinkedHashMap<String, Kanal>();
  public final Kanal ukendtKanal = new Kanal();

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
    kanalkoder.remove(k.kode);
    p4koder.remove(k.kode);
    kanalFraKode.remove(k.kode);
    kanalFraSlug.remove(k.slug);
  }


  private void parseKanaler(JSONArray jsonArray, boolean p4) throws JSONException {


    int antal = jsonArray.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = jsonArray.getJSONObject(i);
      Kanal k = new Kanal();
      k.kode = j.optString("scheduleIdent", "P4F");
      k.navn = j.getString("title");
      k.urn = j.getString("urn");
      k.slug = j.optString("slug", "p4");
      k.p4underkanal = p4;
      kanaler.add(k);
      if (p4) p4koder.add(k.kode);
      else kanalkoder.add(k.kode);
      kanalFraKode.put(k.kode, k);
      kanalFraSlug.put(k.slug, k);
      if (j.optBoolean("isDefault")) forvalgtKanal = k;

      JSONArray underkanaler = j.optJSONArray("channels");
      if (underkanaler != null) {
        if (!Kanal.P4kode.equals(k.kode)) Log.rapporterFejl(new IllegalStateException("Forkert P4-kode: " + k.kode));
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

    parseKanaler(json.getJSONArray("channels"), false);
    android_json = json.getJSONObject("android");
    if (forvalgtKanal == null) forvalgtKanal = kanaler.get(2); // Det er nok P3 :-)
  }


  public void hentSupplerendeDataBg_KUN_TIL_UDVIKLING() {
    for (Kanal k : kanaler)
      try {
        String url = k.getStreamsUrl();
        String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
        JSONObject o = new JSONObject(data);
        k.slug = o.getString(DRJson.Slug.name());
        kanalFraSlug.put(k.slug, k);
        k.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
        //Log.d(k.kode + " k.lydUrl=" + k.streams);
      } catch (Exception e) {
        Log.e(e);
      }
    ;
    Log.d("DRData.instans.grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug);
  }
}
