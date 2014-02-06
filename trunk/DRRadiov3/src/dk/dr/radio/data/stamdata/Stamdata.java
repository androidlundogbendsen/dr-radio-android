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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Diverse;
import dk.dr.radio.diverse.App;
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
  private HashMap<String, Kanal> kanalFraLogonøgle = new HashMap<String, Kanal>();
  ;


  private void fjernKanalMedFejl(Kanal k) {
    kanaler.remove(k);
    kanalkoder.remove(k.kode);
    p4koder.remove(k.kode);
    kanalFraKode.remove(k.kode);
    kanalFraUrn.remove(k.urn);
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
        String url = "http://www.dr.dk/tjenester/mu-apps/channel?urn=" + k.urn + "&includeStreams=true";
        String data = Diverse.læsInputStreamSomStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
        JSONObject o = new JSONObject(data);
        k.slug = o.getString(DRJson.Slug.name());
        k.lydUrl = parsLyddata(o.getJSONArray(DRJson.Streams.name()));
        Log.d(k.kode + " k.lydUrl=" + k.lydUrl);
      } catch (Exception e) {
        Log.e(e);
      }
    ;
  }

  private static HashMap<String, String> parsLyddata(JSONArray sa) throws JSONException {
    HashMap<String, String> lydData = new HashMap<String, String>();
    for (int n = 0; n < sa.length(); n++) {
      JSONObject s = sa.getJSONObject(n);
      String lydUrl = s.getString(DRJson.Uri.name());
      lydData.put(null, lydUrl);
      lydData.put(s.getString(DRJson.Quality.name()), lydUrl);
    }
    return null;
  }


  /**
   * Henter stamdata (faste data)
   *
   * @throws java.io.IOException hvis der er et problem med netværk
   *                             eller parsning (dvs interne fejl af forskellig art som bør rapporteres til udvikler)
   */
  public static Stamdata skrald__parseStamdatafil(String str) throws JSONException {

    //Log.d("xxx_parseStamdatafil str=\n=============" + str + "\n==================");

    Stamdata d = new Stamdata();
    JSONObject json = d.json = new JSONObject(str);

    d.kanalkoder = Diverse.jsonArrayTilArrayListString(json.getJSONArray("kanalkoder"));
    d.p4koder = Diverse.jsonArrayTilArrayListString(json.getJSONArray("p4koder"));

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
      k.urn = j.getString("Slug");
      k.kanalappendis_resid = res.getIdentifier("kanalappendix_" + k.kode.toLowerCase(), "drawable", pn);
      Log.d("kanalappendix_" + k.kode.toLowerCase() + "  resid=" + k.kanalappendis_resid);
      d.kanaler.add(k);
      d.kanalFraKode.put(k.kode, k);
      d.kanalFraUrn.put(k.urn, k);
    }

    return d;
  }


  public void skrald__parseAlleKanaler(String str) throws JSONException {
    //Log.d("skrald_parseAlleKanaler str=\n=============" + str + "\n==================");
    JSONArray alleKanaler = new JSONObject(str).getJSONArray("Data");
    int antal = alleKanaler.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = alleKanaler.getJSONObject(i);
      Kanal k = kanalFraUrn.get(j.optString("Slug"));
      if (k == null) continue; // Ignorer kanal
      try {
        k.json = j;
        Log.d(k.kode + " '" + k.navn + "' : urn=" + k.urn + "  '" + j.optString("Title"));
        JSONArray servere = j.optJSONArray("StreamingServers");
        if (servere != null) for (int n = 0; n < servere.length(); n++) {
          JSONObject s = servere.getJSONObject(n);
          String streamtype = s.optString("LinkType");
          //k.lydUrl.put(streamtype, s);
          Log.d(k.kode + " '" + streamtype + "' : " + s);
        }
        //k.kanalside = "http://www.dr.dk/tjenester/mu-apps/schedule/" + k.kode;
      } catch (Exception e) {
        Log.e(e);
        fjernKanalMedFejl(k);
      }
    }
  }
}
