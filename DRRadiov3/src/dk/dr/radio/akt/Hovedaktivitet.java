package dk.dr.radio.akt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.MediabuttonReceiver;
import dk.dr.radio.v3.R;

public class Hovedaktivitet extends Basisaktivitet implements Runnable {

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
    setTitle("D R Radio"); // til blinde, for at undgå at "DR Radio" bliver udtalt som "Doktor Radio"

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

      //Log.d("getIntent()="+getIntent().getFlags());
      if (App.prefs.getBoolean("startAfspilningMedDetSammme", false) && DRData.instans.afspiller.getAfspillerstatus()==Status.STOPPET) {
        App.forgrundstråd.post(new Runnable() {
          @Override
          public void run() {
            try {
              DRData.instans.afspiller.startAfspilning();
            } catch (Exception e) { Log.rapporterFejl(e); }
          }
        });
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
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    DRData.instans.grunddata.observatører.add(this);
    run();
  }

  @Override
  protected void onPause() {
    DRData.instans.grunddata.observatører.remove(this);
    super.onPause();
  }

  private static final String drift_statusmeddelelse_NØGLE = "drift_statusmeddelelse";
  private static String vis_drift_statusmeddelelse;
  private boolean viser_drift_statusmeddelelse;

  @Override
  public void run() {
    if (viser_drift_statusmeddelelse) return;
    if (vis_drift_statusmeddelelse==null) {
      String drift_statusmeddelelse = DRData.instans.grunddata.android_json.optString(drift_statusmeddelelse_NØGLE).trim();
      // Tjek i prefs om denne drifmeddelelse allerede er vist.
      // Der er 1 ud af en millards chance for at hashkoden ikke er ændret, den risiko tør vi godt løbe
      int drift_statusmeddelelse_hash = drift_statusmeddelelse.hashCode();
      final int gammelHashkode = App.prefs.getInt(drift_statusmeddelelse_NØGLE, 0);
      if (gammelHashkode != drift_statusmeddelelse_hash && !"".equals(drift_statusmeddelelse)) { // Driftmeddelelsen er ændret. Vis den...
        Log.d("vis_drift_statusmeddelelse='" + drift_statusmeddelelse + "' nyHashkode=" + drift_statusmeddelelse_hash + " gammelHashkode=" + gammelHashkode);
        vis_drift_statusmeddelelse = drift_statusmeddelelse;
      }
    }
    if (vis_drift_statusmeddelelse!=null) {
      AlertDialog.Builder ab = new AlertDialog.Builder(this);
      ab.setMessage(Html.fromHtml(vis_drift_statusmeddelelse));
      ab.setPositiveButton("OK", new AlertDialog.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          if (vis_drift_statusmeddelelse==null) return;
          App.prefs.edit().putInt(drift_statusmeddelelse_NØGLE, vis_drift_statusmeddelelse.hashCode()).commit(); // ...og gem ny hashkode i prefs
          vis_drift_statusmeddelelse = null;
          viser_drift_statusmeddelelse = false;
          run(); // Se om der er flere meddelelser
        }
      });
      AlertDialog d = ab.create();
      d.show();
      viser_drift_statusmeddelelse = true;
      ((TextView) (d.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
    }
  }

  @Override
  public void finish() {
    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    int volumen = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

    // Hvis der er skruet helt ned så stop afspilningen
    if (volumen == 0 && DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET) {
      DRData.instans.afspiller.stopAfspilning();
    }

    if (DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET) {
      // Spørg brugeren om afspilningen skal stoppes
      showDialog(0);
      return;
    }
    MediabuttonReceiver.afregistrér();
    super.finish();
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    AlertDialog.Builder ab = new AlertDialog.Builder(this);
    ab.setMessage("Stop afspilningen?");
    ab.setPositiveButton("Stop", new AlertDialog.OnClickListener() {
      public void onClick(DialogInterface arg0, int arg1) {
        DRData.instans.afspiller.stopAfspilning();
        Hovedaktivitet.super.finish();
      }
    });
    ab.setNeutralButton("Fortsæt i\nbaggrunden", new AlertDialog.OnClickListener() {
      public void onClick(DialogInterface arg0, int arg1) {
        Hovedaktivitet.super.finish();
      }
    });
    //ab.setNegativeButton("Annullér", null);
    return ab.create();
  }
/*
  @Override
  public void onPrepareDialog(int id, Dialog d) {
    if (id == 0) try {
      ((AlertDialog) d).setMessage(Html.fromHtml(vis_drift_statusmeddelelse));
      ((TextView) (d.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
    } catch (Exception e) { Log.rapporterFejl(e); }
  }
*/
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
