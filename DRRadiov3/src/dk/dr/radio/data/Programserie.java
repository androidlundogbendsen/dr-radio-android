package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import dk.dr.radio.diverse.Log;

/**
 * Created by j on 01-03-14.
 */
public class Programserie { //implements Serializable {
  //  private static final long serialVersionUID = 1L;
  public String titel;
  public String beskrivelse;
  public String slug;
  public int antalUdsendelser;
  public String urn;
  private ArrayList<Udsendelse> udsendelserListe;
  private TreeMap<Integer, ArrayList<Udsendelse>> udsendelserListeFraOffset = new TreeMap<Integer, ArrayList<Udsendelse>>();
  private TreeSet<Udsendelse> udsendelserSorteret;
  public Date senesteUdsendelseTid;

  public ArrayList<Udsendelse> getUdsendelser() {
    return udsendelserListe;
  }


  public void tilføjUdsendelser(int offset, ArrayList<Udsendelse> uds) {
    Log.d(this+ " tilføjUdsendelser:"+(udsendelserListe==null?"nul":udsendelserListe.size())+" elem liste:\n"+ udsendelserListe + "\nfår tilføjet "+(uds==null?"nul":uds.size())+" elem:\n" +uds);

    udsendelserListeFraOffset.put(offset, uds);
    Log.d("tilføjUdsendelser udsendelserListeFraOffset: "+ udsendelserListeFraOffset.keySet());

    if (this.udsendelserListe == null) {
      udsendelserSorteret = new TreeSet<Udsendelse>(uds);
      udsendelserListe = new ArrayList<Udsendelse>(udsendelserSorteret);
    } else {
      udsendelserListe.clear();
      for (ArrayList<Udsendelse> lx : udsendelserListeFraOffset.values()) {
        udsendelserListe.addAll(lx);
      }
      udsendelserSorteret.addAll(uds);
      if (!Arrays.equals(udsendelserListe.toArray(), udsendelserSorteret.toArray())) {
        Log.d("tilføjUdsendelser INKONSISTENS??!?nu:\nlisten:"+ udsendelserListe+"\nsorter:"+udsendelserSorteret);
      }
//      udsendelserListe.clear();
//      udsendelserListe.addAll(udsendelserSorteret);
    }
    //Log.d("tilføjUdsendelser nu:\n"+ udsendelserListe);
    /*
    {
      ArrayList<Udsendelse> udsendelser = this.udsendelserListe;
      Collections.sort(udsendelser);
      udsendelser = new ArrayList<Udsendelse>(new TreeSet<Udsendelse>(udsendelser));
      Log.d("tilføjUdsendelser sorteret ville være:\n"+udsendelser);
    }
    */

  }

  public int findUdsendelseIndexFraSlug(String slug) {
    int n = -1;
    if (udsendelserListe != null) {
      for (int i = 0; i < udsendelserListe.size(); i++) {
        if (slug.equals(udsendelserListe.get(i).slug)) n = i;
      }
    }
    return n;
  }
}
