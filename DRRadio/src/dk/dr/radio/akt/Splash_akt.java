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

package dk.dr.radio.akt;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import dk.dr.radio.R;
import dk.dr.radio.data.DRData;
import dk.dr.radio.diverse.Log;

public class Splash_akt extends Activity implements Runnable {


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      setContentView(R.layout.splash_akt);
    } catch (Throwable ignored) {
    } // bitmappen main_app_bg.png fylder for meget på Xperia X10i. De må leve uden splash-skærmbillede

    // Jacob: Det oprindelige 200k billede med baggrund og DR-logo var på 640x960 punkter
    // Det træk jeg DR-logoet ud på 260x78 punkter.
    // DVS vi skal skalere logoet så det fylder 260/640 = 40,625 % af skærmen i bredden
    // og proportionalt i højden
    View dr_logo = findViewById(R.id.splash_dr_logo);

    // Jacob: Det er set enkelte gange at dr_logo==null !
    if (dr_logo != null) {
      int skærmbredde = getWindowManager().getDefaultDisplay().getWidth();
      dr_logo.getLayoutParams().width = skærmbredde * 260 / 640;
    }

    // Volumen op/ned skal styre lydstyrken af medieafspilleren, uanset som noget spilles lige nu eller ej
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    if (savedInstanceState == null) try { // Hvis frisk start (ikke skærmvending)
      DRData.instans.tjekBaggrundstrådStartet();

      Handler handler = new Handler();
      // Starter hurtig splash nu - under udviklingen skal vi ikke sidde og vente på den!
      handler.postDelayed(this, 200);
    } catch (Exception ex) {
      // TODO popop-advarsel til bruger om intern fejl og rapporter til udvikler-dialog
      Log.rapporterOgvisFejl(this, ex);
    }
  }

  public void run() {
    startActivity(new Intent(this, Afspilning_akt.class));
  }
}
