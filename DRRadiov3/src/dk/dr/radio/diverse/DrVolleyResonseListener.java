package dk.dr.radio.diverse;

import com.android.volley.Response;
import com.android.volley.VolleyError;

/**
 * Created by j on 13-03-14.
 */
public abstract class DrVolleyResonseListener implements Response.Listener<String>, Response.ErrorListener {

  public DrVolleyResonseListener() {
    App.sætErIGang(true);
  }

  @Override
  public final void onResponse(String response) {
    App.sætErIGang(false);
    try {
      fikSvar(response, false);
    } catch (Exception e) {
      Log.e(e);
      onErrorResponse(new VolleyError(e));
    }
  }

  @Override
  public final void onErrorResponse(VolleyError error) {
    App.sætErIGang(false);
    fikFejl(error);
  }

  protected abstract void fikFejl(VolleyError error);

  protected abstract void fikSvar(String response, boolean fraCache) throws Exception;
}
