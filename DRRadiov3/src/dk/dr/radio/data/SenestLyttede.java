package dk.dr.radio.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Serialisering;

/**
 * Created by j on 08-03-14.
 */
public class SenestLyttede {
  private ArrayList<Lydkilde> liste;

  private String FILNAVN = App.instans.getFilesDir()+"/SenestLyttede.ser";

  private void tjekDataOprettet() {
    if (liste!=null) return;
    if (new File(FILNAVN).exists()) try {
      liste = (ArrayList<Lydkilde>) Serialisering.hent(FILNAVN);
      return;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    liste = new ArrayList<Lydkilde>();
  }

  private Runnable gemListe = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(gemListe);
      try {
        long tid = System.currentTimeMillis();
        Serialisering.gem(liste, FILNAVN);
        Log.d("SenestLyttede: Gemning tog "+(System.currentTimeMillis()-tid)+" ms - filstr:" + new File(FILNAVN).length());
      } catch (IOException e) {
        Log.rapporterFejl(e);
      }
    }
  };


  public ArrayList<Lydkilde> getListe() {
    tjekDataOprettet();
    return liste;
  }

  public void registrérLytning(Lydkilde lydkilde) {
    tjekDataOprettet();
    liste.remove(lydkilde);
    liste.add(lydkilde);
    if (liste.size()>20) liste.remove(0); // Husk kun de seneste 20
    App.forgrundstråd.removeCallbacks(gemListe);
    App.forgrundstråd.postDelayed(gemListe, 30000); // Gem listen om 30 sekunder
  }
}
