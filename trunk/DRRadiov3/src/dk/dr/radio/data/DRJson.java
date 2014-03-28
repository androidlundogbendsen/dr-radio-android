package dk.dr.radio.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.diverse.Log;

/**
 * Navne for felter der er i DRs JSON-feeds og støttefunktioner til at parse dem
 * Created by j on 19-01-14.
 */
public enum DRJson {
  Slug,       // unik ID for en udsendelse eller getKanal
  SeriesSlug, // unik ID for en programserie
  Urn,        // en anden slags unik ID
  Title, Description,
  StartTime, EndTime,
  Streams,
  Uri, Played, Artist, Image,
  Type, Kind, Quality, Kbps, ChannelSlug, TotalPrograms, Programs, FirstBroadcast, Watchable, DurationInSeconds, Format, OffsetMs, ProductionNumber, LatestProgramBroadcasted;

  /*
    public enum StreamType {
      Shoutcast, // 0 tidligere 'Streaming'
      HLS_fra_DRs_servere,  // 1 tidligere 'IOS' - virker p.t. ikke på Android
      RTSP, // 2 tidligere 'Android' - udfases
      HDS, // 3 Adobe  HTTP Dynamic Streaming
      HLS_fra_Akamai, // 4 oprindeligt 'HLS'
      HLS_med_probe_og_fast_understream,
      HLS_byg_selv_m3u8_fil,
      Ukendt;  // = -1 i JSON-svar
      static StreamType[] v = values();
    }
    */
  public enum StreamType {
    Streaming_RTMP, // 0
    HLS_fra_DRs_servere,  // 1 tidligere 'IOS' - virker p.t. ikke på Android
    RTSP, // 2 tidligere 'Android' - udfases
    HDS, // 3 Adobe  HTTP Dynamic Streaming
    HLS_fra_Akamai, // 4 oprindeligt 'HLS' - virker på Android 4
    HTTP, // 5 Til on demand/hentning af lyd
    Shoutcast, // 6 Til Android 2
    Ukendt;  // = -1 i JSON-svar
    static StreamType[] v = values();

  }


  public enum StreamKind {
    Audio,
    Video;
    static StreamKind[] v = values();
  }

  public enum StreamQuality {
    High,     // 0
    Medium,   // 1
    Low,      // 2
    Variable; // 3
    static StreamQuality[] v = values();
  }

  public enum StreamConnection {
    Wifi,
    Mobile;
    static StreamConnection[] v = values();
  }


  /**
   * parser der kan forstå DRs tidformat: "2014-02-13T10:03:00+01:00"
   */
  public static DateFormat servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");

  /**
   * parser der kan forstå DRs tidformat: "2014-02-13T10:03:00"
   */
  public static DateFormat servertidsformat_playlise = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

/*
  public static void main(String[] a) throws ParseException {
    System.out.println(servertidsformat.format(new Date()));
    System.out.println(servertidsformat.parse("2014-01-16T09:04:00+01:00"));
  }
*/

  public static final Locale dansk = new Locale("da", "DA");
  public static final DateFormat klokkenformat = new SimpleDateFormat("HH:mm", dansk);
  public static final DateFormat datoformat = new SimpleDateFormat("d. MMM yyyy", dansk);


  private static Udsendelse getUdsendelse(DRData drData, JSONObject o) throws JSONException {
    String slug = o.optString(DRJson.Slug.name());  // Bemærk - kan være tom!
    Udsendelse u = null; //drData.udsendelseFraSlug.get(slug);
    if (u == null) {
      u = new Udsendelse();
      u.slug = slug;
      drData.udsendelseFraSlug.put(slug, u);
    }
    u.titel = o.getString(DRJson.Title.name());
    u.beskrivelse = o.getString(DRJson.Description.name());
    u.programserieSlug = o.optString(DRJson.SeriesSlug.name());  // Bemærk - kan være tom!
    u.urn = o.optString(DRJson.Urn.name());  // Bemærk - kan være tom!
    return u;
  }

  /**
   * Parser udsendelser for getKanal. A la http://www.dr.dk/tjenester/mu-apps/schedule/P3/0
   * Deduplikerer objekterne undervejs
   */
  public static ArrayList<Udsendelse> parseUdsendelserForKanal(JSONArray jsonArray, Kanal kanal, DRData drData) throws JSONException, ParseException {
    String iDagDatoStr = datoformat.format(new Date());
    String iMorgenDatoStr = datoformat.format(new Date(DrVolleyStringRequest.serverCurrentTimeMillis() + 24 * 60 * 60 * 1000));
    String iGårDatoStr = datoformat.format(new Date(DrVolleyStringRequest.serverCurrentTimeMillis() - 24 * 60 * 60 * 1000));

    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Udsendelse u = getUdsendelse(drData, o);
      u.kanalSlug = o.optString(DRJson.ChannelSlug.name(), kanal.slug);  // Bemærk - kan være tom..
      u.kanHøres = o.getBoolean(DRJson.Watchable.name());
      u.startTid = servertidsformat.parse(o.getString(DRJson.StartTime.name()));
      u.startTidKl = klokkenformat.format(u.startTid);
      u.slutTid = servertidsformat.parse(o.getString(DRJson.EndTime.name()));
      u.slutTidKl = klokkenformat.format(u.slutTid);
      String datoStr = datoformat.format(u.startTid);

      if (datoStr.equals(iDagDatoStr)) ; // ingen ting
      else if (datoStr.equals(iMorgenDatoStr)) u.startTidKl += " - i morgen";
      else if (datoStr.equals(iGårDatoStr)) u.startTidKl += " - i går";
      else u.startTidKl += " - " + datoStr;

      uliste.add(u);
    }
    return uliste;
  }

  /**
   * Parser udsendelser for getKanal. A la http://www.dr.dk/tjenester/mu-apps/series/sprogminuttet?type=radio&includePrograms=true
   * Deduplikerer objekterne undervejs
   */
  public static ArrayList<Udsendelse> parseUdsendelserForProgramserie(JSONArray jsonArray, DRData drData) throws JSONException, ParseException {
    ArrayList<Udsendelse> uliste = new ArrayList<Udsendelse>();
    for (int n = 0; n < jsonArray.length(); n++) {
      JSONObject o = jsonArray.getJSONObject(n);
      Udsendelse u = getUdsendelse(drData, o);
      u.kanalSlug = o.getString(DRJson.ChannelSlug.name());
      u.startTid = servertidsformat.parse(o.getString(DRJson.FirstBroadcast.name()));
      u.slutTid = new Date(u.startTid.getTime() + o.getInt(DRJson.DurationInSeconds.name()) * 1000);
      uliste.add(u);
    }
    return uliste;
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
      u.titel = o.getString(DRJson.Title.name());
      u.kunstner = o.getString(DRJson.Artist.name());
      u.billedeUrl = o.optString(DRJson.Image.name());
      u.startTid = DRJson.servertidsformat_playlise.parse(o.getString(DRJson.Played.name()));
      u.startTidKl = klokkenformat.format(u.startTid);
      u.offsetMs = o.optInt(DRJson.OffsetMs.name(), -1);
      liste.add(u);
    }
    return liste;
  }

  /**
   * Parse en stream.
   * F.eks. Streams-objekt fra
   * http://www.dr.dk/tjenester/mu-apps/channel?urn=urn:dr:mu:bundle:4f3b8926860d9a33ccfdafb9&includeStreams=true
   * http://www.dr.dk/tjenester/mu-apps/program?includeStreams=true&urn=urn:dr:mu:programcard:531520836187a20f086b5bf9
   *
   * @param jsonArray
   * @return
   * @throws JSONException
   */

  public static ArrayList<Lydstream> parsStreams(JSONArray jsonArray) throws JSONException {
    ArrayList<Lydstream> lydData = new ArrayList<Lydstream>();
    for (int n = 0; n < jsonArray.length(); n++)
      try {
        JSONObject o = jsonArray.getJSONObject(n);
        //Log.d("streamjson=" + o.toString());
        Lydstream l = new Lydstream();
        //if (o.getInt("FileSize")!=0) { Log.d("streamjson=" + o.toString(2)); System.exit(0); }
        l.url = o.getString(DRJson.Uri.name());
        if (l.url.startsWith("rtmp:")) continue; // Skip Adobe Real-Time Messaging Protocol til Flash
        int type = o.getInt(Type.name());
        l.type = type < 0 ? StreamType.Ukendt : StreamType.values()[type];
        if (l.type == StreamType.HDS) continue; // Skip Adobe HDS - HTTP Dynamic Streaming
        //if (l.type == StreamType.IOS) continue; // Gamle HLS streams der ikke virker på Android
        if (o.getInt(Kind.name()) != StreamKind.Audio.ordinal()) continue;
        l.kvalitet = StreamQuality.values()[o.getInt(Quality.name())];
        l.format = o.optString(Format.name()); // null for direkte udsendelser
        l.kbps = o.getInt(Kbps.name());
        lydData.add(l);
        if (App.fejlsøgning) Log.d("lydstream=" + l);
      } catch (Exception e) {
        Log.e(e);
      }
    return lydData;
  }

  /*
  {
  Channel: "dr.dk/mas/whatson/channel/P3",
  Webpage: "http://www.dr.dk/p3/programmer/monte-carlo",
  Explicit: true,
  TotalPrograms: 365,
  ChannelType: 0,
  Programs: [],
  Slug: "monte-carlo",
  Urn: "urn:dr:mu:bundle:4f3b8b29860d9a33ccfdb775",
  Title: "Monte Carlo på P3",
  Subtitle: "",
  Description: "Nu kan du dagligt fra 14-16 komme en tur til Monte Carlo, hvor Peter Falktoft og Esben Bjerre vil guide dig rundt. Du kan læne dig tilbage og nyde turen og være på en lytter, når Peter og Esben vender ugens store og små kulturelle begivenheder, kigger på ugens bedste tv og spørger hvad du har #HørtOverHækken. "
  }*/

  /**
   * Parser et Programserie-objekt
   * @param o JSON
   * @param ps et eksisterende objekt, der skal opdateres, eller null
   * @return objektet
   * @throws JSONException
   */
  public static Programserie parsProgramserie(JSONObject o, Programserie ps) throws JSONException {
    if (ps==null) ps = new Programserie();
    ps.titel = o.getString(DRJson.Title.name());
    ps.beskrivelse = o.optString(DRJson.Description.name());
    ps.slug = o.getString(DRJson.Slug.name());
    ps.urn = o.optString(DRJson.Urn.name());
    ps.antalUdsendelser = o.getInt(DRJson.TotalPrograms.name());
    try {
      ps.senesteUdsendelseTid = servertidsformat.parse(o.getString(DRJson.LatestProgramBroadcasted.name()));
    } catch (ParseException e) {
      Log.rapporterFejl(e);
    }
    return ps;
  }

}
