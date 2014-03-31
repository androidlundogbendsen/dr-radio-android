package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

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
  private ArrayList<Udsendelse> udsendelser;
  public Date senesteUdsendelseTid;

  public ArrayList<Udsendelse> getUdsendelser() {
    return udsendelser;
  }

  /*
  private static Comparator<Udsendelse> udsendelseComparator = new Comparator<Udsendelse>() {

    @Override
    public int compare(Udsendelse u1, Udsendelse u2) {
      int e1 = u1.episodeIProgramserie;
      int e2 = u2.episodeIProgramserie;
      return e2 < e1 ? 1 : (e2 == e1 ? 0 : -1);
    }
  };
  */

  public void tilføjUdsendelser(List<Udsendelse> uds) {
    if (this.udsendelser == null) {
      this.udsendelser = new ArrayList<Udsendelse>(uds);
    } else {
      this.udsendelser.addAll(uds);
      //Collections.sort(udsendelser);
      this.udsendelser = new ArrayList<Udsendelse>(new TreeSet<Udsendelse>(this.udsendelser));
    }
  }

  public int findUdsendelseIndexFraSlug(String slug) {
    int n = -1;
    if (udsendelser != null) {
      for (int i = 0; i < udsendelser.size(); i++) {
        if (slug.equals(udsendelser.get(i).slug)) n = i;
      }
    }
    return n;
  }
}
