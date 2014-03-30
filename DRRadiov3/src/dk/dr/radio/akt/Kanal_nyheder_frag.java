package dk.dr.radio.akt;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.androidquery.AQuery;

import org.json.JSONObject;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.volley.DrVolleyResonseListener;
import dk.dr.radio.diverse.volley.DrVolleyStringRequest;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Kanal_nyheder_frag extends Basisfragment implements View.OnClickListener, Runnable {

  private Kanal kanal;
  protected View rod;
  private boolean fragmentErSynligt;
  private AQuery aq;

  @Override
  public String toString() {
    return super.toString() + "/" + kanal;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(this + " onCreateView startet efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    String kanalkode = getArguments().getString(P_kode);
    kanal = DRData.instans.grunddata.kanalFraKode.get(kanalkode);
    rod = inflater.inflate(R.layout.kanal_nyheder_frag, container, false);
    aq = new AQuery(rod);

    aq.id(R.id.hør_live).typeface(App.skrift_gibson).clicked(this);
    // Knappen er meget vigtig, og har derfor et udvidet område hvor det også er den man rammer
    // se http://developer.android.com/reference/android/view/TouchDelegate.html
    final int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);
    final View hør = aq.id(R.id.hør_live).getView();
    hør.post(new Runnable() {
      @Override
      public void run() {
        Rect r = new Rect();
        hør.getHitRect(r);
        r.top -= udvid;
        r.bottom += udvid;
        r.right += udvid;
        r.left -= udvid;
        //Log.d("hør_udvidet_klikområde=" + r);
        ((View) hør.getParent()).setTouchDelegate(new TouchDelegate(r, hør));
      }
    });



    // Hent streams
    App.forgrundstråd.postDelayed(this, 500);
    udvikling_checkDrSkrifter(rod, this + " rod");
    DRData.instans.afspiller.observatører.add(this);
    App.netværk.observatører.add(this);
    Log.d(this + " onCreateView færdig efter " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    return rod;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    DRData.instans.afspiller.observatører.remove(this);
    App.netværk.observatører.remove(this);

  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    Log.d(kanal + " QQQ setUserVisibleHint " + isVisibleToUser + "  " + this);
    if (kanal == null) return;
    fragmentErSynligt = isVisibleToUser;
    if (fragmentErSynligt) {
      run();
      App.forgrundstråd.post(new Runnable() {
        @Override
        public void run() {
          //scrollTilAktuelUdsendelse();
          if (DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET && DRData.instans.afspiller.getLydkilde() != kanal) {
            DRData.instans.afspiller.setLydkilde(kanal);
          }
        }
      });
    } else {
      App.forgrundstråd.removeCallbacks(this);
    }
    super.setUserVisibleHint(isVisibleToUser);
  }

  @Override
  public void onPause() {
    super.onPause();
    App.forgrundstråd.removeCallbacks(this);
    Log.d(this + " onPause() " + this);
  }

  @Override
  public void run() {
    App.forgrundstråd.removeCallbacks(this);
    App.forgrundstråd.postDelayed(this, 15000);

    if (kanal.streams == null && App.erOnline()) {
      Request<?> req = new DrVolleyStringRequest(kanal.getStreamsUrl(), new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
          if (uændret) return; // ingen grund til at parse det igen
          JSONObject o = new JSONObject(json);
          kanal.slug = o.getString(DRJson.Slug.name());
          DRData.instans.grunddata.kanalFraSlug.put(kanal.slug, kanal);
          kanal.streams = DRJson.parsStreams(o.getJSONArray(DRJson.Streams.name()));
          Log.d("hentSupplerendeDataBg " + kanal.kode + " fraCache=" + fraCache + " => " + kanal.slug + " k.lydUrl=" + kanal.streams);
        }
      }) {
        public Priority getPriority() {
          return fragmentErSynligt ? Priority.HIGH : Priority.NORMAL;
        }
      };
      App.volleyRequestQueue.add(req);
    }
    boolean spillerDenneKanal = DRData.instans.afspiller.getAfspillerstatus() != Status.STOPPET && DRData.instans.afspiller.getLydkilde() == kanal;
    boolean online = App.netværk.erOnline();
    aq.id(R.id.hør_live).enabled(!spillerDenneKanal && online && kanal.streams != null)
        .text(!online ? "Internetforbindelse mangler" :
            (spillerDenneKanal ? " SPILLER " : " HØR ") + kanal.navn.toUpperCase() + " LIVE");

  }



  @Override
  public void onClick(View v) {
    if (kanal.streams == null) {
      Log.rapporterOgvisFejl(getActivity(), new IllegalStateException("kanal.streams er null"));
    } else {
      // hør_udvidet_klikområde eller hør
      Kanal_frag.hør(kanal, getActivity());
      Log.registrérTestet("Afspilning af seneste radioavis", "ja");
    }
  }
}

