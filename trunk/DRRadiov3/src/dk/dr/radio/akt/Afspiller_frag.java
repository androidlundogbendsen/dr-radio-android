package dk.dr.radio.akt;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidquery.AQuery;

import dk.dr.radio.afspilning.Status;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.DRJson;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

public class Afspiller_frag extends Basisfragment implements Runnable, View.OnClickListener {
  private AQuery aq;
  private ImageView startStopKnap;
  private ProgressBar progressbar;
  private TextView titel;
  private TextView metainformation;
  private ImageView udvidSkjulKnap;
  private View udvidSkjulOmråde;
  private View rod;
  private View indhold_overskygge;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d("Viser fragment " + this);
    rod = inflater.inflate(R.layout.afspiller_frag, container, false);
    aq = new AQuery(rod);
    rod.setOnClickListener(this); // Ved klik på baggrunden skal kanalforside eller aktuel udsendelsesside vises
    startStopKnap = aq.id(R.id.startStopKnap).clicked(this).getImageView();
    udvidSkjulKnap = aq.id(R.id.udvidSkjulKnap).getImageView();
    // udvid/skjul knap - hvis vi bruger en onClickListener får vi først  besked når knappen slippes.
    // I stedet viser/skjuler vi allerede når fingeren trykkes ned på viewet
    udvidSkjulKnap.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          udvidSkjulOmråde();
        }
        return false;
      }
    });

    udvidSkjulOmråde = aq.id(R.id.udvidSkjulOmråde).gone().getView();
    progressbar = aq.id(R.id.progressBar).getProgressBar();
    titel = aq.id(R.id.titel).typeface(App.skrift_gibson_fed).getTextView();
    metainformation = aq.id(R.id.metainformation).typeface(App.skrift_gibson).getTextView();
    // Knappen er meget vigtig, og har derfor et udvidet område hvor det også er den man rammer
    // se http://developer.android.com/reference/android/view/TouchDelegate.html
    final int udvid = getResources().getDimensionPixelSize(R.dimen.hørknap_udvidet_klikområde);
    startStopKnap.post(new Runnable() {
      @Override
      public void run() {
        Rect r = new Rect();
        startStopKnap.getHitRect(r);
        r.top -= udvid;
        r.bottom += udvid;
        r.right += udvid;
        r.left -= udvid;
        //Log.d("hør_udvidet_klikområde=" + r);
        ((View) startStopKnap.getParent()).setTouchDelegate(new TouchDelegate(r, startStopKnap));
      }
    });
    aq.id(R.id.forrige).clicked(this);
    aq.id(R.id.næste).clicked(this);
    DRData.instans.afspiller.observatører.add(this);
    DRData.instans.afspiller.forbindelseobservatører.add(this);
    DRData.instans.afspiller.positionsobservatører.add(this);
    run(); // opdatér views

    if (App.accessibilityManager.isEnabled()) setHasOptionsMenu(true);
    return rod;
  }

  @Override
  public void onDestroyView() {
    DRData.instans.afspiller.observatører.remove(this);
    DRData.instans.afspiller.forbindelseobservatører.remove(this);
    DRData.instans.afspiller.positionsobservatører.remove(this);
    super.onDestroyView();
  }


  int startStopKnapImageResource;
  int startStopKnapNyImageResource;

  @Override
  public void run() {
    Lydkilde lydkilde = DRData.instans.afspiller.getLydkilde();
    Kanal k = lydkilde.getKanal();
    if (k == null) return;
    Status status = DRData.instans.afspiller.getAfspillerstatus();
    if (lydkilde.erDirekte()) {
      titel.setText(k.navn + " Live");
    } else {
      Udsendelse udsendelse = lydkilde.getUdsendelse();
      titel.setText(udsendelse == null ? k.navn : udsendelse.titel);
    }
    switch (status) {
      case STOPPET:
        startStopKnapNyImageResource = R.drawable.afspiller_spil;
        startStopKnap.setContentDescription("Start afspilning");
        progressbar.setVisibility(View.INVISIBLE);
        metainformation.setText(k.navn);
        metainformation.setTextColor(App.color.grå40);
        break;
      case FORBINDER:
        startStopKnapNyImageResource = R.drawable.afspiller_pause;
        startStopKnap.setContentDescription("Stop afspilning");
        progressbar.setVisibility(View.VISIBLE);
        int fpct = DRData.instans.afspiller.getForbinderProcent();
        metainformation.setTextColor(App.color.blå);
        metainformation.setText("Forbinder " + (fpct > 0 ? fpct : ""));
        break;
      case SPILLER:
        startStopKnapNyImageResource = R.drawable.afspiller_pause;
        startStopKnap.setContentDescription("Stop afspilning");
        progressbar.setVisibility(View.INVISIBLE);
        metainformation.setTextColor(App.color.blå);
        metainformation.setText(k.navn);
        break;
    }
    if (startStopKnapImageResource != startStopKnapNyImageResource) {
      startStopKnapImageResource = startStopKnapNyImageResource;

      Animation anim;
      /*
      if (App.prefs.getBoolean("startStopKnapAnim", false)) {
        anim = new RotateAnimation(0f, 180f, 0, 0);
        //r.setStartOffset(1000);
        anim.setDuration(200);
        anim.setRepeatCount(1);
        anim.setRepeatMode(RotateAnimation.REVERSE);
        anim.setInterpolator(new AccelerateInterpolator());
      } else {
      */
        anim = new ScaleAnimation(1, 1.2f, 1, 1.2f, startStopKnap.getWidth() / 2, startStopKnap.getHeight() / 2);
        anim.setDuration(100);
        anim.setRepeatCount(1); // skalér ind og ud igen
        anim.setRepeatMode(Animation.REVERSE);
        anim.setInterpolator(new DecelerateInterpolator());
      //}
      anim.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
          startStopKnap.setImageResource(startStopKnapNyImageResource);
        }
      });
      startStopKnap.startAnimation(anim);
    }
    if (App.accessibilityManager.isEnabled() && getActivity() != null)
      ActivityCompat.invalidateOptionsMenu(getActivity());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    if (App.accessibilityManager.isEnabled()) {

      inflater.inflate(R.menu.tilg_afspiller, menu);
      MenuItem menuItem = menu.findItem(R.id.startStopKnap);

      if (DRData.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
        menuItem.setTitle("Start " + titel.getText());
      } else {
        menuItem.setTitle("Stop afspilning");
        menuItem.setIcon(R.drawable.dri_radio_stop_graa40);
      }
    }
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.startStopKnap) {
      if (DRData.instans.afspiller.afspillerstatus == Status.STOPPET) {
        DRData.instans.afspiller.startAfspilning();
      } else {
        DRData.instans.afspiller.stopAfspilning();
      }
    }
    return super.onOptionsItemSelected(item);
  }

  public boolean viserUdvidetOmråde() {
    return udvidSkjulOmråde.getVisibility() == View.VISIBLE;
  }

  /*
  Virkede med
      android:animateLayoutChanges="true"


    public void udvidSkjulOmråde() {
      final View indhold_overskygge = getActivity().findViewById(R.id.indhold_overskygge);
      if (viserUdvidetOmråde()) {
        udvidSkjulOmråde.setVisibility(View.GONE);
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_op_graa40);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          indhold_overskygge.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
              // Får det til at hakke midt i animationen
              // TODO fix, ved at lave egen åbne/lukke animation i afspiller, der ikke først fader og DEREFTER krympler området
              //indhold_overskygge.setVisibility(View.GONE);
            }
          });
        } else {
          indhold_overskygge.setVisibility(View.GONE);
        }
      } else {
        udvidSkjulOmråde.setVisibility(View.VISIBLE);
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_ned_graa40);

        indhold_overskygge.setOnTouchListener(new View.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            udvidSkjulOmråde(); // Skjul udvidet afspiller igen
            indhold_overskygge.setOnTouchListener(null);
            return true;
          }
        });
        indhold_overskygge.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          indhold_overskygge.animate().alpha(1);
        }
      }
    }
  */
  public void setIndholdOverskygge(View v) {
    indhold_overskygge = v;
    indhold_overskygge.setVisibility(View.GONE);
  }

  View.OnTouchListener indhold_overskygge_onTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      udvidSkjulOmråde(); // Skjul udvidet afspiller igen
      return true;
    }
  };

  public void udvidSkjulOmråde() {
    if (!viserUdvidetOmråde()) {
      indhold_overskygge.setOnTouchListener(indhold_overskygge_onTouchListener);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        indhold_overskygge.setVisibility(View.VISIBLE);
        udvidSkjulOmråde.setVisibility(View.VISIBLE);
        int højde = udvidSkjulOmråde.getHeight();
        //App.kortToast("højde " + højde);
        int højdeGæt = getResources().getDimensionPixelSize(R.dimen.afspiller_udvidet_højde_gæt);
        if (højde == 0) {
          //udvidSkjulKnap.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
          højde = højdeGæt;
          //App.kortToast("højde "+højde);
        } else {
          if (højdeGæt != højde) {
            Log.d("udvidSkjulOmråde(): højdeGæt på " + højdeGæt + " afviger fra reel højde på " + højde);
          }
        }
        rod.setTranslationY(højde);
        rod.animate().translationY(0);
        indhold_overskygge.animate().alpha(1);
        udvidSkjulKnap.animate().rotation(-180);
      } else {
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_ned_graa40);
        indhold_overskygge.setVisibility(View.VISIBLE);
        udvidSkjulOmråde.setVisibility(View.VISIBLE);
      }
    } else {
      indhold_overskygge.setOnTouchListener(null);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        rod.animate().translationY(udvidSkjulOmråde.getHeight());
        indhold_overskygge.animate().alpha(0).withEndAction(new Runnable() {
          @TargetApi(Build.VERSION_CODES.HONEYCOMB)
          @Override
          public void run() {
            indhold_overskygge.setVisibility(View.GONE);
            udvidSkjulOmråde.setVisibility(View.GONE);
            rod.setTranslationY(0);
          }
        });
        udvidSkjulKnap.animate().rotation(0);
      } else {
        udvidSkjulKnap.setImageResource(R.drawable.dri_pil_op_graa40);
        udvidSkjulOmråde.setVisibility(View.GONE);
        indhold_overskygge.setVisibility(View.GONE);
      }
    }
  }



  @Override
  public void onClick(View v) {
    if (v == startStopKnap) {
      if (DRData.instans.afspiller.afspillerstatus == Status.STOPPET) {
        DRData.instans.afspiller.startAfspilning();
      } else {
        DRData.instans.afspiller.stopAfspilning();
      }
    } else if (v.getId() == R.id.forrige) {
      DRData.instans.afspiller.forrige();
    } else if (v.getId() == R.id.næste) {
      DRData.instans.afspiller.næste();
    } else if (v == udvidSkjulKnap) {
      udvidSkjulOmråde();
    } else try {
      // Ved klik på baggrunden skal kanalforside eller aktuel udsendelsesside vises
      Lydkilde lydkilde = DRData.instans.afspiller.getLydkilde();
      FragmentManager fm = getFragmentManager();
      if (lydkilde.erDirekte()) {
        // Fjern backstak - så vi starter forfra i 'roden'
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // Vis kanaler (den aktuelle getKanal vælges automatisk af Kanaler_frag)
        fm.beginTransaction()
            .replace(R.id.indhold_frag, new Kanaler_frag())
            .commit();
      } else {
        Udsendelse udsendelse = lydkilde.getUdsendelse();
        Fragment f = new Udsendelse_frag();
        f.setArguments(new Intent()
            .putExtra(P_kode, lydkilde.getKanal().kode)
            .putExtra(DRJson.Slug.name(), udsendelse.slug).getExtras());
        //Forkert: getFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
        //Forkert: getChildFragmentManager().beginTransaction().replace(R.id.indhold_frag, f).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
        getActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.indhold_frag, f)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit();
      }
    } catch (Exception e) {
      Log.rapporterFejl(e);
    } // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/825688064
  }
}
