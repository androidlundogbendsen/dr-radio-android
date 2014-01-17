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

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import dk.dr.radio.data.spiller_nu.SpillerNu;
import dk.dr.radio.data.spiller_nu.SpillerNuElement;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.data.stamdata.Stamdata;
import dk.dr.radio.data.udsendelser.Udsendelse;
import dk.dr.radio.data.udsendelser.Udsendelser;
import dk.dr.radio.diverse.Log;


public class JsonIndlaesning {


  /**
   * Henter stamdata (faste data)
   *
   * @throws IOException hvis der er et problem med netværk
   *                     eller parsning (dvs interne fejl af forskellig art som bør rapporteres til udvikler)
   */
  public static Stamdata parseStamdata(String str) throws JSONException {

    //Log.d("str=\n============="+str+"\n==================");

    Stamdata d = new Stamdata();
    JSONObject json = d.json = new JSONObject(str);

    d.kanalkoder = jsonArrayTilArrayListString(json.getJSONArray("kanalkoder"));
    d.p4koder = jsonArrayTilArrayListString(json.getJSONArray("p4koder"));

    JSONArray kanaler = json.getJSONArray("kanaler");
    int antal = kanaler.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = kanaler.getJSONObject(i);
      Kanal k = new Kanal();
      k.shortName = j.optString("shortName", "");
      k.longName = j.optString("longName", "");
      k.aacUrl = j.optString("aacUrl", "");
      k.rtspUrl = j.optString("rtspUrl", "");
      k.shoutcastUrl = j.optString("shoutcastUrl", "");
      d.kanaler.add(k);
    }

    //Log.d("TIDSTAGNING parsning tog "+dt("parsning stamdataUrl"));
    //d.lavKanalkodeTilKanalMap();
    for (Kanal k : d.kanaler) {
      d.kanalkodeTilKanal.put(k.shortName, k);
    }

    d.kanalerDerSkalViseSpillerNu.addAll(jsonArrayTilArrayListString(json.getJSONArray("vis_spiller_nu")));

    return d;
  }


  static Udsendelser hentUdsendelser(String url) throws Exception {
    String jsondata = hentUrlSomStreng(url);

    JSONObject json = new JSONObject(jsondata);

    Udsendelser uds = new Udsendelser();

    uds.currentProgram = jsonTilUdsendelse(json.getJSONObject("currentProgram"));
    uds.nextProgram = jsonTilUdsendelse(json.getJSONObject("nextProgram"));
    //Log.d("TIDSTAGNING parsning tog "+dt("parsning "+url));
    return uds;
  }


  private static HttpClient httpClient;

  public static String hentUrlSomStreng(String url) throws IOException {
    // AndroidHttpClient er først defineret fra Android 2.2
    //if (httpClient == null) httpClient = android.net.http.AndroidHttpClient.newInstance("Android DRRadio");
    if (httpClient == null) {
      HttpParams params = new BasicHttpParams();
      HttpConnectionParams.setConnectionTimeout(params, 15 * 1000);
      HttpConnectionParams.setSoTimeout(params, 15 * 1000);
      HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
      HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
      //HttpProtocolParams.setUseExpectContinue(params, true);
      HttpProtocolParams.setUserAgent(params, "Android DRRadio/1.x");
      httpClient = new DefaultHttpClient(params);
    }
    //dt("");
    Log.d("Henter " + url);
    //Log.e(new Exception("Henter "+url));
    //InputStream is = new URL(url).openStream();

    HttpGet c = new HttpGet(url);
    HttpResponse response = httpClient.execute(c);
    InputStream is = response.getEntity().getContent();

    String jsondata = læsInputStreamSomStreng(is);
    //Log.d("Hentede "+url+" på "+dt("hente "+url));

    // frederik: GratisDanmark fix: Strip the file of XML tags that might ruin the JSON format
    jsondata.replaceAll("<[^>]*>", "");

    return jsondata;
  }

  public static String læsInputStreamSomStreng(InputStream is) throws IOException, UnsupportedEncodingException {

    // Det kan være nødvendigt at hoppe over BOM mark - se http://android.forums.wordpress.org/topic/xml-pull-error?replies=2
    //is.read(); is.read(); is.read(); // - dette virker kun hvis der ALTID er en BOM
    // Hop over BOM - hvis den er der!
    is = new BufferedInputStream(is);  // bl.a. FileInputStream understøtter ikke mark, så brug BufferedInputStream
    is.mark(1); // vi har faktisk kun brug for at søge én byte tilbage
    if (is.read() == 0xef) {
      is.read();
      is.read();
    } // Der var en BOM! Læs de sidste 2 byte
    else is.reset(); // Der var ingen BOM - hop tilbage til start


    final char[] buffer = new char[0x3000];
    StringBuilder out = new StringBuilder();
    Reader in = new InputStreamReader(is, "UTF-8");
    int read;
    do {
      read = in.read(buffer, 0, buffer.length);
      if (read > 0) {
        out.append(buffer, 0, read);
      }
    } while (read >= 0);
    in.close();
    return out.toString();
  }


  static SpillerNu hentSpillerNuListe(String url) throws Exception {

    String str = hentUrlSomStreng(url);
    SpillerNu d = new SpillerNu(); //mapper.readValue(jsondata, SpillerNu.class);

    JSONObject json = new JSONObject(str);
    JSONArray liste = json.getJSONArray("tracks");
    int antal = liste.length();
    for (int i = 0; i < antal; i++) {
      JSONObject j = liste.getJSONObject(i);
      SpillerNuElement e = new SpillerNuElement();
      e.title = j.optString("title", "");
      e.displayArtist = j.optString("displayArtist", "");
      e.lastFM = j.optString("lastFM", "");
      e.start = j.optString("start", "");
      d.liste.add(e);
    }

    return d;

  }

  /*
  private static long førsteTid;
  private static long sidsteTid;
  private static Map<String,Integer> tidstagning = new TreeMap<String,Integer>();

  private static String dt(String hvad) {
    hvad = hvad + " ";
    long nu = System.currentTimeMillis();
    int dt = (int) (nu - sidsteTid);
    sidsteTid = nu;

    if (førsteTid==0) førsteTid = nu;

    int samlet = dt;
    if (tidstagning.containsKey(hvad)) samlet += tidstagning.get(hvad);
    tidstagning.put(hvad, samlet);

    return dt + " ms";
  }*/

  public static ArrayList<String> jsonArrayTilArrayListString(JSONArray j) throws JSONException {
    int n = j.length();
    ArrayList<String> res = new ArrayList<String>(n);
    for (int i = 0; i < n; i++) {
      res.add(j.getString(i));
    }
    return res;
  }

  private static Udsendelse jsonTilUdsendelse(JSONObject j) throws JSONException {
    Udsendelse u = new Udsendelse();
    u.description = j.optString("description", "");
    u.start = j.optString("start", "");
    u.stop = j.optString("stop", "");
    u.title = j.optString("title", "");
    return u;
  }

}
