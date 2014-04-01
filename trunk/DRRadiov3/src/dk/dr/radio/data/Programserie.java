package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
  private TreeSet<Udsendelse> udsendelserSorteret;
  public Date senesteUdsendelseTid;

  public ArrayList<Udsendelse> getUdsendelser() {
    return udsendelserListe;
  }

  /*
  private static Comparator<Udsendelse> udsendelseComparator = new Comparator<Udsendelse>() {

    @Override
    public int compare(Udsendelse u1, Udsendelse u2) {
      int e1 = u1.episodeIProgramserie;
      int e2 = u2.episodeIProgramserie;
      return e2 < e1 ? 1 : (e2 == e1 ? 0 : -1);
    }  };
  */

  public void tilføjUdsendelser(List<Udsendelse> uds) {
    Log.d("tilføjUdsendelser:\n"+ udsendelserListe + "\nfår tilføjet:\n" +uds);
    if (this.udsendelserListe == null) {
      udsendelserSorteret = new TreeSet<Udsendelse>(uds);
      udsendelserListe = new ArrayList<Udsendelse>(udsendelserSorteret);
    } else {
      if (udsendelserListe.containsAll(uds)) {
        Log.d("tilføjUdsendelser - liste allerede tilføjet.");
        return;
      }
      udsendelserListe.addAll(uds);
      udsendelserSorteret.addAll(uds);
      if (!Arrays.equals(udsendelserListe.toArray(), udsendelserSorteret.toArray())) {
        Log.d("tilføjUdsendelser INKONSISTENS??!?nu:\n"+ udsendelserListe+"\n"+udsendelserSorteret);
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
