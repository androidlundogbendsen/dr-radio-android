package dk.dr.radio.data;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.dr.radio.diverse.App;
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


  public void opdaterAntalNyeUdsendelserBg() {
    tjekDataOprettet();
    AQuery aq = new AQuery(App.instans);
    int antalNyeIAlt = 0;
    for (Map.Entry<String, Integer> e : favoritTilStartnummer.entrySet()) {
      String programserieSlug = e.getKey();
      Integer startFraNummer = e.getValue();
      int offset = 0;
      String url = "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true&offset=" + offset;
      AjaxCallback<String> cb = new AjaxCallback<String>().url(url).type(String.class).expire(1 * 60 * 60 * 1000);
      aq.sync(cb);
      String json = cb.getResult();
      AjaxStatus status = cb.getStatus();
      Log.d(this + " url " + url + "   status=" + status.getCode());
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
      if (programserie == null) programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
      if (programserie != null) antalNyeIAlt += programserie.antalUdsendelser - startFraNummer;
    }
    antalNyeUdsendelser = antalNyeIAlt;
    for (Runnable r : observatører) App.forgrundstråd.post(r);  // Informér observatører - i forgrundstråden
  }

  public Set<String> getProgramserieSlugSæt() {
    return favoritTilStartnummer.keySet();
  }

  public int getAntalNyeUdsendelser() {
    return antalNyeUdsendelser;
  }
}
