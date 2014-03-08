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

package dk.dr.radio.skrald;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.widget.ImageButton;

import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.MedieafspillerInfo;
import dk.dr.radio.v3.R;

public class Om_DRRadio_akt extends Basisfragment implements OnClickListener {
  WebView webview;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rod = inflater.inflate(R.layout.om_drradio_akt, container, false);

    String aboutUrl = DRData.instans.stamdata.json.optString("about_url");

    webview = (WebView) rod.findViewById(R.id.about_webview);

    // Jacob: Fix for 'syg' webview-cache - se http://code.google.com/p/android/issues/detail?id=10789
    WebViewDatabase webViewDB = WebViewDatabase.getInstance(getActivity());
    if (webViewDB != null) {
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

    ImageButton sendFeedbackButton = (ImageButton) rod.findViewById(R.id.about_footer_button);

    sendFeedbackButton.setOnClickListener(this);
    return rod;
  }

  public void onClick(View v) {
    String brødtekst = "";
    brødtekst += DRData.instans.stamdata.android_json.optString("feedback_brugerspørgsmål");
    //brødtekst += "\nkanal: " + DRData.instans.afspiller.kanalNavn + " (" + DRData.instans.afspiller.kanalUrl + ")";
    brødtekst += "\n" + new MedieafspillerInfo().lavTelefoninfo(getActivity());

    App.kontakt(getActivity(), "Feedback på DR Radio Android App", brødtekst, Log.getLog());
  }
}
