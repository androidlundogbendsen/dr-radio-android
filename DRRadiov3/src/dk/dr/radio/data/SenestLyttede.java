package dk.dr.radio.data;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Serialisering;

/**
 * Created by j on 08-03-14.
 */
public class SenestLyttede {
  public static class SenestLyttet implements Serializable {
    private static final long serialVersionUID = 1L;
    public Lydkilde lydkilde;
    public Date tidpunkt;
    public int positionMs;

    public String toString() {
      return tidpunkt + " / " + positionMs;
    }
  }

  private LinkedHashMap<String, SenestLyttet> liste;

  private String FILNAVN = App.instans == null ? null : App.instans.getFilesDir() + "/SenestLyttede.ser";

  private void tjekDataOprettet() {
    if (liste != null) return;
    if (new File(FILNAVN).exists()) try {
      liste = (LinkedHashMap<String, SenestLyttet>) Serialisering.hent(FILNAVN);
      return;
    } catch (ClassCastException e) {
      Log.d("SenestLyttede: " + e);
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    liste = new LinkedHashMap<String, SenestLyttet>();
  }

  private Runnable gemListe = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(gemListe);
      try {
        long tid = System.currentTimeMillis();
        Serialisering.gem(liste, FILNAVN);
        if (!App.PRODUKTION) Log.d("SenestLyttede: " + liste);
        Log.d("SenestLyttede: Gemning tog " + (System.currentTimeMillis() - tid) + " ms - filstr:" + new File(FILNAVN).length());
      } catch (IOException e) {
        Log.rapporterFejl(e);
      }
    }
  };


  public java.util.Collection<SenestLyttet> getListe() {
    tjekDataOprettet();
    return liste.values();
  }

  public void registrérLytning(Lydkilde lydkilde) {
    tjekDataOprettet();
    SenestLyttet senestLyttet = liste.remove(lydkilde.slug);
    if (senestLyttet == null) senestLyttet = new SenestLyttet();
    senestLyttet.lydkilde = lydkilde;
    senestLyttet.tidpunkt = new Date(App.serverCurrentTimeMillis());
    liste.put(lydkilde.slug, senestLyttet);
    if (liste.size() > 50) liste.remove(0); // Husk kun de seneste 50
    App.forgrundstråd.removeCallbacks(gemListe);
    App.forgrundstråd.postDelayed(gemListe, 10000); // Gem listen om 10 sekunder
  }

  public void sætStartposition(Lydkilde lydkilde, int pos) {
    try {
      liste.get(lydkilde.slug).positionMs = pos;
    } catch (Exception e) {
      Log.rapporterFejl(e, lydkilde);
    }
    App.forgrundstråd.removeCallbacks(gemListe);
    App.forgrundstråd.postDelayed(gemListe, 10000); // Gem listen om 10 sekunder
  }

  public int getStartposition(Lydkilde lydkilde) {
    try {
      return liste.get(lydkilde.slug).positionMs;
    } catch (Exception e) {
      Log.rapporterFejl(e, lydkilde);
    }
    return 0;
  }

}
