package dk.dr.radio.data;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Netvaerksstatus;

/**
 * En lydkilde der kan spilles af afspilleren
 */
public abstract class Lydkilde {
  public String urn;   // Bemærk - kan være tom!
  public String slug;  // Bemærk - kan være tom!
  public ArrayList<Lydstream> streams;

  private static class Bedst {
    Lydstream bedsteLydstream;
    int bedsteScore = Integer.MIN_VALUE;

    void tjekBedst(Lydstream s, int score) {
      if (bedsteScore < score) {
        bedsteScore = score;
        bedsteLydstream = s;
      }
    }
  }

  public Lydstream findBedsteStream(boolean tilHentning) {
    return findBedsteStreams(tilHentning).get(0);
  }


  public List<Lydstream> findBedsteStreams(boolean tilHentning) {
    //Bedst bedst = new Bedst();
    String ønsketkvalitet = App.prefs.getString("lydkvalitet", "auto");
    String ønsketformat = App.prefs.getString("lydformat", "auto");

    ArrayList<Lydstream> kandidater = new ArrayList<Lydstream>(streams.size());

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
            break; // bryd ud af switch
          case RTSP:
            score -= 10; // RTSP udfases og har en enorm ventetid, foretræk andre
            if (!tilHentning) score -= 20; // ... og har en enorm ventetid, foretræk andre
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
        s.score = score;
        Log.d("findBedsteStreams " + s);
        kandidater.add(s);
      } catch (Exception e) {
        Log.rapporterFejl(new Exception(this + " ls=" + sxxx, e));
      }

    Collections.sort(kandidater);
    return kandidater;
  }

  public abstract Kanal kanal();

  public abstract boolean erStreaming();

  public abstract Udsendelse getUdsendelse();
}
