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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import dk.dr.radio.diverse.Log;


public class JsonIndlaesning {


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


  public static ArrayList<String> jsonArrayTilArrayListString(JSONArray j) throws JSONException {
    int n = j.length();
    ArrayList<String> res = new ArrayList<String>(n);
    for (int i = 0; i < n; i++) {
      res.add(j.getString(i));
    }
    return res;
  }
}
