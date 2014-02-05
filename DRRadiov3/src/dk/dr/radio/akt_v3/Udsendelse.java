package dk.dr.radio.akt_v3;

import org.json.JSONObject;

import java.util.Date;

/**
 * Repræsenterer en udsendelse
 * Created by j on 28-01-14.
 */
public class Udsendelse {
  public JSONObject json;
  public String slug;
  public String programserieSlug;
  public String urn;
  public Date startTid;
  public String titel;
  public String startTidKl;
  public String beskrivelse;
  public Date slutTid;
  public String slutTidKl;

  @Override
  public String toString() {
    return slug + "/" + startTid + "/" + slutTid;
  }
}
