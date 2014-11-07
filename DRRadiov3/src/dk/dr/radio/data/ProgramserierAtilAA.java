package dk.dr.radio.data;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;

/**
 * Created by j on 05-10-14.
 */
public class ProgramserierAtilAA {
  public ArrayList<Programserie> liste;
  public List<Runnable> observatører = new ArrayList<Runnable>();



  public void startHentData() {
    Request<?> req = new DrVolleyStringRequest(DRData.getAtilÅUrl(), new DrVolleyResonseListener() {
      @Override
      public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
        if (uændret) return;
        JSONArray jsonArray = new JSONArray(json);
        ArrayList<Programserie> res = new ArrayList<Programserie>();
        for (int n = 0; n < jsonArray.length(); n++) {
          JSONObject programserieJson = jsonArray.getJSONObject(n);
          String programserieSlug = programserieJson.getString(DRJson.Slug.name());
          Log.d("\n=========================================== programserieSlug = " + programserieSlug);
          Programserie programserie = DRData.instans.programserieFraSlug.get(programserieSlug);
          if (programserie == null) {
            programserie = new Programserie();
            DRData.instans.programserieFraSlug.put(programserieSlug, programserie);
          }
          res.add(DRJson.parsProgramserie(programserieJson, programserie));
        }
        Log.d(" res=" + res);
        DRData.instans.programserierAtilÅ.liste = res;
        for (Runnable r : observatører) r.run(); // Informér observatører
      }
    }) {
      public Priority getPriority() {
        return Priority.LOW;
      }
    };
    App.volleyRequestQueue.add(req);
  }
}