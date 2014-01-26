package dk.dr.radio.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Navne for formater der er i DRs JSON-feeds
 * Created by j on 19-01-14.
 */
public enum DrJson {
  /*
     {
        "Urn" : "urn:dr:mu:programcard:52cc9ac66187a213f89ade03",
        "Surround" : false,
        "HasLatest" : true,
        "Title" : "P3 med Mathias Buch Jensen",
        "SeriesSlug" : "p3-med-mathias-buch-jensen",
        "Rerun" : false,
        "FirstPartOid" : 245273145813,
        "StartTime" : "2014-01-16T14:04:00+01:00",
        "Slug" : "p3-med-mathias-buch-jensen-14",
        "TransmissionOid" : 245273144812,
        "Watchable" : false,
        "Widescreen" : false,
        "HD" : false,
        "EndTime" : "2014-01-16T15:04:00+01:00",
        "Episode" : 0,
        "Description" : ""
     },

     */
  Slug,       // unik ID for en udsendelse eller kanal
  SeriesSlug, // unik ID for en programserie
  Urn,        // en anden slags unik ID
  Title, Description,
  StartTime, EndTime,;


  /**
   * parser der kan forstå DRs tidformat
   */
  public static final DateFormat servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // +01:00 springes over da kolon i +01:00 er ikke-standard

  public static void main(String[] a) throws ParseException {
    System.out.println(servertidsformat.format(new Date()));
    System.out.println(servertidsformat.parse("2014-01-16T09:04:00+01:00"));
  }


}
