package dk.dr.radio.diverse.volley;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import dk.dr.radio.diverse.App;

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
    /*
     * DRs serverinfrastruktur caches med Varnish, men det kan tage lang tid for den bagvedliggende
     * serverinfrastruktur at svare.
     */
    setRetryPolicy(new DefaultRetryPolicy(4000, 3, 1.5f)); // Ny instans hver gang, da der ændres i den

    final Cache.Entry response = App.volleyRequestQueue.getCache().get(url);
    if (response == null) return; // Vi har ikke en cachet udgave
    try {
      lytter.cachetVærdi = new String(response.data, HttpHeaderParser.parseCharset(response.responseHeaders));
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    //Log.d("XXXXXXXXXXXXXX Cache.Entry  e=" + response);
    // Kald først fikSvar når forgrundstråden er færdig med hvad den er i gang med
    // - i tilfælde af at en forespørgsel er startet midt under en listeopdatering giver det problemer
    // at opdatere listen omgående, da elementer så kan skifte position (og måske type) midt i det hele
    App.forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        try {
          listener.fikSvar(listener.cachetVærdi, true, false);
        } catch (Exception e) {
          listener.onErrorResponse(new VolleyError(e));
        }
      }
    });
  }


  /**
   * Omdefineret så vi kan aflæse servertiden og korrigere hvis klientens ur ikke passer med serverens
   */
  @Override
  protected Response<String> parseNetworkResponse(NetworkResponse response) {
/*
    Log.d("YYYY servertid " + response.headers.get("Date"));
    Log.d("YYYY servertid " + response.headers.get("Expires"));
    Log.d("YYYY servertid " + response.headers);
*/
    long servertid = HttpHeaderParser.parseDateAsEpoch(response.headers.get("Date"));
    if (servertid > 0) {
      App.sætServerCurrentTimeMillis(servertid);
    }

    return super.parseNetworkResponse(response);
  }

  @Override
  public void cancel() {
    super.cancel();
    lytter.annulleret();
  }
}
