/**
DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
Some parts of the code are loosely based on Sveriges Radio Play for Android.

DR Radio 2 for Android is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 as published by
the Free Software Foundation.

DR Radio 2 for Android is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

*/

package dk.dr.radio;

import dk.dr.radio.data.DRData;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.widget.ImageButton;
import dk.dr.radio.util.Kontakt;
import dk.dr.radio.util.Log;
import dk.dr.radio.util.MedieafspillerInfo;

public class Om_DRRadio_akt extends Activity implements OnClickListener {
	WebView webview;

	private static final String EMAILSUBJECT = "DR Radio Android App - Feedback";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);

    DRData drData;
    try {
      drData = DRData.tjekInstansIndlæst(this);
    } catch (Exception ex) {
      Log.e(ex);
      finish(); // Hop ud!
      return;
    }
    String aboutUrl = drData.stamdata.s("about_url");

    webview = (WebView) findViewById(R.id.about_webview);

    // Jacob: Fix for 'syg' webview-cache - se http://code.google.com/p/android/issues/detail?id=10789
    WebViewDatabase webViewDB = WebViewDatabase.getInstance(this);
    if (webViewDB!=null) {
      // OK, webviewet kan bruge sin cache
      webview.getSettings().setJavaScriptEnabled(true);
      webview.loadUrl(aboutUrl);
      // hjælper det her??? webview.getSettings().setDatabasePath(...);
    } else {
      // Øv, vi viser URLen i en ekstern browser.
      // Når brugeren derefter trykker 'tilbage' ser han et tomt webview.
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(aboutUrl)));
    }

    webview.setBackgroundColor(Color.parseColor("#333333"));

    ImageButton sendFeedbackButton = (ImageButton) findViewById(R.id.about_footer_button);

    sendFeedbackButton.setOnClickListener(this);
  }

  public void onClick(View v) {
    String brødtekst = "";
    brødtekst += DRData.instans.stamdata.s("feedback_brugerspørgsmål");
    brødtekst += "\nkanal: " + DRData.instans.afspiller.kanalNavn+" ("+DRData.instans.afspiller.kanalUrl+")";
    brødtekst += "\n" + new MedieafspillerInfo().lavTelefoninfo(Om_DRRadio_akt.this);

    Kontakt.kontakt(this, EMAILSUBJECT, brødtekst, Log.log.toString());
  }
}
