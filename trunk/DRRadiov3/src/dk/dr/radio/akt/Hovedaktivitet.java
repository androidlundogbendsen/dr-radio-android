package dk.dr.radio.akt;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.MediabuttonReceiver;
import dk.dr.radio.v3.R;

public class Hovedaktivitet extends Basisaktivitet {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private Venstremenu_frag venstremenuFrag;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // android:logo="@drawable/dr_logo" ignoreres på Android 2, sæt det derfor også her:
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      getSupportActionBar().setLogo(R.drawable.dr_logo);
    }

    if (App.prefs.getBoolean("tving_lodret_visning", true)) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    setContentView(R.layout.hoved_akt);

    //ActionBar actionBar = getSupportActionBar();
    //actionBar.setDisplayShowTitleEnabled(false);

    venstremenuFrag = (Venstremenu_frag) getSupportFragmentManager().findFragmentById(R.id.venstremenu_frag);

    // Set up the drawer.
    venstremenuFrag.setUp(R.id.venstremenu_frag, (DrawerLayout) findViewById(R.id.drawer_layout));

    if (savedInstanceState == null) try {

      String visFragment = getIntent().getStringExtra(VisFragment_akt.KLASSE);
      if (visFragment != null) {
        Fragment f = (Fragment) Class.forName(visFragment).newInstance();
        Bundle b = getIntent().getExtras();
        f.setArguments(b);

        // Vis fragmentet i FrameLayoutet
        Log.d("Viser fragment " + f + " med arg " + b);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, f)
            .commit();
      } else {
        // Startet op fra hjemmeskærm eller notifikation
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, new Kanaler_frag())
            .commit();
        // Hvis det ikke er en direkte udsendelse, så hop ind i den pågældende udsendelsesside
        if (DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET) {
          Lydkilde lydkilde = DRData.instans.afspiller.getLydkilde();
          if (lydkilde instanceof Udsendelse) {
            Udsendelse udsendelse = lydkilde.getUdsendelse();
            Fragment f = new Udsendelse_frag();
            f.setArguments(new Intent()
                .putExtra(Basisfragment.P_kode, lydkilde.getKanal().kode)
                .putExtra(DRJson.Slug.name(), udsendelse.slug).getExtras());
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.indhold_frag, f)
                .addToBackStack("Udsendelse")
                .commit();
            return;
          }
        }
        venstremenuFrag.sætListemarkering(Venstremenu_frag.FORSIDE_INDEX); // "Forside
      }

      if (App.erOnline()) {
        App.forgrundstråd.postDelayed(App.instans.onlineinitialisering, 500); // Initialisér onlinedata
      } else {
        App.netværk.observatører.add(App.instans.onlineinitialisering); // Vent på vi kommer online og lav så et tjek
      }


    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    MediabuttonReceiver.registrér();
  }


  @Override
  public void onBackPressed() {
    if (venstremenuFrag.isDrawerOpen()) {
      venstremenuFrag.skjulMenu();
    } else {
      super.onBackPressed();
      MediabuttonReceiver.afregistrér();
    }
  }

  /**
   * Om tilbageknappen skal afslutte programmet eller vise venstremenuen
   static boolean tilbageViserVenstremenu = true; // hack - static, ellers skulle den gemmes i savedInstanceState

   @Override public void onBackPressed() {
   if (tilbageViserVenstremenu) {
   venstremenuFrag.visMenu();
   tilbageViserVenstremenu = false;
   } else {
   super.onBackPressed();
   tilbageViserVenstremenu = true;
   }
   }


   @Override public boolean dispatchTouchEvent(MotionEvent ev) {
   tilbageViserVenstremenu = true;
   return super.dispatchTouchEvent(ev);
   }

   @Override public boolean dispatchTrackballEvent(MotionEvent ev) {
   tilbageViserVenstremenu = true;
   return super.dispatchTrackballEvent(ev);
   }
   */
}
