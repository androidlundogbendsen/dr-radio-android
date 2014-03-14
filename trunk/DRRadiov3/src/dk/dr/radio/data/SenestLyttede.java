package dk.dr.radio.data;

import java.util.ArrayList;

import dk.dr.radio.diverse.App;

/**
 * Created by j on 08-03-14.
 */
public class SenestLyttede {
  private static final String PREF_NØGLE = "senest lyttede";
  private ArrayList<Lydkilde> liste;

  public ArrayList<Lydkilde> getListe() {
    if (liste == null) opretListe();
    return liste;
  }

  private void opretListe() {
    liste = new ArrayList<Lydkilde>();
    String[] linjer = App.prefs.getString(PREF_NØGLE, "").split("\n");
    for (String linje : linjer) {
      if (linje.length() == 0) continue;

    }
  }

  public void registrérLytning(Lydkilde lydkilde) {
    getListe();
    liste.remove(lydkilde);
    liste.add(lydkilde);
  }
}
