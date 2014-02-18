package dk.dr.radio.data;

import org.json.JSONObject;

import java.util.ArrayList;
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
  public ArrayList<Playlisteelement> playliste;
  public ArrayList<Lydstream> streams;

  public Udsendelse(String s) {
    titel = s;
  }

  public Udsendelse() {
  }

  @Override
  public String toString() {
    return slug;// + "/" + startTid + "/" + slutTid;
  }

  // http://www.dr.dk/tjenester/mu-apps/program/monte-carlo-361


  public String getStreamsUrl() {
    // http://www.dr.dk/tjenester/mu-apps/program?urn=urn:dr:mu:programcard:52e6fa58a11f9d1588de9c49&includeStreams=true
    return "http://www.dr.dk/tjenester/mu-apps/program?includeStreams=true&urn=" + urn;
  }


}
