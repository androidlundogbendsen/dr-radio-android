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

package dk.dr.radio.data.json.stamdata;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Stamdata {
  /**
   * Grunddata
   */
  public JSONObject json;

  public List<String> all = new ArrayList<String>();
  public List<String> p4 = new ArrayList<String>();
  public List<Kanal> kanaler = new ArrayList<Kanal>();


  public HashMap<String, Kanal> kanalkodeTilKanal = new HashMap<String, Kanal>();
  /**
   * Liste over de kanaler der vises 'Spiller lige nu' med info om musiknummer på skærmen
   */
  public Set<String> kanalerDerSkalViseSpillerNu = new HashSet<String>();

  /**
   * Slår en streng op efter en nøgle. Giver "" i fald nøglen ikke findes +stakspor i loggen
   */
  public String s(String nøgle) {
    return json.optString(nøgle, "");
  }

}
