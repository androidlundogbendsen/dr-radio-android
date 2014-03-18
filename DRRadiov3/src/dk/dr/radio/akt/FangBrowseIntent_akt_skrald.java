package dk.dr.radio.akt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

public class FangBrowseIntent_akt_skrald extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Tjek om vi er blevet startet med et Intent med en URL, f.eks. som
    // new Intent(Intent.ACTION_VIEW, Uri.parse("http://javabog.dk/OOP/kapitel3.jsp"));

    Intent i = getIntent();
    String urlFraIntent = i.getDataString();

    if (urlFraIntent == null) {
      TextView tv = new TextView(this);
      tv.setText("Dette eksempel viser hvordan man fanger et browserintent. ");
      Linkify.addLinks(tv, Linkify.WEB_URLS);
      setContentView(tv);
    } else {
      // Ok, der var en URL med i intentet
      Toast.makeText(this, " viser\n" + urlFraIntent, Toast.LENGTH_LONG).show();
      Toast.makeText(this, "Intent var\n" + i, Toast.LENGTH_LONG).show();

      WebView webView = new WebView(this);
      webView.loadUrl(urlFraIntent);

      setContentView(webView);
    }
  }
}
