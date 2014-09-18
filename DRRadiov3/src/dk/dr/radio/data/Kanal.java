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

import org.json.JSONException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

public class Kanal extends Lydkilde {
  private static final long serialVersionUID = 1L;

  public String kode; // P3
  public static final String P4kode = "P4F";
  public String navn;
  public int kanallogo_resid;
  public boolean p4underkanal;
  public transient ArrayList<Udsendelse> udsendelser = new ArrayList<Udsendelse>();
  public transient SortedMap<String, ArrayList<Udsendelse>> udsendelserPerDag = new TreeMap<String, ArrayList<Udsendelse>>();

  @Override
  public String toString() {
    return kode;// + "/" + navn + "/" + logoUrl;
  }

  public boolean harUdsendelserForDag(String dato) {
    return udsendelserPerDag.containsKey(dato);
  }

  public void setUdsendelserForDag(ArrayList<Udsendelse> uliste, String dato) throws JSONException, ParseException {
    udsendelserPerDag.put(dato, uliste);
    udsendelser.clear();
    for (ArrayList<Udsendelse> ul : udsendelserPerDag.values()) udsendelser.addAll(ul);
  }


  @Override
  public String getStreamsUrl() {
    //return "http://www.dr.dk/tjenester/mu-apps/channel?includeStreams=true&urn=" + urn;
    return "http://www.dr.dk/tjenester/mu-apps/channel/" + slug + "?includeStreams=true";
  }


  public String getUdsendelserUrl() {
    return "http://www.dr.dk/tjenester/mu-apps/schedule/" + kode;  // svarer til v3_kanalside__p3.json;
  }

  @Override
  public Kanal getKanal() {
    return this;
  }

  @Override
  public boolean erDirekte() {
    return true;
  }

  @Override
  public Udsendelse getUdsendelse() {
    int n = getAktuelUdsendelseIndex();
    if (n >= 0) return udsendelser.get(n);
    return null;
  }

  public int getAktuelUdsendelseIndex() {
    if (udsendelser.size()==0) return -2;
    Date nu = new Date(App.serverCurrentTimeMillis()); // Kompenseret for forskelle mellem telefonens ur og serverens ur
    // Nicolai: "jeg løber listen igennem fra bunden og op,
    // og så finder jeg den første der har starttid >= nuværende tid + sluttid <= nuværende tid."
    for (int n = udsendelser.size() - 1; n >= 0; n--) {
      Udsendelse u = udsendelser.get(n);
      //Log.d(n + " " + nu.after(u.startTid) + u.slutTid.before(nu) + "  " + u);
      if (u.startTid.before(nu)) { // && nu.before(u.slutTid)) {
        return n;
      }
    }
    Log.e(new IllegalStateException("Ingen aktuel udsendelse fundet!"));
    Log.d("nu = " + nu+"  - "+nu.getTime()+" "+DRJson.servertidsformat.format(nu));
    for (int n=0; n<udsendelser.size(); n++) {
      Udsendelse u = udsendelser.get(n);
      Log.d(n + " " + u.startTid.before(nu) + nu.before(u.slutTid) + "  " + u+" "+DRJson.servertidsformat.format(u.startTid)+" - "+DRJson.servertidsformat.format(u.slutTid));
    }
    if (nu.before(udsendelser.get(0).slutTid)) return 0;
    return -2;
  }
}
