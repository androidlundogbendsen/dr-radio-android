package dk.dr.radio.diverse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import dk.dr.radio.data.Diverse;

/**
 * Created by j on 19-02-14.
 */
public class P4Stedplacering {
  /*
  wget --referer 'http://www.dr.dk/radio' http://j.maxmind.com/app/geoip.js

     */
  public static String findP4KanalnavnFraIP() throws Exception {
    URL u = new URL("http://j.maxmind.com/app/geoip.js");
    HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
    urlConnection.setRequestProperty("Referer", "http://www.dr.dk/radio");
    urlConnection.setConnectTimeout(15000);
    urlConnection.setReadTimeout(90 * 1000);   // 1 1/2 minut
    urlConnection.connect(); // http://stackoverflow.com/questions/8179658/urlconnection-getcontent-return-null
    InputStream is = urlConnection.getInputStream();
    if (urlConnection.getResponseCode() != 200)
      throw new IOException("HTTP-svar var " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage() + " for " + u);

    String str = Diverse.læsStreng(is);
    Log.d("p4Kanalnavn " + str);

    double lat = 0, lon = 0;
    /* str er nu
function geoip_country_name() { return 'Denmark'; }
function geoip_city()         { return 'Copenhagen'; }
function geoip_region()       { return '17'; }
function geoip_region_name()  { return 'Hovedstaden'; }
function geoip_latitude()     { return '55.6667'; }
function geoip_longitude()    { return '12.5833'; }
function geoip_postal_code()  { return ''; }
function geoip_area_code()    { return ''; }
function geoip_metro_code()   { return ''; }
     */

    for (String l : str.split("\n")) {
      if (l.contains("latitude")) {
        lat = Double.parseDouble(l.split("'")[1]); // split efter '
      } else if (l.contains("longitude")) {
        lon = Double.parseDouble(l.split("'")[1]); // split efter '
      } else if (l.contains("country_name")) {
        if (!"Denmark".equals(l.split("'")[1])) return null; // Hop ud hvis vi er uden for Danmark
      }
    }
    if (lat == 0 || lon == 0) return null;

    Log.d("p4Kanalnavn lat=" + lat + " lon=" + lon);

    P4Geokoordinater.Omraade nærmeste = P4Geokoordinater.findNærmesteOmråde(lat, lon);
    Log.d("p4Kanalnavn nærmere område: " + nærmeste.bynavn + "  " + nærmeste.p4kode);

    return nærmeste.p4kode;
  }
/*

   */

  /**
   * Til afprøvning
   */
  public static void main(String[] a) throws Exception {
    findP4KanalnavnFraIP();
  }


}
