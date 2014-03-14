package dk.dr.radio.data;

import com.android.volley.Request;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.DrVolleyResonseListener;
import dk.dr.radio.diverse.DrVolleyStringRequest;
import dk.dr.radio.diverse.Log;

/**
 * Håndtering af favoritter.
 * Created by j on 08-03-14.
 */
public class Favoritter {
  private static final String PREF_NØGLE = "favorit til startnummer";
  private HashMap<String, Integer> favoritTilStartnummer;
  private int antalNyeUdsendelser;
  public List<Runnable> observatører = new ArrayList<Runnable>();

  private void tjekDataOprettet() {
    if (favoritTilStartnummer != null) return;
    favoritTilStartnummer = new HashMap<String, Integer>();
    String str = App.prefs.getString(PREF_NØGLE, "");
    Log.d("Favoritter: læst " + str);
    for (String linje : str.split(",")) {
      if (linje.length() == 0) continue;
      String[] d = linje.split(" ");
      favoritTilStartnummer.put(d[0], Integer.parseInt(d[1]));
    }
  }

  public void sætFavorit(String programserieSlug, int startFraNummer, boolean checked) {
    tjekDataOprettet();
    if (checked) favoritTilStartnummer.put(programserieSlug, startFraNummer);
    else favoritTilStartnummer.remove(programserieSlug);
    gem();
    for (Runnable r : observatører) r.run(); // Informér observatører
  }

  private void gem() {
    StringBuilder sb = new StringBuilder(favoritTilStartnummer.size() * 64);
    for (Map.Entry<String, Integer> e : favoritTilStartnummer.entrySet()) {
      sb.append(',').append(e.getKey()).append(' ').append(e.getValue());
    }
    String str = sb.toString();
    Log.d("Favoritter: gemmer " + str);
    App.prefs.edit().putString(PREF_NØGLE, str).commit();
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
    Integer startFraNummer = favoritTilStartnummer.get(programserieSlug);
    Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
    if (startFraNummer == null) return -1;
    if (programserie == null) return -2;
    return programserie.antalUdsendelser - startFraNummer;
  }

  /**
   * Giver antallet af nye udsendelser
   *
   * @param programserieSlug
   * @return antallet, eller -1 hvis det ikke er en favorit, -2 hvis data for aktuelle antal udsendelser mangler
   */
  public Integer getStartFraUdsendelsesNummer(String programserieSlug) {
    tjekDataOprettet();
    return favoritTilStartnummer.get(programserieSlug);
  }


  public void startOpdaterAntalNyeUdsendelser() {
    tjekDataOprettet();
    for (Map.Entry<String, Integer> e : favoritTilStartnummer.entrySet()) {
      final String programserieSlug = e.getKey();
      final Integer startFraNummer = e.getValue();
      int offset = 0;
      String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
      Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache) throws Exception {
          Log.d("favoritter fikSvar(" + fraCache + " " + url);
          Programserie programserie = null;
          if (json != null && !"null".equals(json)) try {
            JSONObject data = new JSONObject(json);
            programserie = DRJson.parsProgramserie(data);
            programserie.udsendelser = DRJson.parseUdsendelserForProgramserie(data.getJSONArray(DRJson.Programs.name()), DRData.instans);
            DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
          } catch (Exception ex) {
            Log.d("Parsefejl: " + ex + " for json=" + json);
            ex.printStackTrace();
          }
          beregnAntalNyeUdsendelser();
        }
      }) {
        public Priority getPriority() {
          return Priority.LOW;
        }
      };
      App.volleyRequestQueue.add(req);
    }
  }

  private void beregnAntalNyeUdsendelser() {
    int antalNyeIAlt = 0;
    for (Map.Entry<String, Integer> e : favoritTilStartnummer.entrySet()) {
      final String programserieSlug = e.getKey();
      final Integer startFraNummer = e.getValue();
      Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
      if (programserie != null) antalNyeIAlt += programserie.antalUdsendelser - startFraNummer;
      else return; // Mangler info - vent med at opdatere antalNyeUdsendelser
    }
    antalNyeUdsendelser = antalNyeIAlt;
    for (Runnable r : observatører) r.run();  // Informér observatører - i forgrundstråden
  }

  public Set<String> getProgramserieSlugSæt() {
    return favoritTilStartnummer.keySet();
  }

  public int getAntalNyeUdsendelser() {
    return antalNyeUdsendelser;
  }
}
