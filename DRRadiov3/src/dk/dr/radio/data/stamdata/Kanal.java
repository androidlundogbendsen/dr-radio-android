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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Udsendelse;

public class Kanal {

  public String kode; // P3
  public static final String P4kode = "P4F";
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
  public int kanallogo_resid;
  public boolean p4underkanal;
  public String lognøgle = "";
  public ArrayList<Udsendelse> udsendelser = new ArrayList<Udsendelse>();
  public SortedMap<String, ArrayList<Udsendelse>> udsendelserPerDag = new TreeMap<String, ArrayList<Udsendelse>>();
  public ArrayList<Lydstream> streams;

  @Override
  public String toString() {
    return kode;// + "/" + navn + "/" + logoUrl;
  }


  public void setUdsendelserForDag(ArrayList<Udsendelse> uliste, String dato) throws JSONException, ParseException {
    udsendelserPerDag.put(dato, uliste);
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
