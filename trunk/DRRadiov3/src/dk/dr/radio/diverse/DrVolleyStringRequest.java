package dk.dr.radio.diverse;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

/**
 * Created by j on 13-03-14.
 */
public class DrVolleyStringRequest extends StringRequest {
  private final DrVolleyResonseListener lytter;

  /*
      public DrVolleyStringRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
      }
      */
  public DrVolleyStringRequest(String url, final DrVolleyResonseListener listener) {
    super(url, listener, listener);
    listener.url = url;
    lytter = listener;
    final Cache.Entry response = App.volleyRequestQueue.getCache().get(url);
    if (response == null) return; // Vi har ikke en cachet udgave
    // Kald først fikSvar når forgrundstråden er færdig med hvad den er i gang med
    // - i tilfælde af at en forespørgsel er startet midt under en listeopdatering giver det problemer
    // at opdatere listen omgående, da elementer så kan skifte position (og måske type) midt i det hele
    App.forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        try {
          String json = new String(response.data, HttpHeaderParser.parseCharset(response.responseHeaders));
          Log.d("XXXXXXXXXXXXXX Cache.Entry  e=" + response);
          listener.fikSvar(json, true);
        } catch (Exception e) {
          Log.rapporterFejl(e);
          listener.onErrorResponse(new VolleyError(e));
        }
      }
    });
    setRetryPolicy(new DefaultRetryPolicy(3 * 1000, 3, 1.5f));
  }

  /**
   * I fald telefonens ur går forkert kan det ses her - alle HTTP-svar bliver jo stemplet med servertiden
   */
  private static long serverkorrektionTilKlienttidMs = 0;


  public static long serverCurrentTimeMillis() {
    return System.currentTimeMillis() + serverkorrektionTilKlienttidMs;
  }


  @Override
  protected Response<String> parseNetworkResponse(NetworkResponse response) {
/*
    Log.d("YYYY servertid " + response.headers.get("Date"));
    Log.d("YYYY servertid " + response.headers.get("Expires"));
    Log.d("YYYY servertid " + response.headers);
*/
    long servertid = HttpHeaderParser.parseDateAsEpoch(response.headers.get("Date"));
    if (servertid > 0) {
      long serverkorrektionTilKlienttidMs2 = servertid - System.currentTimeMillis();
      if (Math.abs(serverkorrektionTilKlienttidMs - serverkorrektionTilKlienttidMs2) > 10000) {
        Log.d("SERVERTID korrigerer tid - serverkorrektionTilKlienttidMs=" + serverkorrektionTilKlienttidMs2);
        serverkorrektionTilKlienttidMs = serverkorrektionTilKlienttidMs2;
      }
    }

    return super.parseNetworkResponse(response);
  }

  @Override
  public void cancel() {
    super.cancel();
    lytter.annulleret();
  }
}
