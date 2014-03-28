package dk.dr.radio.diverse.volley;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

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
  String cachetVærdi;

  public DrVolleyResonseListener() {
    App.sætErIGang(true);
  }

  @Override
  public final void onResponse(String response) {
    App.sætErIGang(false);
    try {
      boolean uændret = response != null && response.equals(cachetVærdi);
      fikSvar(response, false, uændret);
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
   * @param fraCache Normalt true første gang hvis svaret kommer fra cachen (og eventuelt er forældet).
   *                 Normalt false anden gang hvor svaret kommer fra serveren.
   * @param uændret Serveren svarede med de samme data som der var i cachen
   * @throws Exception Hvis noget går galt i behandlingen - f.eks. ulovligt JSON kaldes fikFejl
   */
  protected abstract void fikSvar(String response, boolean fraCache, boolean uændret) throws Exception;

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
  void annulleret() {
    App.sætErIGang(false);
  }
}
