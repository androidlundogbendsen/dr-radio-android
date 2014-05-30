package dk.dr.radio.data;

import com.android.volley.Request;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;

/**
 * Håndtering af favoritter.
 * Created by j on 08-03-14.
 */
public class Favoritter_gammel {
  private static final String PREF_NØGLE = "favorit til startnummer";
  private HashMap<String, String> favoritTilStartnummer;
  private int antalNyeUdsendelser = -1;
  public List<Runnable> observatører = new ArrayList<Runnable>();


  private void tjekDataOprettet() {
    if (favoritTilStartnummer != null) return;
    String str = App.prefs.getString(PREF_NØGLE, "");
    Log.d("Favoritter: læst " + str);
    favoritTilStartnummer = strengTilMap(str);
    if (favoritTilStartnummer.isEmpty()) antalNyeUdsendelser = 0;
  }


  private void gem() {
    String str = mapTilStreng(favoritTilStartnummer);
    Log.d("Favoritter: gemmer " + str);
    App.prefs.edit().putString(PREF_NØGLE, str).commit();
  }

  public void sætFavorit(String programserieSlug, int startFraNummer, boolean checked) {
    tjekDataOprettet();
    if (checked) favoritTilStartnummer.put(programserieSlug, "" + startFraNummer);
    else favoritTilStartnummer.remove(programserieSlug);
    gem();
    for (Runnable r : observatører) r.run(); // Informér observatører
  }

  public boolean erFavorit(String programserieSlug) {
    tjekDataOprettet();
    return favoritTilStartnummer.containsKey(programserieSlug);
  }

  /**
   * Giver antallet af nye udsendelser
   *
   * @param programserieSlug
   * @return antallet, eller -1 hvis det ikke er en favorit, -2 hvis data for aktuelle antal udsendelser mangler
   */
  public int getAntalNyeUdsendelser(String programserieSlug) {
    tjekDataOprettet();
    String startFraNummer = favoritTilStartnummer.get(programserieSlug);
    Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
    if (startFraNummer == null) return -1;
    if (programserie == null) return -2;
    int antalNye = programserie.antalUdsendelser - Integer.parseInt(startFraNummer);
    if (antalNye < 0) {
      Log.rapporterFejl(new IllegalStateException("antalNye=" + antalNye + " for " + programserieSlug));
      favoritTilStartnummer.put(programserieSlug, "" + programserie.antalUdsendelser);
      gem();
    }
    return antalNye;
  }


  public Runnable startOpdaterAntalNyeUdsendelser = new Runnable() {
    @Override
    public void run() {
      tjekDataOprettet();
      Log.d("Favoritter: Opdaterer favoritTilStartnummer=" + favoritTilStartnummer);
      for (Map.Entry<String, String> e : favoritTilStartnummer.entrySet()) {
        final String programserieSlug = e.getKey();
        Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
        if (programserie != null) continue; // Allerede hentet
        final int offset = 0;
        String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
        Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
          @Override
          public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
            Log.d("favoritter fikSvar(" + fraCache + " " + url);
            if (!uændret) {
              if (json != null && !"null".equals(json)) {
                JSONObject data = new JSONObject(json);
                Programserie programserie = DRJson.parsProgramserie(data, null);
                DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
                programserie.tilføjUdsendelser(offset, DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), null, DRData.instans));
              }
            }
            App.forgrundstråd.postDelayed(beregnAntalNyeUdsendelser, 500); // Vent 1/2 sekund på eventuelt andre svar
          }
        }) {
          public Priority getPriority() {
            return Priority.LOW;
          }
        };
        App.volleyRequestQueue.add(req);
      }
    }
  };

  private Runnable beregnAntalNyeUdsendelser = new Runnable() {
    @Override
    public void run() {
      App.forgrundstråd.removeCallbacks(this);
      int antalNyeIAlt = 0;
      for (Map.Entry<String, String> e : favoritTilStartnummer.entrySet()) {
        final String programserieSlug = e.getKey();
        final String startFraNummer = e.getValue();
        Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
        if (programserie != null) {
          int nye = programserie.antalUdsendelser - Integer.parseInt(startFraNummer);
          if (nye < 0) {
            Log.rapporterFejl(new IllegalStateException("Antal nye favoritter=" + nye), " for " + programserieSlug);
            e.setValue("" + programserie.antalUdsendelser);
            gem();
            continue;
          }
          antalNyeIAlt += nye;
          Log.d("Favoritter: " + programserie + " har " + nye + ", antalNyeIAlt=" + antalNyeIAlt);
        }
      }
      if (antalNyeUdsendelser != antalNyeIAlt) {
        Log.d("Favoritter: Ny favoritTilStartnummer=" + favoritTilStartnummer);
        Log.d("Favoritter: Fortæller observatører at antalNyeUdsendelser er ændret fra " + antalNyeUdsendelser + " til " + antalNyeIAlt);
        antalNyeUdsendelser = antalNyeIAlt;
        for (Runnable r : observatører) r.run();  // Informér observatører - i forgrundstråden
      }
    }
  };

  public Set<String> getProgramserieSlugSæt() {
    tjekDataOprettet();
    return favoritTilStartnummer.keySet();
  }

  public int getAntalNyeUdsendelser() {
    tjekDataOprettet();
    return antalNyeUdsendelser;
  }


  public static HashMap<String, String> strengTilMap(String str) {
    HashMap<String, String> map = new HashMap<String, String>();
    for (String linje : str.split(",")) {
      if (linje.length() == 0) continue;
      String[] d = linje.split(" ");
      map.put(d[0], d[1]);
    }
    return map;
  }

  public static String mapTilStreng(HashMap<String, String> map) {
    StringBuilder sb = new StringBuilder(map.size() * 64);
    for (Map.Entry<String, String> e : map.entrySet()) {
      sb.append(',').append(e.getKey()).append(' ').append(e.getValue());
    }
    return sb.toString();
  }
}
