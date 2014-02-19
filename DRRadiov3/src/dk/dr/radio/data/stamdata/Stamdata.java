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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Diverse;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;

public class Stamdata {
  /**
   * Grunddata
   */
  private JSONObject android_json;
  public JSONObject json;

  public List<String> kanalkoder = new ArrayList<String>();
  public List<String> p4koder = new ArrayList<String>();
  public List<Kanal> kanaler = new ArrayList<Kanal>();
  public Kanal forvalgtKanal;


  public HashMap<String, Kanal> kanalFraKode = new HashMap<String, Kanal>();
  public HashMap<String, Kanal> kanalFraUrn = new HashMap<String, Kanal>();
  public HashMap<String, Kanal> kanalFraSlug = new HashMap<String, Kanal>();
  private HashMap<String, Kanal> kanalFraLogonøgle = new HashMap<String, Kanal>();


  private void fjernKanalMedFejl(Kanal k) {
    kanaler.remove(k);
    kanalkoder.remove(k.kode);
    p4koder.remove(k.kode);
    kanalFraKode.remove(k.kode);
    kanalFraUrn.remove(k.urn);
    kanalFraSlug.remove(k.slug);
  }


  /**
   * Henter stamdata (faste data)
   *
   * @throws java.io.IOException hvis der er et problem med netværk
   *                             eller parsning (dvs interne fejl af forskellig art som bør rapporteres til udvikler)
   */
  public static Stamdata parseAndroidStamdata(String str) throws JSONException {
    Stamdata d = new Stamdata();
    JSONObject json = d.android_json = new JSONObject(str);


    return d;
  }

  private void parseKanaler(JSONArray jsonArray, boolean underkanal) throws JSONException {


    int antal = jsonArray.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = jsonArray.getJSONObject(i);
      Kanal k = new Kanal();
      k.kode = j.getString("scheduleIdent");
      k.navn = j.getString("title");
      k.lognøgle = j.getString("logo");
      k.urn = j.getString("urn");
      kanaler.add(k);
      if (underkanal) p4koder.add(k.kode);
      else kanalkoder.add(k.kode);
      kanalFraKode.put(k.kode, k);
      kanalFraUrn.put(k.urn, k);
      kanalFraLogonøgle.put(k.lognøgle, k);
      if (j.optBoolean("isDefault")) forvalgtKanal = k;

      JSONArray underkanaler = j.optJSONArray("channels");
      if (underkanaler != null) parseKanaler(underkanaler, true);
    }
  }

  public void parseFællesStamdata(String str) throws JSONException {
    Stamdata d = this;
    JSONObject json = d.json = new JSONObject(str);

    parseKanaler(json.getJSONArray("channels"), false);
    if (forvalgtKanal == null) forvalgtKanal = kanaler.get(2); // Det er nok P3 :-)

    JSONArray logoer = json.getJSONArray("logos");
    int antal = logoer.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = logoer.getJSONObject(i);
      Kanal k = kanalFraLogonøgle.get(j.getString("ident"));
      k.logoUrl = j.optString("image");
      k.logoUrl2 = j.optString("image@2x");
      // http://www.dr.dk/tjenester/mu-apps/schedule/P3 - svarer til v3_kanalside__p3.json
      //k.kanalside = "http://www.dr.dk/tjenester/mu-apps/schedule/" + k.kode;

      // http://www.dr.dk/tjenester/mu-apps/channel?urn=urn:dr:mu:bundle:4f3b8926860d9a33ccfdafb9&includeStreams=true
      //k.kanalside = "http://www.dr.dk/tjenester/mu-apps/channel?urn=" + k.urn+"&includeStreams=true";
    }

  }


  public void hentSupplerendeDataBg() {
    for (Kanal k : kanaler)
      try {
        String url = k.getStreamsUrl();
        String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
        JSONObject o = new JSONObject(data);
        k.slug = o.getString(DRJson.Slug.name());
        kanalFraSlug.put(k.slug, k);
        k.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
        Log.d(k.kode + " k.lydUrl=" + k.streams);
      } catch (Exception e) {
        Log.e(e);
      }
    ;
  }
}
