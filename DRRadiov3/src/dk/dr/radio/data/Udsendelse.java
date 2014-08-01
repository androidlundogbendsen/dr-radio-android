package dk.dr.radio.data;

import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.diverse.Log;

/**
 * Repræsenterer en udsendelse
 * Created by j on 28-01-14.
 */
public class Udsendelse extends Lydkilde implements Comparable<Udsendelse> {
  // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/1415558087
  // - at proguard obfuskering havde
  // Se også http://stackoverflow.com/questions/16210831/serialization-deserialization-proguard
  private static final long serialVersionUID = -9161602458987716481L;

  public String titel;
  public String beskrivelse;
  public String kanalSlug;  // Bemærk - kan være tom!
  public String programserieSlug;  // Bemærk - kan være tom!

  public Date startTid;
  public String startTidKl;
  public Date slutTid;
  public String slutTidKl;

  public transient ArrayList<Playlisteelement> playliste;
  public transient ArrayList<Indslaglisteelement> indslag; // 'Chapters' i API'et, undgå undersættelsen 'kapitler'
  public boolean kanNokHøres; // API'ets udmelding på, om der er nogle streams eller ej. Desværre er API'et ikke pålideligt, så den eneste måde reelt at vide det er faktisk at hente streamsne
  public boolean kanStreames; // Efter at streams er hentet, om der er en egnet streams til direkte afspilning
  public boolean kanHentes;   // Efter at streams er hentet, om der er mulighed for at hente udsendelsen ned til offline brug
  public String produktionsnummer;
  public String shareLink;
  public transient int startposition;// hvis der allerede er lyttet til denne lydkilde så notér det her så afspilning kan fortsætte herfra
  public int episodeIProgramserie;

  public Udsendelse(String s) {
    titel = s;
  }

  public Udsendelse() {
  }

  @Override
  public String toString() {
    return slug + "/" + episodeIProgramserie;//startTid + "/" + slutTid;
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


  public String getPlaylisteUrl() {
    // Tidligere (marts 2014) skulle kanalens slug med, såsom
    // http://www.dr.dk/tjenester/mu-apps/playlist/monte-carlo-352/p3
    // Det er tilsyneladende ikke nødvendigt mere, per april 2014
    return "http://www.dr.dk/tjenester/mu-apps/playlist/" + slug + "/0";
  }

  @Override
  public Kanal getKanal() {
    Kanal k = DRData.instans.grunddata.kanalFraSlug.get(kanalSlug);
    if (Kanal.P4kode.equals(k.kode)) {
      Log.rapporterFejl(new IllegalStateException("Vi fik P4 overkanalen - ved ikke hvilken underkanal"), kanalSlug);
      return Grunddata.ukendtKanal;
    }
    if (k == null) {
      Log.rapporterFejl(new Exception(kanalSlug + " manglede i grunddata.kanalFraSlug"), DRData.instans.grunddata.kanalFraSlug.toString());
      return Grunddata.ukendtKanal;
    }
    return k;
  }

  @Override
  public boolean erDirekte() {
    return false;
  }

  @Override
  public Udsendelse getUdsendelse() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Udsendelse)) return false;
    Udsendelse u = (Udsendelse) o;
    if (slug!=null) return slug.equals(u.slug);
    return false;
  }

  @Override
  public int compareTo(Udsendelse u2) {
    int e = episodeIProgramserie;
    int e2 = u2.episodeIProgramserie;
    if (e != e2) return e2 < e ? -1 : 1;
    if (slug==null) return u2.slug==null? 0 : 1;
    return slug.compareTo(u2.slug);
  }

  public boolean streamsKlar() {
    return hentetStream != null || streams != null && streams.size() > 0;
  }
}
