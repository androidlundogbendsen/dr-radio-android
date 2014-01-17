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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import dk.dr.radio.R;
import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.AfspillerListener;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.spiller_nu.SpillerNuElement;
import dk.dr.radio.data.stamdata.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

public class Afspilning_akt extends Activity implements AfspillerListener {
  private ViewFlipper flipper;
  private ImageButton playStopButton;
  private ImageView previousImageView;
  private ImageView nextImageView;
  private Afspiller afspiller;
  private TextView currentProgramTitleTextView;
  private TextView currentProgramDescriptionTextView;
  private TextView currentChannelTextView;
  private TextView nextProgramTitleTextView;
  private LinearLayout tracksLinearLayout;
  private TextView status;
  private SharedPreferences prefs;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    prefs = PreferenceManager.getDefaultSharedPreferences(this);


    // Fuld skærm skjuler den notification vi sætter op så brugeren ikke opdager den,
    // og det er lidt forvirrende så vi slår det fra for nu
    //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN) ;
    setContentView(R.layout.afspilning_akt);
    initControls();

    DRData.instans.tjekBaggrundstrådStartet();
    afspiller = DRData.instans.afspiller;

    registerReceiver(stamdataOpdateretReciever, new IntentFilter(DRData.OPDATERINGSINTENT_Stamdata));
    registerReceiver(udsendelserOpdateretReciever, new IntentFilter(DRData.OPDATERINGSINTENT_Udsendelse));
    registerReceiver(spillerNuListeOpdateretReciever, new IntentFilter(DRData.OPDATERINGSINTENT_SpillerNuListe));

    TextView nextProgramHeaderTextView = (TextView) findViewById(R.id.player_next_program_textview);
    nextProgramHeaderTextView.setText(DRData.instans.stamdata.json.optString("text_footerTitle"));

    INGEN_LASTFM = getResources().getDrawable(R.drawable.nolastfm);
    try {
      visAktuelKanal();
      visAktuelUdsendelse();
      visNæsteUdsendelse();
      visSpillerNuInfo();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }

    visStartStopKnap();

    // Volumen op/ned skal styre lydstyrken af medieafspilleren, uanset som noget spilles lige nu eller ej
    setVolumeControlStream(AudioManager.STREAM_MUSIC);


    // Vis korrekt knap og/eller start afspilning
    // når aktivitet startes men ikke hvis den genoptages (dvs kun når savedInstanceState==null)
    boolean startAfspilningMedDetSammme = prefs.getBoolean("startAfspilningMedDetSammme", false);
    if (savedInstanceState == null && startAfspilningMedDetSammme && afspiller.getAfspillerstatus() == Afspiller.STATUS_STOPPET) {
      startAfspilning();
    } else {
      visStartStopKnap();
    }


    //XXX TODO xxx Garmin A50 skal bruge RTSP som standard. mp3/shoutcast virker ikke

    afspiller.addAfspillerListener(Afspilning_akt.this);
    afspiller.addAfspillerListener(DRData.instans.rapportering);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();

    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    int volumen = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

    //boolean lukAfspillerServiceVedAfslutning = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lukAfspillerServiceVedAfslutning", false);

    // Hvis der er skruet helt ned så stop afspilningen
    if (volumen == 0 && afspiller.afspillerstatus != Afspiller.STATUS_STOPPET) {
      afspiller.stopAfspilning();
      super.onBackPressed();
    } else if (afspiller.afspillerstatus == Afspiller.STATUS_STOPPET) {
      super.onBackPressed();
    } else {
      // Spørg brugeren om afspilningen skal stoppes
      showDialog(1);
    }
  }

  @Override
  protected void onDestroy() {
    afspiller.addAfspillerListener(Afspilning_akt.this);
    afspiller.addAfspillerListener(DRData.instans.rapportering);
    unregisterReceiver(stamdataOpdateretReciever);
    unregisterReceiver(udsendelserOpdateretReciever);
    unregisterReceiver(spillerNuListeOpdateretReciever);
    super.onDestroy();
  }

  AlertDialog internetforbindelseManglerDialog;

  @Override
  protected void onResume() {

    // se om vi er online
    if (App.erOnline()) {
      // hurra - opdater data fra server
      DRData.instans.setBaggrundsopdateringAktiv(true);
    } else {
      // Informer brugeren hvis vi er offline
      if (internetforbindelseManglerDialog == null) {
        internetforbindelseManglerDialog = new AlertDialog.Builder(this).create();
        internetforbindelseManglerDialog.setTitle("Internetforbindelse mangler");
        internetforbindelseManglerDialog.setMessage("Din telefon er ikke tilsluttet internettet. For at høre radio skal du åbne op for forbindelser via WIFI eller mobildata.");
      }
      if (!internetforbindelseManglerDialog.isShowing()) {
        internetforbindelseManglerDialog.show();
      }
    }
    super.onResume();
  }

  @Override
  protected void onPause() {
    DRData.instans.setBaggrundsopdateringAktiv(false);
    super.onPause();
  }

  private void initControls() {
    flipper = (ViewFlipper) findViewById(R.id.flipper);
    currentProgramTitleTextView = (TextView) findViewById(R.id.player_current_program_title_textview);
    currentProgramDescriptionTextView = (TextView) findViewById(R.id.player_current_program_description_textview);
    currentChannelTextView = (TextView) findViewById(R.id.player_current_program_channel_textview);
    nextProgramTitleTextView = (TextView) findViewById(R.id.player_next_program_title_textview);
    tracksLinearLayout = (LinearLayout) findViewById(R.id.player_tracks_linearlayout);
    status = (TextView) findViewById(R.id.status);

    try { // DRs skrifttyper er ikke offentliggjort i SVN, derfor kan følgende fejle:
      Typeface skrift_DRiRegular = Typeface.createFromAsset(getAssets(), "DRiRegular.otf");
      status.setTypeface(skrift_DRiRegular);
    } catch (Exception e) {
      Log.e("DRs skrifttyper er ikke tilgængelige", e);
    }

    Button selectChannelButton = (Button) findViewById(R.id.player_select_channel_button);
    selectChannelButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        startActivityForResult(new Intent(Afspilning_akt.this, Kanalvalg_akt.class), 117);
      }
    });

    playStopButton = (ImageButton) findViewById(R.id.start_stop_knap);
    playStopButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        if (afspiller.getAfspillerstatus() == Afspiller.STATUS_STOPPET) {
          startAfspilning();
        } else {
          stopAfspilning();
        }
      }
    });
    playStopButton.setOnFocusChangeListener(new OnFocusChangeListener() {
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          playStopButton.setColorFilter(0xFFC0C0C0, PorterDuff.Mode.MULTIPLY);
        } else {
          playStopButton.setColorFilter(null);
        }
      }
    });

    Button playerAboutButton = (Button) findViewById(R.id.player_about_button);
    playerAboutButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        startActivity(new Intent(Afspilning_akt.this, Om_DRRadio_akt.class));
      }
    });

    Button formatKnap = (Button) findViewById(R.id.player_format_button);
    formatKnap.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent i = new Intent(Afspilning_akt.this, Indstillinger_akt.class);
        i.putExtra(Indstillinger_akt.åbn_formatindstilling, true);
        startActivity(i);
      }
    });

    previousImageView = (ImageView) findViewById(R.id.previous);
    previousImageView.setAlpha(200);

    nextImageView = (ImageView) findViewById(R.id.next);
    nextImageView.setAlpha(200);
    nextImageView.setVisibility(ImageButton.GONE);
  }

  /**
   * Denne metode kaldes når der er valgt en kanal
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d("onActivityResult " + requestCode + " " + resultCode + " " + data);
    // requestCode er 117
    if (resultCode != RESULT_OK) {
      return;  // Hvis brugeren trykkede på tilbage-knappen eller valgte den samme kanal
    }
    // Afspilning skal starte så når brugeren har valgt kanal
    // sætKanal();
    startAfspilning();

    // Kald for at nulstille skærmen
    try {
      visAktuelUdsendelse();
      visNæsteUdsendelse();
      visSpillerNuInfo();
    } catch (Exception e) {
      Log.rapporterOgvisFejl(this, e);
    }
  }

  private void startAfspilning() {
    Log.d("startAfspilning");
    try {
      sætKanal();
      afspiller.startAfspilning();
      DRData.instans.rapportering.afspilningForsøgtStartet = System.currentTimeMillis();
      visStartStopKnap();
    } catch (Exception e) {
      Log.e(e);
    }
  }

  private void sætKanal() {
    String url = DRData.instans.findKanalUrlFraKode(DRData.instans.aktuelKanal);

    afspiller.setKanal(DRData.instans.aktuelKanal.longName, url);
    //startStopButton.setImageResource(R.drawable.buffer_white);
    visAktuelKanal();
  }

  private void stopAfspilning() {
    afspiller.stopAfspilning();
  }

  private void visStatus(String txt) {
    status.setText(txt);
    Log.d(txt);
  }

  private void visStartStopKnap() {
    if (afspiller == null) {
      return;
    }
    if (afspiller.getAfspillerstatus() == Afspiller.STATUS_STOPPET) {
      playStopButton.setImageResource(R.drawable.play);
    } else {
      playStopButton.setImageResource(R.drawable.stop);
    }
  }

  private void sætForbinderProcent(int procent) {
    //Log.d( "sætforbinderProcent( " + procent + " )" );

    int afspillerstatus = afspiller.getAfspillerstatus();

    if (procent <= 0) {
      if (afspillerstatus == Afspiller.STATUS_FORBINDER) {
        visStatus("Forbinder...");
      } else {
        visStatus("");
      }
    } else {
      visStatus("Forbinder... " + procent + "%");
    }
  }

  public void onAfspilningStartet() {
    visStatus("Afspiller");
    visStartStopKnap();
  }

  public void onAfspilningStoppet() {
    visStatus("Stoppet");
    visStartStopKnap();
  }

  public void onAfspilningForbinder(int procent) {
    if (procent >= 100) {
      onAfspilningStartet();
    } else {
      sætForbinderProcent(procent);
    }
  }

  final static String drift_statusmeddelelse_NØGLE = "drift_statusmeddelelse";
  static int drift_statusmeddelelse_hash = 0;
  static String drift_statusmeddelelse = ""; // static necesas por certigi ke la valoro pluiros al venonta aktiveco se oni turnas la ekranon

  private BroadcastReceiver stamdataOpdateretReciever = new BroadcastReceiver() {
    @Override
    public void onReceive(Context ctx, Intent i) {
      Log.d("stamdataOpdateretReciever");

      drift_statusmeddelelse = DRData.instans.stamdata.json.optString(drift_statusmeddelelse_NØGLE).trim();

      // Tjek i prefs om denne drifmeddelelse allerede er vist.
      // Der er 1 ud af en millards chance for at hashkoden ikke er ændret, den risiko tør vi godt løbe
      drift_statusmeddelelse_hash = drift_statusmeddelelse.hashCode();
      final int gammelHashkode = prefs.getInt(drift_statusmeddelelse_NØGLE, 0);
      Log.d("drift_statusmeddelelse='" + drift_statusmeddelelse + "' nyHashkode=" + drift_statusmeddelelse_hash + " gammelHashkode=" + gammelHashkode);
      if (gammelHashkode != drift_statusmeddelelse_hash && !"".equals(drift_statusmeddelelse)) { // Driftmeddelelsen er ændret. Vis den...
        showDialog(0);
      }
    }
  };


  @Override
  protected Dialog onCreateDialog(final int id) {
    AlertDialog.Builder ab = new AlertDialog.Builder(this);
    if (id == 0) {
      ab.setMessage(drift_statusmeddelelse);
      ab.setPositiveButton("OK", new AlertDialog.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          prefs.edit().putInt(drift_statusmeddelelse_NØGLE, drift_statusmeddelelse_hash).commit(); // ...og gem ny hashkode i prefs
        }
      });
    } else { // if (id == 1)
      ab.setMessage("Stop afspilningen?");
      ab.setPositiveButton("Stop\nradioen", new AlertDialog.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          stopAfspilning();
          finish();
        }
      });
      ab.setNeutralButton("Fortsæt i\nbaggrunden", new AlertDialog.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          finish();
        }
      });
      //ab.setNegativeButton("Annullér", null);
    }
    return ab.create();
  }


  @Override
  public void onPrepareDialog(int id, Dialog d) {
    if (id == 0) ((AlertDialog) d).setMessage(drift_statusmeddelelse);
  }


  private BroadcastReceiver udsendelserOpdateretReciever = new BroadcastReceiver() {
    @Override
    public void onReceive(Context ctx, Intent i) {
      //Log.d("udsendelserOpdateretReciever");
      try {
        visAktuelUdsendelse();
        visNæsteUdsendelse();
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    }
  };
  private BroadcastReceiver spillerNuListeOpdateretReciever = new BroadcastReceiver() {
    String forrigeSpillerNu = "";

    @Override
    public void onReceive(Context ctx, Intent i) {
      Log.d("spillerNuListeOpdateretReciever");
      // visSpillerNuInfo() er dyr at kalde så den kalder vi kun hvis data er ændret
      String spillerNu = "null";

      if (DRData.instans.spillerNuListe != null && DRData.instans.spillerNuListe.liste.size() > 0) {
        spillerNu = String.valueOf(DRData.instans.spillerNuListe.liste.get(0).title);
      }

      if (!forrigeSpillerNu.equals(spillerNu)) {
        forrigeSpillerNu = spillerNu;
        Log.d("spillerNuListeOpdateretReciever opdatering " + spillerNu);
        visSpillerNuInfo();
      }
    }
  };

  private void visAktuelKanal() {
    String kanal = DRData.instans.aktuelKanalkode;

    ImageView aktuelKanalImageView = (ImageView) findViewById(R.id.kanalbillede);
    TextView aktuelKanalTextView = (TextView) findViewById(R.id.kanaltekst);

    Resources res = getResources();
    int id = res.getIdentifier("kanal_" + kanal.toLowerCase(), "drawable", getPackageName());

    if (id == 0) {
      String visningsnavn = DRData.instans.stamdata.kanalkodeTilKanal.get(kanal).longName;
      aktuelKanalTextView.setText(visningsnavn);
      aktuelKanalTextView.setVisibility(View.VISIBLE);
      aktuelKanalImageView.setVisibility(View.GONE);
    } else {
      aktuelKanalImageView.setImageResource(id);
      aktuelKanalTextView.setVisibility(View.GONE);
      aktuelKanalImageView.setVisibility(View.VISIBLE);
    }
  }

  private void visAktuelUdsendelse() {
    String aboveHeader = DRData.instans.stamdata.json.optString("text_aboveHeader");
    Kanal kanal = DRData.instans.aktuelKanal;
    String tekst = aboveHeader + " " + kanal.longName + ":";

    currentChannelTextView.setText(tekst);

    if (DRData.instans.udsendelser_ikkeTilgængeligt) {
      currentProgramTitleTextView.setText("Kunne ikke hente programinfo");
      currentProgramDescriptionTextView.setText("");
    } else if (DRData.instans.udsendelser == null || DRData.instans.udsendelser.currentProgram == null) {
      currentProgramTitleTextView.setText("Venter på programinfo");
      currentProgramDescriptionTextView.setText("");
    } else {

      String currentProgamTitle = DRData.instans.udsendelser.currentProgram.title;
      currentProgramTitleTextView.setText(currentProgamTitle);

      String currentProgamDescription = DRData.instans.udsendelser.currentProgram.description;
      currentProgramDescriptionTextView.setText(Html.fromHtml(currentProgamDescription));

      // Konverter tekst-links i programinfo til rigtige links - frederik
      Linkify.addLinks(currentProgramDescriptionTextView, Linkify.WEB_URLS);
    }
  }

  private void visNæsteUdsendelse() {

    if (DRData.instans.udsendelser != null && DRData.instans.udsendelser.nextProgram != null) {
      String nextProgramTitle = DRData.instans.udsendelser.nextProgram.title;
      String nextProgramTitleLimited = nextProgramTitle;
      String nextProgramStartTime = DRData.instans.udsendelser.nextProgram.start;
      String nextProgramStopTime = DRData.instans.udsendelser.nextProgram.stop;
      String nextProgramTime = "(" + nextProgramStartTime + " - " + nextProgramStopTime + ")";
      nextProgramTitleTextView.setText(nextProgramTitleLimited + " " + nextProgramTime);
    } else {
      nextProgramTitleTextView.setText("Ingen programinfo");
    }
  }

  //@SuppressWarnings("unchecked")
  private void visSpillerNuInfo() {

    boolean showChannel = DRData.instans.stamdata.kanalerDerSkalViseSpillerNu.contains(DRData.instans.aktuelKanalkode);

    if (!showChannel || DRData.instans.spillerNuListe == null || DRData.instans.spillerNuListe.liste == null || DRData.instans.spillerNuListe.liste.isEmpty()) {
      tracksLinearLayout.setVisibility(LinearLayout.GONE);
      return;
    }

    tracksLinearLayout.setVisibility(LinearLayout.VISIBLE);

    final List<SpillerNuElement> tracks = new ArrayList<SpillerNuElement>(DRData.instans.spillerNuListe.liste);


    synchronized (flipper) {
      flipper.removeAllViews();

      Collections.rotate(tracks, -1);
      Collections.reverse(tracks);
      int index = 0;
      for (SpillerNuElement track : tracks) {
        View view = buildViewFromTrack(track, index);
        flipper.addView(view);
        index++;
      }

      //On reload of songs show only the left arrow
      previousImageView.setVisibility(ImageView.VISIBLE);
      nextImageView.setVisibility(ImageView.GONE);

    }
  }

  WeakHashMap<String, Drawable> urlTilDrawable = new WeakHashMap<String, Drawable>();
  Drawable INGEN_LASTFM;

  private View buildViewFromTrack(SpillerNuElement track, int index) {
    LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View view = mInflater.inflate(R.layout.flipper_internallayout, null);

    LinearLayout flipperLinearLayout = (LinearLayout) view.findViewById(R.id.FlipperLinearLayout);

    TextView flipperLastFmTextTextView = (TextView) view.findViewById(R.id.FlipperLastFmTextTextView);
    String lastFm = DRData.instans.stamdata.json.optString("text_trackPhoto");
    flipperLastFmTextTextView.setText(lastFm);

    TextView flipperTrackTitleHeaderTextView = (TextView) view.findViewById(R.id.FlipperTrackTitleHeaderTextView);

    int numberOfTracks = DRData.instans.spillerNuListe.liste.size();
    String trackHeader;
    if (index == 0) {
      trackHeader = DRData.instans.stamdata.json.optString("text_trackLast");
    } else if (index == numberOfTracks - 1) {
      trackHeader = DRData.instans.stamdata.json.optString("text_trackBeforeLast");
    } else {
      trackHeader = DRData.instans.stamdata.json.optString("text_trackPrevious");
    }

    flipperTrackTitleHeaderTextView.setText(trackHeader);

    TextView flipperTrackTitleTextView = (TextView) view.findViewById(R.id.FlipperTrackTitleTextView);
    flipperTrackTitleTextView.setText(track.title);

    TextView flipperTrackDisplayArtistTextView = (TextView) view.findViewById(R.id.FlipperTrackDisplayArtistTextView);
    flipperTrackDisplayArtistTextView.setText(track.displayArtist);

    TextView flipperTrackStartTimeTextView = (TextView) view.findViewById(R.id.FlipperTrackStartTimeTextView);
    flipperTrackStartTimeTextView.setText("Afspillet kl. " + track.start);

    return flipperLinearLayout;
  }

  /**
   * Håndtering af MENU-knappen
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //Log.d("onCreateOptionsMenu!!!");
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.afspilning_menu, menu);
    return true;
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.om) {
      startActivity(new Intent(this, Om_DRRadio_akt.class));
    } else if (id == R.id.indstillinger) {
      startActivity(new Intent(this, Indstillinger_akt.class));
    } else if (id == R.id.sluk) {
      if (afspiller.getAfspillerstatus() != Afspiller.STATUS_STOPPET) {
        stopAfspilning();
      }
      finish();
    }
    return true;
  }
}