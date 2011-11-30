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

import android.content.SharedPreferences;
import dk.dr.radio.data.DRData;
import java.util.Collections;
import java.util.List;
import dk.dr.radio.data.json.stamdata.Kanal;
import dk.dr.radio.data.json.spiller_nu.SpillerNuElement;
import dk.dr.radio.util.AnimationUtil;
import dk.dr.radio.util.ImageUtil;
import dk.dr.radio.util.Network;
import dk.dr.radio.util.StringUtil;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.util.Linkify;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import dk.dr.radio.afspilning.AService;
import dk.dr.radio.afspilning.AfspillerListener;
import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.util.Log;
import java.util.ArrayList;
import java.util.WeakHashMap;
import org.acra.ErrorReporter;


public class Afspilning_akt extends Activity implements AfspillerListener {

	private ViewFlipper flipper;
	private DRData drdata;
	private ImageButton playStopButton;
	private ImageView previousImageView;
	private ImageView nextImageView;
	private Afspiller afspiller;
	private Handler previousNextHandler;
	private Runnable previousNextRunner;
	private AsyncTask indlæsningAfBilleder = null;
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

    DRData.udvikling = prefs.getBoolean("udvikler", false);


    // Fuld skærm skjuler den notification vi sætter op så brugeren ikke opdager den,
    // og det er lidt forvirrende så vi slår det fra for nu
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN) ;
		setContentView(R.layout.afspilning_akt);
		initControls();

    try {
      drdata = DRData.tjekInstansIndlæst(this);
      drdata.tjekBaggrundstrådStartet();
      afspiller = drdata.afspiller;
    } catch (Exception ex) {
      // TODO popop-advarsel til bruger om intern fejl og rapporter til udvikler-dialog
      Log.kritiskFejl(this, ex);
      return;
    }

    registerReceiver(stamdataOpdateretReciever, new IntentFilter(DRData.OPDATERINGSINTENT_Stamdata));
    registerReceiver(udsendelserOpdateretReciever, new IntentFilter(DRData.OPDATERINGSINTENT_Udsendelse));
    registerReceiver(spillerNuListeOpdateretReciever, new IntentFilter(DRData.OPDATERINGSINTENT_SpillerNuListe));

		TextView nextProgramHeaderTextView = (TextView) findViewById(R.id.player_next_program_textview);
		nextProgramHeaderTextView.setText(drdata.stamdata.s("text_footerTitle"));

    INGEN_LASTFM = getResources().getDrawable(R.drawable.nolastfm);
    try {
      visAktuelKanal();
      visAktuelUdsendelse();
      visNæsteUdsendelse();
      visSpillerNuInfo();
    } catch (Exception e) {
      Log.kritiskFejlStille(e);
    }

    visStartStopKnap();

		// Volumen op/ned skal styre lydstyrken af medieafspilleren, uanset som noget spilles lige nu eller ej
		setVolumeControlStream(AudioManager.STREAM_MUSIC);



    // Vis korrekt knap og/eller start afspilning
    boolean startAfspilningMedDetSammme = prefs.getBoolean("startAfspilningMedDetSammme", false);
    if (startAfspilningMedDetSammme && afspiller.getAfspillerstatus() == Afspiller.STATUS_STOPPET)
      startAfspilning();
    else visStartStopKnap();


    afspiller.addAfspillerListener(Afspilning_akt.this);
    afspiller.addAfspillerListener(drdata.rapportering);
	}


  // For Android 1.6-kompetibilitet bruger vi ikke onBackPressed()
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    if (keyCode != KeyEvent.KEYCODE_BACK)
    {
    	return super.onKeyDown(keyCode, event);
    }

    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    int volumen = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

    //boolean lukAfspillerServiceVedAfslutning = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lukAfspillerServiceVedAfslutning", false);

    // Hvis der er skruet helt ned så stop afspilningen
    if (volumen==0 && afspiller.afspillerstatus != Afspiller.STATUS_STOPPET)
    {
      afspiller.stopAfspilning();
    }

    finish();
    return true;
  }



	@Override
	protected void onDestroy()
	{
    afspiller.addAfspillerListener(Afspilning_akt.this);
    afspiller.addAfspillerListener(drdata.rapportering);
    unregisterReceiver(stamdataOpdateretReciever);
    unregisterReceiver(udsendelserOpdateretReciever);
    unregisterReceiver(spillerNuListeOpdateretReciever);
		super.onDestroy();
	}


  AlertDialog internetforbindelseManglerDialog;

	@Override
	protected void onResume()
	{
		// se om vi er online
		boolean connectionOK = Network.testConnection(getApplicationContext()) ;
		if( connectionOK )
		{
			// hurra - opdater data fra server
			drdata.setBaggrundsopdateringAktiv(true);
		}
		else
		{
			// Informer brugeren hvis vi er offline
		  if (internetforbindelseManglerDialog == null)
		  {
		    internetforbindelseManglerDialog = new AlertDialog.Builder(this).create() ;
		    internetforbindelseManglerDialog.setTitle("Internetforbindelse mangler") ;
		    internetforbindelseManglerDialog.setMessage("Din telefon er ikke tilsluttet internettet. For at høre radio skal du åbne op for forbindelser via WiFI eller mobil data.") ;
		  }
		  if (!internetforbindelseManglerDialog.isShowing())
		  {
			  internetforbindelseManglerDialog.show();
		  }
		}
		super.onResume();
	}


	@Override
	protected void onPause()
	{
		drdata.setBaggrundsopdateringAktiv(false);
		super.onPause();
	}


	private void initControls() {

    currentProgramTitleTextView = (TextView) findViewById(R.id.player_current_program_title_textview);
    currentProgramDescriptionTextView = (TextView) findViewById(R.id.player_current_program_description_textview);
		currentChannelTextView = (TextView) findViewById(R.id.player_current_program_channel_textview);
    nextProgramTitleTextView = (TextView) findViewById(R.id.player_next_program_title_textview);
		tracksLinearLayout = (LinearLayout) findViewById(R.id.player_tracks_linearlayout);
		status = (TextView) findViewById(R.id.status);

    try { // DRs skrifttyper er ikke offentliggjort i SVN, derfor kan følgende fejle:
      Typeface skrift_DRiRegular = Typeface.createFromAsset(getAssets(),"DRiRegular.otf");
      status.setTypeface(skrift_DRiRegular);
    } catch (Exception e) {
      Log.e("DRs skrifttyper er ikke tilgængelige", e);
    }

		Button selectChannelButton = (Button) findViewById(R.id.player_select_channel_button);
		selectChannelButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				startActivityForResult(new Intent(Afspilning_akt.this, Kanalvalg_akt.class),117);
			}
		});

		playStopButton = (ImageButton) findViewById(R.id.start_stop_knap);
		playStopButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (afspiller.getAfspillerstatus() == Afspiller.STATUS_STOPPET)
				{
					startAfspilning();
				} else {
					stopAfspilning();
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

		previousImageView = (ImageView)findViewById(R.id.previous);
		previousImageView.setAlpha(200);
		previousImageView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_UP) flipPreviousIfNotLastTrack();
				return true;
			}
		});

		nextImageView = (ImageView)findViewById(R.id.next);
		nextImageView.setAlpha(200);
		nextImageView.setVisibility(ImageButton.GONE);
		nextImageView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_UP) flipNextIfNotFirstTrack();;
				return true;
			}
		});


    final GestureDetector gestureDetector = new GestureDetector(new MyGestureDetector());
    flipper = (ViewFlipper) findViewById(R.id.flipper);
    flipper.setOnTouchListener(new OnTouchListener() {
      public boolean onTouch(View v, MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
          return false;
        } else {
          return true;
        }
      }
    });
	}


	/** Denne metode kaldes når der er valgt en kanal */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("onActivityResult "+requestCode+" "+resultCode+" "+data);
		// requestCode er 117
		if (resultCode != RESULT_OK) return;  // Hvis brugeren trykkede på tilbage-knappen eller valgte den samme kanal

    // Afspilning skal starte så når brugeren har valgt kanal
    // sætKanal();
    startAfspilning();

    // Kald for at nulstille skærmen
    try {
      visAktuelUdsendelse();
      visNæsteUdsendelse();
      visSpillerNuInfo();
    } catch (Exception e) {
      Log.kritiskFejl(this, e);
    }
	}


	private void startAfspilning() {
		Log.d("startAfspilning");
		try {
			sætKanal();
      afspiller.startAfspilning();
      drdata.rapportering.afspilningForsøgtStartet = System.currentTimeMillis();
      visStartStopKnap();
		} catch (Exception e) {
			Log.e(e);
		}
	}

  private void sætKanal() {
    String url = drdata.findKanalUrlFraKode(drdata.aktuelKanal);

    afspiller.setKanal(drdata.aktuelKanal.longName, url);
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
  if (afspiller==null) return;
    if (afspiller.getAfspillerstatus() == Afspiller.STATUS_STOPPET) {
      playStopButton.setImageResource(R.drawable.play);
    } else {
      playStopButton.setImageResource(R.drawable.stop);
    }
  }

	private void sætForbinderProcent(int procent)
	{
		Log.d( "sætforbinderProcent( " + procent + " )" );

    int afspillerstatus = afspiller.getAfspillerstatus();

		if (procent <= 0)
		{
      if (afspillerstatus == Afspiller.STATUS_FORBINDER) {
        visStatus("Forbinder...");
      }
      else visStatus("");
		} else {
			visStatus("Forbinder... " + procent + "%");
		}
	}

	public void onAfspilningStartet() {
		Log.d( "onAfspilningStartet()" ) ;
		//startStopButton.setImageResource(R.drawable.pause_white);
		visStatus("Afspiller");
    visStartStopKnap();
	}

	public void onAfspilningStoppet() {
		Log.d( "onAfspilningStoppet()" ) ;
		//startStopButton.setImageResource(R.drawable.play_white);
		visStatus("Stoppet");
    visStartStopKnap();
    // Rapportering
    if (Log.RAPPORTER_VELLYKKET_AFSPILNING) {
      String rapNøgle = "rapport_"+ drdata.rapportering.lydformat;
      boolean rapporteret = prefs.getBoolean(rapNøgle, false);
      if (!rapporteret) {
        String rapport = drdata.rapportering.rapport();
        if (rapport != null) {
          Log.d("Indsender rapport: "+rapport);
          ErrorReporter.getInstance().putCustomData(drdata.rapportering.lydformat, rapport);
          ErrorReporter.getInstance().handleSilentException(null);
          prefs.edit().putBoolean(rapNøgle, true).commit();
          if (DRData.udvikling) Toast.makeText(this, "Sender rapport for "+drdata.rapportering.lydformat, Toast.LENGTH_LONG).show();
        }
      }
    }
    if (DRData.udvikling) {
      String rapport = drdata.rapportering.rapport();
      if (rapport != null) Toast.makeText(this, rapport, Toast.LENGTH_LONG).show();
    }
	}

	public void onAfspilningForbinder(int procent) {
		if (procent >= 100) {
			onAfspilningStartet();
		} else {
			//startStopButton.setImageResource(R.drawable.buffer_white);
			sætForbinderProcent(procent);
		}
	}


  private BroadcastReceiver stamdataOpdateretReciever = new BroadcastReceiver() {
    @Override
    public void onReceive(Context ctx, Intent i) {
      Log.d("stamdataOpdateretReciever");

      final String NØGLE = "drift_statusmeddelelse";
      final String drift_statusmeddelelse = drdata.stamdata.s(NØGLE);
      // Tjek i prefs om denne drifmeddelelse allerede er vist.
      // Der er 1 ud af en millards chance for at hashkoden ikke er ændret, den risiko tør vi godt løbe
      final int nyHashkode = drift_statusmeddelelse.hashCode();
      final int gammelHashkode = prefs.getInt(NØGLE, 0);
      Log.d("drift_statusmeddelelse='"+drift_statusmeddelelse + "' nyHashkode="+nyHashkode+" gammelHashkode="+gammelHashkode);
      if (gammelHashkode != nyHashkode && !"".equals(drift_statusmeddelelse)) { // Driftmeddelelsen er ændret. Vis den...
        AlertDialog.Builder dialog=new AlertDialog.Builder(Afspilning_akt.this);
        dialog.setMessage(drift_statusmeddelelse);
        dialog.setPositiveButton("OK", new AlertDialog.OnClickListener() {
          public void onClick(DialogInterface arg0, int arg1) {
            prefs.edit().putInt(NØGLE,  nyHashkode).commit(); // ...og gem ny hashkode i prefs
          }
        });
        dialog.show();
      }
    }
  };



  private BroadcastReceiver udsendelserOpdateretReciever = new BroadcastReceiver() {
    @Override
    public void onReceive(Context ctx, Intent i) {
      //Log.d("udsendelserOpdateretReciever");
      try {
        visAktuelUdsendelse();
        visNæsteUdsendelse();
      } catch (Exception e) {
        Log.kritiskFejlStille(e);
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

      if (drdata.spillerNuListe != null && drdata.spillerNuListe.liste.size()>0) {
        spillerNu = String.valueOf(drdata.spillerNuListe.liste.get(0).title);
      }

      if (!forrigeSpillerNu.equals(spillerNu)) {
        forrigeSpillerNu = spillerNu;
        Log.d("spillerNuListeOpdateretReciever opdatering "+spillerNu);
        visSpillerNuInfo();
      }
    }
  };


	private void visAktuelKanal() {
		String kanal = drdata.aktuelKanalkode;

		ImageView aktuelKanalImageView = (ImageView) findViewById(R.id.player_select_channel_billede);
		TextView aktuelKanalTextView = (TextView) findViewById(R.id.player_select_channel_text);

		Resources res = getResources();
		int id = res.getIdentifier("kanal_"+kanal.toLowerCase(), "drawable", getPackageName());

		if(id == 0) {
			String visningsnavn = drdata.stamdata.kanalkodeTilKanal.get(kanal).longName;
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
		String aboveHeader = drdata.stamdata.s("text_aboveHeader");
		Kanal kanal = drdata.aktuelKanal;
    String tekst = aboveHeader + " " + kanal.longName + ":";

		currentChannelTextView.setText(tekst);

		if (drdata.udsendelser_ikkeTilgængeligt) {
			currentProgramTitleTextView.setText("Kunne ikke hente programinfo");
			currentProgramDescriptionTextView.setText("");
    } else if(drdata.udsendelser == null || drdata.udsendelser.currentProgram == null) {
			currentProgramTitleTextView.setText("Venter på programinfo");
			currentProgramDescriptionTextView.setText("");
    } else {

      String currentProgamTitle = drdata.udsendelser.currentProgram.title;
			currentProgramTitleTextView.setText(currentProgamTitle);

			String currentProgamDescription = drdata.udsendelser.currentProgram.description;

      //Drop klipning af tekst og lad layout om at begrænse tekstens størrelse
			// fixet ved at lave et scrollview i afspilning_akt.xml - frederik
			/*
			String descriptionMaxLength ;
			if(isFlipperDisplayed()) {
				descriptionMaxLength = drdata.stamdata.s("program_descMaxLengthWithTrack");
			}
			else {
				descriptionMaxLength = drdata.stamdata.s("program_descMaxLength");
			}
      Log.d(currentProgamDescription);
			currentProgamDescription = StringUtil.limitString(currentProgamDescription, descriptionMaxLength);
			*/
			currentProgramDescriptionTextView.setText(Html.fromHtml(currentProgamDescription));

			// Konverter tekst-links i programinfo til rigtige links - frederik
			Linkify.addLinks(currentProgramDescriptionTextView, Linkify.WEB_URLS ) ;
    	}
	}


	private void visNæsteUdsendelse() {

		if (drdata.udsendelser!=null && drdata.udsendelser.nextProgram != null) {
			String nextProgramTitle = drdata.udsendelser.nextProgram.title;
			String descriptionMaxLength = drdata.stamdata.s("program_nextProgramTitleMaxLength");
			String nextProgramTitleLimited = StringUtil.limitString(nextProgramTitle, descriptionMaxLength);
			String nextProgramStartTime = StringUtil.getTimeFromDate(drdata.udsendelser.nextProgram.start);
			String nextProgramStopTime = StringUtil.getTimeFromDate(drdata.udsendelser.nextProgram.stop);
			String nextProgramTime = "(" + nextProgramStartTime + " - " + nextProgramStopTime + ")";
			nextProgramTitleTextView.setText(nextProgramTitleLimited + " " + nextProgramTime );
		} else {
			nextProgramTitleTextView.setText("Ingen programinfo");
		}
	}


	//@SuppressWarnings("unchecked")
	private void visSpillerNuInfo() {

    boolean showChannel = drdata.stamdata.kanalerDerSkalViseSpillerNu.contains(drdata.aktuelKanalkode);

    if (!showChannel || drdata.spillerNuListe==null ||
            drdata.spillerNuListe.liste==null || drdata.spillerNuListe.liste.isEmpty()) {
			tracksLinearLayout.setVisibility(LinearLayout.GONE);
      return;
    }

    tracksLinearLayout.setVisibility(LinearLayout.VISIBLE);

		final List <SpillerNuElement> tracks =new ArrayList<SpillerNuElement>(drdata.spillerNuListe.liste);


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

    handlePreviousNextDisplay();


    // Da getImagefromUrl(track.lastFM) skal hente over netværket skal det ske i baggrunden
    synchronized (flipper) {
      if (indlæsningAfBilleder!=null) indlæsningAfBilleder.cancel(false);
    }

    indlæsningAfBilleder = new AsyncTask() {
      @Override
      protected Object doInBackground(Object... arg0) {
        int index = 0;
        for (SpillerNuElement track : tracks) {

          Drawable drawable = getImagefromUrl(track.lastFM);
          if (isCancelled()) break;

          publishProgress(index, drawable);
          index++;
        }
        synchronized (flipper) {
          indlæsningAfBilleder=null;
        }
        return null;
      }
      protected void onProgressUpdate(Object[] values) {
        if (isCancelled() || flipper==null) return; // Nødvendigt da GUI-trådens udførelse kan være forsinket. Jacob 21/11 2011
        View view = flipper.getChildAt((Integer) values[0]);
        Drawable drawable = (Drawable) values[1];
        ImageView flipperImageImageView = (ImageView) view.findViewById(R.id.FlipperImageImageView);
        flipperImageImageView.setImageDrawable(drawable);
        // Vis/skjul teksten "Last.fm"
        View flipperLastFmTextTextView = view.findViewById(R.id.FlipperLastFmTextTextView);
        if (drawable != INGEN_LASTFM) {
          flipperLastFmTextTextView.setVisibility(View.VISIBLE);
        } else {
          flipperLastFmTextTextView.setVisibility(View.INVISIBLE);
        }
      }
    }.execute();
	}


	WeakHashMap<String,Drawable> urlTilDrawable = new WeakHashMap<String,Drawable>();
  Drawable INGEN_LASTFM;

	private Drawable getImagefromUrl(String url) {
		Drawable d = urlTilDrawable.get(url);
		if (d != null) return d;

		Bitmap bitmap = ImageUtil.downloadImage(url);
		if(bitmap != null) {
			d = new BitmapDrawable(bitmap);
		}
		else {
			d = INGEN_LASTFM;
		}

		urlTilDrawable.put(url, d);
		return d;
	}

	private View buildViewFromTrack(SpillerNuElement track, int index) {
		LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = mInflater.inflate(R.layout.flipper_internallayout, null);

		LinearLayout flipperLinearLayout = (LinearLayout) view.findViewById(R.id.FlipperLinearLayout);

		TextView flipperLastFmTextTextView = (TextView) view.findViewById(R.id.FlipperLastFmTextTextView);
		String lastFm = drdata.stamdata.s("text_trackPhoto");
		flipperLastFmTextTextView.setText(lastFm);

		TextView flipperTrackTitleHeaderTextView = (TextView) view.findViewById(R.id.FlipperTrackTitleHeaderTextView);

		int numberOfTracks = drdata.spillerNuListe.liste.size();
    String trackHeader;
		if (index == 0) trackHeader = drdata.stamdata.s("text_trackLast");
		else if (index == numberOfTracks - 1) trackHeader = drdata.stamdata.s("text_trackBeforeLast");
		else trackHeader = drdata.stamdata.s("text_trackPrevious");

		flipperTrackTitleHeaderTextView.setText(trackHeader);

		TextView flipperTrackTitleTextView = (TextView) view.findViewById(R.id.FlipperTrackTitleTextView);
		flipperTrackTitleTextView.setText(track.title);

		TextView flipperTrackDisplayArtistTextView = (TextView) view.findViewById(R.id.FlipperTrackDisplayArtistTextView);
		flipperTrackDisplayArtistTextView.setText(track.displayArtist);

		TextView flipperTrackStartTimeTextView = (TextView) view.findViewById(R.id.FlipperTrackStartTimeTextView);
		flipperTrackStartTimeTextView.setText("Afspillet kl. "+ StringUtil.getTimeFromDate(track.start));

		return flipperLinearLayout;
	}

	private boolean isFlipperDisplayed() {
		return tracksLinearLayout.getVisibility() == LinearLayout.VISIBLE;
	}

	private void flipNextIfNotFirstTrack() {
		if(flipper.getDisplayedChild() != 0) {
			flipper.setInAnimation(AnimationUtil.inFromRightAnimation());
			flipper.setOutAnimation(AnimationUtil.outToLeftAnimation());
			flipper.showNext();

			if(flipper.getDisplayedChild() == 0) {
				previousImageView.setVisibility(ImageView.VISIBLE);
				nextImageView.setVisibility(ImageView.GONE);
			}
			else {
				previousImageView.setVisibility(ImageView.VISIBLE);
				nextImageView.setVisibility(ImageView.VISIBLE);
			}
		}
		handlePreviousNextDisplay();
	}

	private void  flipPreviousIfNotLastTrack() {
		if(flipper.getDisplayedChild() != 1) {

			flipper.setInAnimation(AnimationUtil.inFromLeftAnimation());
			flipper.setOutAnimation(AnimationUtil.outToRightAnimation());
			flipper.showPrevious();

			if(flipper.getDisplayedChild() == 1) {
				previousImageView.setVisibility(ImageView.GONE);
				nextImageView.setVisibility(ImageView.VISIBLE);
			}
			else {
				previousImageView.setVisibility(ImageView.VISIBLE);
				nextImageView.setVisibility(ImageView.VISIBLE);
			}
		}
		handlePreviousNextDisplay();
	}

	public void handlePreviousNextDisplay() {
		if(previousNextHandler != null ) {
			previousNextHandler.removeCallbacks(previousNextRunner);
		}
		previousNextHandler = new Handler();
		previousNextRunner = new Runnable() {
			public void run() {
				previousImageView.setVisibility(ImageView.GONE);
				nextImageView.setVisibility(ImageView.GONE);
			}
		};
		previousNextHandler.postDelayed(previousNextRunner, 5000);
	}


	class MyGestureDetector extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 50;//120;
		private static final int SWIPE_MAX_OFF_PATH = 250;
		private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    @Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			Log.d(" in onFling() :: "+e1+"   "+e2);
			if (e1==null || e2==null) return false; // Det sker nogen gange at en af dem er null
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return false;
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				flipNextIfNotFirstTrack();

			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				flipPreviousIfNotLastTrack();
			}
			return super.onFling(e1, e2, velocityX, velocityY);
		}
	}


	/** Håndtering af MENU-knappen */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 101, Menu.NONE, "Om").setIcon(android.R.drawable.ic_menu_help);
		menu.add(Menu.NONE, 102, Menu.NONE, "Indstillinger").setIcon(android.R.drawable.ic_menu_preferences);
    /*
		menu.add(Menu.NONE, 103, Menu.NONE, "SR P1 rtsp");
		menu.add(Menu.NONE, 105, Menu.NONE, "DR P3 rtsp");
     */
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
	      case 101:
	        startActivity(new Intent(this, Om_DRRadio_akt.class));
	        break;
	      case 102:
	        startActivity(new Intent(this, Indstillinger_akt.class));
	        break;
/*
	      case 103:
	        Kanal k = new Kanal();
	        k.setLongName("SR p1-aac-96", "rtsp://mobil-live.sr.se/mobilradio/kanaler/p1-aac-96");
	    		afspillerService.setKanal(k);
	        break;
	      case 105:
	        k = new Kanal("DR rtsp", "rtsp://live-rtsp.dr.dk:1935/rtplive/_definst_/Channel5_LQ.stream");
	    		afspillerService.setKanal(k);
	        break;
           *
           */
	    }
		return true;
	}


}