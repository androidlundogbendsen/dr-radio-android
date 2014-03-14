package dk.dr.radio.diverse;

import com.android.volley.Response;
import com.android.volley.VolleyError;

/**
 * DR Radios ResponseListener-klient til Volley.
 * Lavet sådan at også cachede (eventuelt forældede) værdier leveres.
 * Håndterer også at signalere over for brugeren når netværskommunikation er påbegyndt eller afsluttet
 * Created by j on 13-03-14.
 */
public abstract class DrVolleyResonseListener implements Response.Listener<String>, Response.ErrorListener {

  /**
   * URL på anmodningen - rar at have til logning
   */
  protected String url;

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

  /**
   * Kaldes med svaret fra cachen (hvis der er et) og igen når svaret fra serveren ankommer
   *
   * @param response Svaret
   * @param fraCache Normalt true første gang hvis svaret kommer fra cachen og eventuelt forældet - Normalt false anden gang hvor svaret kommer fra serveren.
   * @throws Exception Hvis noget går galt i behandlingen - f.eks. ulovligt JSON kaldes fikFejl
   */
  protected abstract void fikSvar(String response, boolean fraCache) throws Exception;

  /**
   * Kaldes af Volley hvis der skete en netværksfejl. Kaldes også hvis behandlingen i #fikSvar gik galt.
   *
   * @param error Fejlen
   */
  protected void fikFejl(VolleyError error) {
    Log.e("fikFejl for " + url + " " + error.networkResponse, error);
  }

  /**
   * Kaldes (fra DrVolleyStringRequest) hvis forespørgslen blev annulleret
   */
  protected void annulleret() {
    App.sætErIGang(false);
  }
}
