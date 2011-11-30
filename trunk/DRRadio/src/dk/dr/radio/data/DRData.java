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

package dk.dr.radio.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;
import dk.dr.radio.data.json.stamdata.Kanal;
import dk.dr.radio.data.json.udsendelser.Udsendelser;
import dk.dr.radio.data.json.stamdata.Stamdata;
import dk.dr.radio.data.json.spiller_nu.SpillerNu;
import dk.dr.radio.diverse.Rapportering;
import dk.dr.radio.R;
import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.util.Log;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;

/**
 * Det centrale objekt som alt andet bruger til
 */
public class DRData implements java.io.Serializable {

	public static Context appCtx;
	public static SharedPreferences prefs;

	private static final int stamdataID = 22 ;
	private static final String stamdataUrl = "http://www.dr.dk/tjenester/iphone/radio/settings/android" + stamdataID + ".drxml";
	private static final String STAMDATA = "stamdata" + stamdataID ;

	/** Globalt flag */
	public static boolean udvikling;
	public Afspiller afspiller;

	Handler handler = new Handler();

	private SpillerNu spillerNuListe2;
	private Udsendelser udsendelser2;

	public Stamdata stamdata;
	public Udsendelser udsendelser;
	public boolean udsendelser_ikkeTilgængeligt;
	public SpillerNu spillerNuListe;

	public String aktuelKanalkode;
	public Kanal aktuelKanal;

	public static final String NØGLE_lydformat = "lydformat";
	public static final String NØGLE_kanal = "kanal";

	public final Rapportering rapportering = new Rapportering();

	/** Bruges til at sende broadcasts om nye stamdata */
	public static final String OPDATERINGSINTENT_Stamdata = "dk.dr.radio.afspiller.OPDATERING_Stamdata";

	/** Bruges til at sende broadcasts om ny info om udsendelsen (programinfo) */
	public static final String OPDATERINGSINTENT_Udsendelse = "dk.dr.radio.afspiller.OPDATERING_Udsendelse";

	/** Bruges til at sende broadcasts om ny info om hvad der spiller nu  */
	public static final String OPDATERINGSINTENT_SpillerNuListe = "dk.dr.radio.afspiller.OPDATERING_SpillerNuListe";

	/** Hvis true er indlæsning i gang og der skal vises en venteskærm.
	* Man kan vente på et broadcast eller kalde wait() for at blive vækket når indlæsning er færdig
	*/
	public boolean indlæserVentVenligst = false;

	public static DRData instans;

	//
	// Opdateringer i baggrunden.
	//
	private boolean baggrundsopdateringAktiv = false;
	private boolean baggrundstrådSkalVente = true;

	/** Variation der tjekker om instansen er tom og - hvis det er tilfældet - indlæser en instans fra disk - synkront
	* SKAL kaldes fra GUI-tråden
	*/
	public static synchronized DRData tjekInstansIndlæst(Context akt) throws IOException, JSONException {
		appCtx = akt.getApplicationContext();
		if (instans == null) {
      prefs = PreferenceManager.getDefaultSharedPreferences(akt);

      String stamdatastr = prefs.getString(STAMDATA, null);

      if (stamdatastr == null) {
        // Indlæs fra raw this vi ikke har nogle cachede stamdata i prefs
        InputStream is = akt.getResources().openRawResource(R.raw.stamdata_android21);
        stamdatastr = JsonIndlaesning.læsInputStreamSomStreng(is);
      }

      // Det skulle være rimeligt sikkert at vælge lydformat
      // HLS2 (httplive2) på Android 3.2 og frem
      // Det gælder nok også Android 3.1, men jeg er ikke sikkker. Jacob
		/*
      if (Build.VERSION.SDK_INT >= 13 && !prefs.contains(NØGLE_lydformat)) {
        prefs.edit().putString(NØGLE_lydformat, "httplive2").commit();
      }
      */	// fjernet da DRs HLS p.t. er ukompatibel med android devices

      instans = new DRData();
      instans.stamdata = JsonIndlaesning.parseStamdata(stamdatastr);

			// Kanalvalg. Tjek først Preferences, brug derefter JSON-filens forvalgte kanal
			if (instans.aktuelKanalkode == null) instans.aktuelKanalkode = prefs.getString(NØGLE_kanal, null);
			if (instans.aktuelKanalkode == null) instans.aktuelKanalkode = instans.stamdata.s("forvalgt");
      instans.aktuelKanal = instans.stamdata.kanalkodeTilKanal.get(instans.aktuelKanalkode);

      instans.afspiller = new Afspiller();

      String url = instans.findKanalUrlFraKode(instans.aktuelKanal);
      instans.afspiller.setKanal(instans.aktuelKanal.longName, url);
		}


    // 31. okt: Fjernet af Jacob - da baggrundstråden ikke skal startes af f.eks. widgetter
    // se tjekBaggrundstrådStartet()
		//if (!instans.baggrundstråd.isAlive()) instans.baggrundstråd.start();

		return instans;
	}


  /**
   * Først efter indlæstning starter vi baggrundstråden - fra splash og fra afspiller_akt.
   * Dette er et separat skridt da det ikke skal ske ved opstart af levende ikon
   */
	public void tjekBaggrundstrådStartet() {
		if (!baggrundstråd.isAlive()) baggrundstråd.start();
  }



	/**
	 * Skifter til en anden kanal
	 * @param nyKanalkode en af "P1", "P2", "P3", "P5D", "P6B", "P7M", "RAM", etc
	 * eller evt P4-kanal "KH4", "NV4", "AR4", "AB4", "OD4", "AL4", "HO4", "TR4", "RO4", "ES4", "NS4"],
	 * Bemærk at "P4" eller andre uden en streamUrl IKKE er tilladt
	 */
	public void skiftKanal(String nyKanalkode) {
    Log.d("DRData.skiftKanal("+nyKanalkode);
		aktuelKanalkode = nyKanalkode;
    aktuelKanal = stamdata.kanalkodeTilKanal.get(aktuelKanalkode);

		prefs.edit().putString(NØGLE_kanal, aktuelKanalkode).commit();
    udsendelser = null;
    spillerNuListe = null;
    udsendelser_ikkeTilgængeligt = false;
    // Væk baggrundstråden så den indlæser den nye kanals udsendelser etc og laver broadcasts med nyt info
    baggrundstrådSkalOpdatereNu();
	}


  public void setBaggrundsopdateringAktiv(boolean aktiv) {
    if (baggrundsopdateringAktiv == aktiv) return;

    baggrundsopdateringAktiv = aktiv;

    Log.d("setBaggrundsopdateringAktiv( "+aktiv);

    if (baggrundsopdateringAktiv) baggrundstrådSkalOpdatereNu(); // væk baggrundtråd
  }

  private void baggrundstrådSkalOpdatereNu() {
    baggrundstrådSkalVente = false;
    synchronized (baggrundstråd) { baggrundstråd.notify(); }
  }

	final Thread baggrundstråd = new Thread() {
		@Override
		public void run() {

      // Hovedløkke
			while (true) {
				try {
          if (baggrundstrådSkalVente) synchronized (baggrundstråd) {
            if (baggrundsopdateringAktiv)
              baggrundstråd.wait(15000); // Vent 15 sekunder. Men vågn op hvis nogen kalder baggrundstråd.notify()!
            else
              baggrundstråd.wait(); // Vent indtil tråden vækkes

            baggrundstråd.wait(50); // Vent kort så den aktiverende tråd kan gøre sit arbejde færdigt
          }
          baggrundstrådSkalVente = true;

					hentUdsendelserOgSpillerNuListe();

				} catch (Exception ex) { Log.e(ex); }
			}
		}
	};


  private void hentUdsendelserOgSpillerNuListe() {
    String url = stamdata.s("spiller_nu_url") + aktuelKanalkode;
    try {
      spillerNuListe2 = JsonIndlaesning.hentSpillerNuListe(url);
    } catch (Exception ex) {
      Log.e("Kunne ikke hente spillerNuListe "+url, ex);
      spillerNuListe2 = null;
    }
    // Al opdatering, herunder tildeling bør ske i GUI-tråden for at undgå af GUIen er i gang med at
    // bruge objektet mens det opdateres
    handler.post(new Runnable() {
      public void run() {
        spillerNuListe = spillerNuListe2;
        appCtx.sendBroadcast(new Intent(OPDATERINGSINTENT_SpillerNuListe));
      }
    });

    try {
      url = stamdata.s("program_url") + aktuelKanalkode;
      udsendelser2 = JsonIndlaesning.hentUdsendelser(url);
      udsendelser_ikkeTilgængeligt = false;
    } catch (Exception ex) {
      Log.e("Kunne ikke hente udsendelser fra "+url, ex);
      udsendelser2 = null;
      udsendelser_ikkeTilgængeligt = true;
    }
    handler.post(new Runnable() {
      public void run() {
        udsendelser = udsendelser2;
        appCtx.sendBroadcast(new Intent(OPDATERINGSINTENT_Udsendelse));
      }
    });


    // Tjek om en evt ny udgave af stamdata skal indlæses
    final String STAMDATA_SIDST_INDLÆST = "stamdata_sidst_indlæst";
    long sidst = prefs.getLong(STAMDATA_SIDST_INDLÆST, 0);
    long nu = System.currentTimeMillis();
    long alder = (nu - sidst)/1000/60;
    if (alder>= 30) try { // stamdata er ældre end en halv time
      Log.d("Stamdata er "+alder+" minutter gamle, opdaterer dem...");
      String stamdatastr  = JsonIndlaesning.hentUrlSomStreng(stamdataUrl);
      //Log.d(stamdatastr);
      final Stamdata stamdata2 = JsonIndlaesning.parseStamdata(stamdatastr);
      // Hentning og parsning gik godt - vi gemmer den nye udgave i prefs
      prefs.edit().putString(STAMDATA, stamdatastr).
              putLong(STAMDATA_SIDST_INDLÆST, nu).
              commit();

      handler.post(new Runnable() {
        public void run() {
          stamdata = stamdata2;
          appCtx.sendBroadcast(new Intent(OPDATERINGSINTENT_Stamdata));
        }
      });
    } catch (Exception e) {
      Log.e("Fejl parsning af stamdata. Url="+stamdataUrl, e);
    }
  }


  public String findKanalUrlFraKode(Kanal kanal) {
    String lydformat = prefs.getString(NØGLE_lydformat, "shoutcast");
    boolean højKvalitet = "høj".equals(prefs.getString("lydkvalitet", "standard"));
    rapportering.nulstil();
    rapportering.lydformat = lydformat + (højKvalitet?"_høj":"_standard");
    String url = kanal.shoutcastUrl;
    if ("rtsp".equals(lydformat)) url = kanal.rtspUrl;
    else if ("httplive".equals(lydformat)) url = kanal.aacUrl;
    else if ("httplive2".equals(lydformat)) url = kanal.aacUrl.replaceAll("httplive", "http");
    if (højKvalitet)
	{
		url = url.replace("LQ", "HQ");
    	url = url.replace("L.stream", "H.stream") ;		// MP3, RTSP stream name workaround
	}
    String info = "Kanal: "+kanal.longName+"\nlydformat: "+lydformat
            +"\nKvalitet: "+(højKvalitet?"Høj":"Normal")+"\n"+url;
    if (DRData.udvikling) Toast.makeText(appCtx, info, Toast.LENGTH_LONG).show();
    Log.d(info);
    return url;
  }

}
