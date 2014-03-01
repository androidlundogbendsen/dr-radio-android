package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Date;

/**
 * Repræsenterer en udsendelse
 * Created by j on 28-01-14.
 */
public class Udsendelse extends Lydkilde {

  public String titel;
  public String beskrivelse;
  public String kanalSlug;  // Bemærk - kan være tom!
  public String programserieSlug;  // Bemærk - kan være tom!

  public Date startTid;
  public String startTidKl;
  public Date slutTid;
  public String slutTidKl;

  public ArrayList<Playlisteelement> playliste;

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


  @Override
  public Kanal kanal() {
    return DRData.instans.stamdata.kanalFraSlug.get(kanalSlug);
  }

  @Override
  public boolean erStreaming() {
    return false;
  }

  @Override
  public Udsendelse getUdsendelse() {
    return this;
  }
}
