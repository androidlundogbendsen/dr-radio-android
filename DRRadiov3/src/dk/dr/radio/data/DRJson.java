package dk.dr.radio.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.Log;

/**
 * Navne for formater der er i DRs JSON-feeds
 * Created by j on 19-01-14.
 */
public enum DRJson {
  Slug,       // unik ID for en udsendelse eller kanal
  SeriesSlug, // unik ID for en programserie
  Urn,        // en anden slags unik ID
  Title, Description,
  StartTime, EndTime,
  Streams,
  Uri, Played, Artist, Image,
  Type, Kind, Quality, Kbps;  // til streams - se enumsne herunder

  public enum StreamType {
    //Unknown = -1,
    Streaming,
    IOS,
    Android,
    HDS,
    HLS,
    HLS_med_probe_og_fast_understream,
    HLS_byg_selv_m3u8_fil,;
    static StreamType[] v = values();

  }

  public enum StreamKind {
    Audio,
    Video;
    static StreamKind[] v = values();
  }

  public enum StreamQuality {
    High,
    Medium,
    Low,
    Variable;
    static StreamQuality[] v = values();
  }

  public enum StreamConnection {
    Wifi,
    Mobile;
    static StreamConnection[] v = values();
  }


  /**
   * parser der kan forstå DRs tidformat
   */
  public static final DateFormat servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard

  public static void main(String[] a) throws ParseException {
    System.out.println(servertidsformat.format(new Date()));
    System.out.println(servertidsformat.parse("2014-01-16T09:04:00+01:00"));
  }


  /*
  Title: "Back to life",
  Artist: "Soul II Soul",
  DetailId: "2213875-1-1",
  Image: "http://api.discogs.com/image/A-4970-1339439274-8053.jpeg",
  ScaledImage: "http://asset.dr.dk/discoImages/?discoserver=api.discogs.com&file=%2fimage%2fA-4970-1339439274-8053.jpeg&h=400&w=400&scaleafter=crop&quality=85",
  Played: "2014-02-06T15:58:33",
  OffsetMs: 6873000
   */
  public static ArrayList<Playlisteelement> parsePlayliste(JSONArray jsonArray) throws JSONException, ParseException {
    ArrayList<Playlisteelement> liste = new ArrayList<Playlisteelement>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Playlisteelement u = new Playlisteelement();
      u.titel = o.optString(DRJson.Title.name());
      u.kunstner = o.optString(DRJson.Artist.name());
      u.billedeUrl = (String) o.get(DRJson.Image.name());
      u.startTid = DRJson.servertidsformat.parse(o.optString(DRJson.Played.name()));
      u.startTidKl = Kanal.klokkenformat.format(u.startTid);
      liste.add(u);
    }
    return liste;
  }


  public static ArrayList<Lydstream> parsStreams(JSONArray jsonArray) throws JSONException {
    ArrayList<Lydstream> lydData = new ArrayList<Lydstream>();
    for (int n = 0; n < jsonArray.length(); n++)
      try {
        JSONObject o = jsonArray.getJSONObject(n);
        Log.d("streamjson=" + o.toString());
        Lydstream l = new Lydstream();
        l.url = o.getString(DRJson.Uri.name());
        if (l.url.startsWith("rtmp:")) continue; // Adobe Real-Time Messaging Protocol til Flash
        l.type = StreamType.values()[o.getInt(Type.name())];
        l.kind = StreamKind.values()[o.getInt(Kind.name())];
        if (l.type == StreamType.HDS) continue; // Adobe HDS - HTTP Dynamic Streaming
        if (l.type == StreamType.IOS) continue; // Gamle HLS streams der ikke virker på Android
        if (l.kind != StreamKind.Audio) continue;
        l.kvalitet = StreamQuality.values()[o.getInt(Quality.name())];
        lydData.add(l);
        Log.d("lydstream=" + l);
      } catch (Exception e) {
        Log.e(e);
      }
    return lydData;
  }
}
