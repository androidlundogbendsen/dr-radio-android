package dk.dr.radio.data;

import java.util.ArrayList;

/**
 * En lydkilde der kan spilles af afspilleren
 */
public abstract class Lydkilde {
  public String urn;   // Bemærk - kan være tom!
  public String slug;  // Bemærk - kan være tom!
  public ArrayList<Lydstream> streams;

  public Lydstream findBedsteStream() {
    for (Lydstream s : streams) {
      if (s.foretrukken) return s;
    }
    for (Lydstream s : streams) {
      if (s.url.endsWith(".mp3")) return s;
    }
    return streams.get(0);
  }

  public abstract Kanal kanal();

  public abstract boolean erStreaming();

  public abstract Udsendelse getUdsendelse();
}
