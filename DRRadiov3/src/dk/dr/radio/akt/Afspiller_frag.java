package dk.dr.radio.akt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidquery.AQuery;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.diverse.Basisfragment;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Afspiller_frag extends Basisfragment implements Runnable, View.OnClickListener {
  private AQuery aq;
  private ImageView start_stop_pauseknap;
  private ProgressBar progressbar;
  private TextView titel;
  private TextView metainformation;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    View rod = inflater.inflate(R.layout.afspiller_lille_frag, container, false);
    aq = new AQuery(rod);
    start_stop_pauseknap = aq.id(R.id.start_stop_pauseknap).clicked(this).getImageView();
    progressbar = aq.id(R.id.progressBar).getProgressBar();
    titel = aq.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
    metainformation = aq.id(R.id.metainformation).typeface(App.skrift_gibson).getTextView();
    DRData.instans.afspiller.observatører.add(this);
    DRData.instans.afspiller.forbindelseobservatører.add(this);
    run(); // opdatér views
    return rod;
  }

  @Override
  public void onDestroyView() {
    DRData.instans.afspiller.observatører.remove(this);
    DRData.instans.afspiller.forbindelseobservatører.remove(this);
    super.onDestroyView();
  }

  @Override
  public void run() {
    Lydkilde lydkilde = DRData.instans.afspiller.getLydkilde();
    if (lydkilde == null) lydkilde = DRData.instans.aktuelKanal;
    Kanal k = lydkilde.kanal();
    Status status = DRData.instans.afspiller.getAfspillerstatus();
    boolean live = lydkilde.erStreaming() && status != Status.STOPPET;
    Udsendelse udsendelse = lydkilde.getUdsendelse();
    titel.setText(udsendelse == null ? "" : udsendelse.titel);
    switch (status) {
      case STOPPET:
        start_stop_pauseknap.setImageResource(R.drawable.afspiller_spil);
        progressbar.setVisibility(View.INVISIBLE);
        metainformation.setTextColor(getResources().getColor(R.color.grå40));
        metainformation.setText(k.navn + (live ? " LIVE" : ""));
        break;
      case FORBINDER:
        start_stop_pauseknap.setImageResource(R.drawable.afspiller_pause);
        progressbar.setVisibility(View.VISIBLE);
        int fpct = DRData.instans.afspiller.getForbinderProcent();
        metainformation.setTextColor(getResources().getColor(R.color.blå));
        metainformation.setText("Forbinder " + (fpct > 0 ? fpct : ""));
        break;
      case SPILLER:
        start_stop_pauseknap.setImageResource(R.drawable.afspiller_pause);
        progressbar.setVisibility(View.INVISIBLE);
        metainformation.setTextColor(getResources().getColor(R.color.blå));
        metainformation.setText(k.navn + (live ? " LIVE" : ""));
        break;
    }
  }

  @Override
  public void onClick(View v) {
    if (DRData.instans.afspiller.afspillerstatus == Status.STOPPET) {
      DRData.instans.afspiller.startAfspilning();
    } else {
      DRData.instans.afspiller.stopAfspilning();
    }
  }
}

