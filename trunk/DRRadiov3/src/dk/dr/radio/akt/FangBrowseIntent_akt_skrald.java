package dk.dr.radio.akt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;

import org.json.JSONObject;

import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;

public class FangBrowseIntent_akt_skrald extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Tjek om vi er blevet startet med et Intent med en URL, f.eks. som
    // new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.lundogbendsen.dk/hej.txt"));

    Intent i = getIntent();
    String urlFraIntent = i.getDataString();

    if (urlFraIntent == null) {
      TextView tv = new TextView(this);
      tv.setText("Dette eksempel viser hvordan man fanger et browserintent. ");
      Linkify.addLinks(tv, Linkify.WEB_URLS);
      setContentView(tv);
    } else {
      // Ok, der var en URL med i intentet
      Log.d(" viser " + urlFraIntent);
      Log.d("Intent var " + i);
      WebView webView = new WebView(this);
      webView.loadUrl(urlFraIntent);
      setContentView(webView);

      String[] bidder = urlFraIntent.split("/");

      if (urlFraIntent.contains("/radio/ondemand")) hentOgVisUdsendelse(bidder);
      if (urlFraIntent.contains("/radio/live")) {
        final String kanalSlug = bidder[bidder.length - 1];
        Kanal kanal = DRData.instans.grunddata.kanalFraSlug.get(kanalSlug);
        Intent intent = new Intent(this, VisFragment_akt.class)
            .putExtra(Kanal_frag.P_kode, kanal.kode)
            .putExtra(VisFragment_akt.KLASSE, Kanal_frag.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

      }
    }
  }

  private void hentOgVisUdsendelse(String[] bidder) {
    final String kanalSlug = bidder[bidder.length - 2];
    final String udsendelseSlug = bidder[bidder.length - 1];


    final Udsendelse udsendelse = DRData.instans.udsendelseFraSlug.get(udsendelseSlug);
    if (udsendelse != null) {
      visUdsendelseFrag(kanalSlug, udsendelseSlug);
    } else {
      Request<?> req = new DrVolleyStringRequest("http://www.dr.dk/tjenester/mu-apps/program/" + udsendelseSlug + "?type=radio&includeStreams=true", new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (uændret) return;
          Log.d("hentStreams fikSvar(" + fraCache + " " + url);
          if (json != null && !"null".equals(json)) {
            JSONObject o = new JSONObject(json);
            Udsendelse udsendelse2 = DRJson.parseUdsendelseForProgramseriexx(null, DRData.instans, o);
            udsendelse2.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
            udsendelse2.indslag = DRJson.parsIndslag(o.optJSONArray(DRJson.Chapters.name()));
            udsendelse2.kanStreames = udsendelse2.findBedsteStreams(false).size() > 0;
            udsendelse2.kanHentes = udsendelse2.findBedsteStreams(true).size() > 0;
            udsendelse2.kanNokHøres = udsendelse2.kanStreames;
            udsendelse2.produktionsnummer = o.optString(DRJson.ProductionNumber.name());
            udsendelse2.shareLink = o.optString(DRJson.ShareLink.name());

            visUdsendelseFrag(kanalSlug, udsendelseSlug);

          }
        }
      }).setTag(this);
      App.volleyRequestQueue.add(req);
    }
  }

  private void visUdsendelseFrag(String kanalSlug, String udsendelseSlug) {
    Intent intent = new Intent(this, VisFragment_akt.class)
        .putExtra(Kanal_frag.P_kode, kanalSlug)
        .putExtra(DRJson.Slug.name(), udsendelseSlug)
        .putExtra(VisFragment_akt.KLASSE, Udsendelse_frag.class.getName());
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
  }
}
