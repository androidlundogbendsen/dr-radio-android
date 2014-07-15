package dk.dr.radio.afspilning;

import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Diverse;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by json on 09-07-14.
 */
class GemiusStatistik {

  static final String RAPPORTERINGSURL = "http://www.dr.dk/mu-online/api/1.0/reporting/gemius";


  private static SimpleDateFormat servertidsformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssSSSZ"); // "2014-07-09T09:54:32.086603Z" +01:00 springes over da kolon i +01:00 er ikke-standard Java
  private JSONObject json;
  private String sporingsnøgle;

  private ArrayList hændelser = new ArrayList();
  private Lydkilde lydkilde;

  GemiusStatistik() {
    String påkrævedeFelter =
        "{\"ScreenResolution\":{\"X\":1024,\"Y\":768}"
            +",\"VideoResolution\":{\"X\":1024,\"Y\":768}"
            +",\"ScreenColorDepth\":24"
            +",\"PlayerEvents\":[{\"MaterialOffsetSeconds\":0,\"Started\":\"FirstPlay\",\"Created\":\"2014-07-09T09:54:32.086603Z\"}]"
            +",\"ChannelType\":\"RADIO\""
            +",\"Testmode\":\"true\""
//            +",\"Testmode\":"+!App.PRODUKTION
            +"}";
    try {
      json = new JSONObject(påkrævedeFelter);
      json.put("AutoStarted", false);
      json.put("Platform", "dr.android." + App.versionsnavn);
      json.put("Telefonmodel", Build.MODEL + " " + Build.PRODUCT);
      json.put("Android_v", Build.VERSION.RELEASE);
/* Behøves, men har ingen meningsfulde værdier
    json.put("ScreenResolution", );
    json.put("ScreenResolution = new Resolution(1024, 768),
    json.put("VideoResolution = new Resolution(1024, 768),
    json.put("ScreenColorDepth = 24
*/
      if (App.prefs!=null) {
        sporingsnøgle = App.prefs.getString("Gemius sporingsnøgle", null);
        json.put("CorrelationId", sporingsnøgle);
        Display display = ((WindowManager) App.instans.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();  // deprecated
        int height = display.getHeight();  // deprecated
        json.put("ScreenResolution", new JSONObject().put("X", width).put("Y", height));
      }
    } catch (JSONException e) {
      Log.rapporterFejl(e);
      json = new JSONObject();
    }
  }



  void testSetlydkilde() throws JSONException {
    hændelser.clear();
    setLydkilde(null);

    registérHændelse(PlayerAction.FirstPlay, 0);
  }

  void setLydkilde(Lydkilde nyLydkilde) {
    Log.d("Gemius setLydkilde "+ lydkilde);
    if (hændelser.size()>0) {
      Log.d("Gemius setLydkilde, hov, havde ikke sendt disse hændelser: "+ hændelser);
      startSendData();
    }
    lydkilde = nyLydkilde;
    if (lydkilde != null) {
      registérHændelse(PlayerAction.FirstPlay, 0);
    }
  }



  void startSendData() {
    Log.d("Gemius startSendData json="+ json);
    try {
      // json.put("Url", "http://test.com");  // behøves ikke?
      // json.put("InitialLoadTime", 0);  // behøves ikke?
      // json.put("TimezoneOffsetInMinutes", -120);  // behøves ikke?
      if (lydkilde!=null) {
        json.put("Id", lydkilde.getUdsendelse().slug);
        json.put("Channel", lydkilde.getKanal().slug);
        json.put("IsLiveStream", lydkilde.erDirekte());
      } else {
        json.put("Id", "matador-24-24");// kan ikke være "ukendt"
        json.put("Channel", "ukendt");
        json.put("IsLiveStream", false);
      }
      json.put("PlayerEvents", new JSONArray(hændelser));
    } catch (JSONException e) {
      Log.rapporterFejl(e);
    }
    hændelser.clear();
    final String data = json.toString();

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          JSONObject res = Diverse.postJson(RAPPORTERINGSURL, data);
          String nySporingsnøgle = res.optString("CorrelationId");
          if (nySporingsnøgle.length()>0 && !nySporingsnøgle.equals(sporingsnøgle)) {
            json.put("CorrelationId", nySporingsnøgle);
            sporingsnøgle = nySporingsnøgle;
            if (App.prefs!=null) App.prefs.edit().putString("Gemius sporingsnøgle", sporingsnøgle).commit();
          }
          Log.d("Gemius res="+res);
        } catch (Exception e) {
          Log.rapporterFejl(e);
        }
      }
    }).start();
  }



  static enum PlayerAction
  {
    FirstPlay,
    Play,
    Pause,
    Seeking,
    Completed,
    Quit,
    Stopped
  }

  void registérHændelse(PlayerAction hvad, long mediaOffsetISekunder) {
    try {
      JSONObject hændelse = new JSONObject();
      hændelse.put("Started", hvad.toString());
      hændelse.put("Created", servertidsformat.format(new Date()) ); // "2014-07-09T09:54:32.086603Z"
      hændelse.put("MaterialOffsetSeconds", mediaOffsetISekunder);
      Log.d("Gemius registérHændelse "+ hændelse);
      hændelser.add(hændelse);
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
  }


  /**
   * Til afprøvning
   */
  public static void main(String[] a) throws Exception {
    GemiusStatistik i = new GemiusStatistik();
    i.testSetlydkilde();
    i.registérHændelse(PlayerAction.Play, 0);
    i.startSendData();

  }

}
