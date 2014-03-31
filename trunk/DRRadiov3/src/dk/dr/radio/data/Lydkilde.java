package dk.dr.radio.data;

import android.os.Build;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Netvaerksstatus;

/**
 * En lydkilde der kan spilles af afspilleren
 */
public abstract class Lydkilde implements Serializable {
  private static final long serialVersionUID = 1L;

  public String urn;   // Bemærk - kan være tom!
  public String slug;  // Bemærk - kan være tom!
  public transient ArrayList<Lydstream> streams;
  public transient Lydstream hentetStream;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o instanceof Lydkilde && slug != null) return slug.equals(((Lydkilde) o).slug);
    return super.equals(o);
  }

  public Lydstream findBedsteStream(boolean tilHentning) {
    return findBedsteStreams(tilHentning).get(0);
  }

  public void nulstilForetrukkenStream() {
    if (streams == null) return;
    for (Lydstream s : streams) s.foretrukken = false;
  }


  public List<Lydstream> findBedsteStreams(boolean tilHentning) {
    //Bedst bedst = new Bedst();
    String ønsketkvalitet = App.prefs.getString("lydkvalitet", "auto");
    String ønsketformat = App.prefs.getString("lydformat", "auto");

    ArrayList<Lydstream> kandidater = new ArrayList<Lydstream>();
    if (hentetStream != null) kandidater.add(hentetStream);
    if (streams == null) return kandidater;

    Lydstream sxxx = null;
      næste_stream:
    for (Lydstream s : streams)
      try {
        sxxx = s;
        int score = 100;
        switch (s.type) {
          case HLS_fra_Akamai:
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) continue næste_stream;
            if (tilHentning) continue næste_stream;
            if ("hls".equals(ønsketformat)) score += 40;
            if ("auto".equals(ønsketformat)) score += 20;
            break; // bryd ud af switch
          case HTTP:
            if (tilHentning) score += 20;
            if ("shoutcast".equals(ønsketformat)) score += 40;
            break; // bryd ud af switch
          case RTSP:
            if (tilHentning) continue næste_stream;
            score -= 40; // RTSP udfases og har en enorm ventetid, foretræk andre
          case Shoutcast:
            if (tilHentning) continue næste_stream;
            if ("shoutcast".equals(ønsketformat)) score += 40;
            break; // bryd ud af switch
          default:
            continue næste_stream;
        }
        switch (s.kvalitet) {
          case High:
            if ("høj".equals(ønsketkvalitet)) score += 10;
            if ("auto".equals(ønsketkvalitet) && App.netværk.status == Netvaerksstatus.Status.WIFI) score += 10;
            break;
          case Low:
          case Medium:
            if ("standard".equals(ønsketkvalitet)) score += 10;
            if ("auto".equals(ønsketkvalitet) && App.netværk.status == Netvaerksstatus.Status.MOBIL) score += 10;
            break;
          case Variable:
            if ("auto".equals(ønsketkvalitet)) score += 10;
            break;
        }
        if (s.foretrukken) score += 1000;
        if ("mp3".equals(s.format)) score += 10; // mp3 er mere pålideligt end mp4
        s.score = score;
        Log.d("findBedsteStreams " + s);
        kandidater.add(s);
      } catch (Exception e) {
        Log.rapporterFejl(new Exception(this + " ls=" + sxxx, e));
      }

    Collections.sort(kandidater);
    return kandidater;
  }

  public abstract Kanal getKanal();

  public abstract boolean erDirekte();

  public abstract Udsendelse getUdsendelse();
}
