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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Udsendelse;

public class Kanal {

  public String kode; // P3
  public String slug; // p3
  public String navn;

  public String urn;
  public JSONObject json;

  /**
   * Eksemlelvis v3_kanalside__p3.json
   * http://www.dr.dk/tjenester/mu-apps/schedule/P3 .
   * Er sorteret kronologisk og skal vises sådan. Starter d.d. om morgenen.
   * Tilføj /1, /2, …  i URL for at se senere. /7 er max.
   * Se tidligere med /-1 , /-2 etc
   */
  //public String kanalside;
  public int kanalappendis_resid;
  public String lognøgle = "";
  public String logoUrl = "";
  public String logoUrl2 = "";
  public ArrayList<Udsendelse> udsendelser = new ArrayList<Udsendelse>();
  public SortedMap<Integer, ArrayList<Udsendelse>> udsendelserPerDag = new TreeMap<Integer, ArrayList<Udsendelse>>();
  public ArrayList<Lydstream> streams;

  @Override
  public String toString() {
    return kode;// + "/" + navn + "/" + logoUrl;
  }


  public static final Locale dansk = new Locale("da", "DA");
  public static final DateFormat klokkenformat = new SimpleDateFormat("HH:mm", dansk);
  public static final DateFormat datoformat = new SimpleDateFormat("d. MMM. yyyy", dansk);


  public void parsUdsendelser(JSONArray jsonArray, int dag) throws JSONException, ParseException {
    String nuDatoStr = datoformat.format(new Date());
    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Udsendelse u = new Udsendelse();
      u.json = o;
      u.startTid = DRJson.servertidsformat.parse(o.optString(DRJson.StartTime.name()));
      u.startTidKl = klokkenformat.format(u.startTid);
      u.slutTid = DRJson.servertidsformat.parse(o.optString(DRJson.EndTime.name()));
      u.slutTidKl = klokkenformat.format(u.slutTid);
      String datoStr = datoformat.format(u.startTid);
      if (!datoStr.equals(nuDatoStr)) u.startTidKl += " - " + datoStr;
      u.titel = o.optString(DRJson.Title.name());
      u.beskrivelse = o.optString(DRJson.Description.name());
      u.slug = o.optString(DRJson.Slug.name());
      u.programserieSlug = o.optString(DRJson.SeriesSlug.name());
      u.urn = o.optString(DRJson.Urn.name());
      uliste.add(u);
    }
    udsendelserPerDag.put(dag, uliste);
    udsendelser.clear();
    for (ArrayList<Udsendelse> ul : udsendelserPerDag.values()) udsendelser.addAll(ul);
  }


  public String getStreamsUrl() {
    return "http://www.dr.dk/tjenester/mu-apps/channel?includeStreams=true&urn=" + urn;
  }


  public String getUdsendelserUrl() {
    return "http://www.dr.dk/tjenester/mu-apps/schedule/" + kode;  // svarer til v3_kanalside__p3.json;
  }

  public String getPlaylisteUrl(Udsendelse u) {
    // http://www.dr.dk/tjenester/mu-apps/playlist/monte-carlo-352/p3
    return "http://www.dr.dk/tjenester/mu-apps/playlist/" + u.slug + "/" + slug;
  }

  public Udsendelse findUdsendelseFraSlug(String slug) {
    for (Udsendelse u : udsendelser) {
      if (u.slug.equals(slug)) return u;
    }
    return null;
  }
}
