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
  /*
  public DrVolleyStringRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
    super(url, listener, errorListener);
  }
  */
  public DrVolleyStringRequest(String url, DrVolleyResonseListener listener) {
    super(url, listener, listener);
    try {
      Cache.Entry response = App.volleyRequestQueue.getCache().get(url);
      if (response == null) return; // Vi har ikke en cachet udgave
      String json = new String(response.data, HttpHeaderParser.parseCharset(response.responseHeaders));
      Log.d("XXXXXXXXXXXXXX Cache.Entry  e=" + response);
      listener.fikSvar(json, true);
    } catch (Exception e) {
      Log.rapporterFejl(e);
      listener.onErrorResponse(new VolleyError(e));
    }
    setRetryPolicy(new DefaultRetryPolicy(3 * 1000, 3, 1.5f));
  }

  @Override
  protected Response<String> parseNetworkResponse(NetworkResponse response) {
    Log.d("YYYY servertid " + response.headers.get("Date"));
    Log.d("YYYY servertid " + response.headers.get("Expires"));
    Log.d("YYYY servertid " + response.headers);
    return super.parseNetworkResponse(response);
  }
  /*

   */
}
