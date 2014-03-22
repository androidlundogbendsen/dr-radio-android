package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.diverse.Log;

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

  public transient ArrayList<Playlisteelement> playliste;
  public boolean kanHøres; // om der er nogle streams eller ej

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


  public String getProgramserieUrl() {
    // svarer til v3_programserie.json
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true
    // http://www.dr.dk/tjenester/mu-apps/series/monte-carlo?type=radio&includePrograms=true&includeStreams=true

    return "http://www.dr.dk/tjenester/mu-apps/series/" + programserieSlug + "?type=radio&includePrograms=true";
  }

  public String getStreamsUrl() {
    // http://www.dr.dk/tjenester/mu-apps/program?urn=urn:dr:mu:programcard:52e6fa58a11f9d1588de9c49&includeStreams=true
    return "http://www.dr.dk/tjenester/mu-apps/program?includeStreams=true&urn=" + urn;
  }


  @Override
  public Kanal getKanal() {
    Kanal k = DRData.instans.grunddata.kanalFraSlug.get(kanalSlug);
    if (k == null) {
      Log.rapporterFejl(new Exception(kanalSlug + " manglede i grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug));
      k = DRData.instans.grunddata.ukendtKanal;
    }
    return k;
  }

  @Override
  public boolean erKanal() {
    return false;
  }

  @Override
  public Udsendelse getUdsendelse() {
    return this;
  }
}
