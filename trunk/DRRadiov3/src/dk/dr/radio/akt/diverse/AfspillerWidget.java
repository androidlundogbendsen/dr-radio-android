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

package dk.dr.radio.akt.diverse;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Arrays;

import dk.dr.radio.afspilning.AfspillerReciever;
import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

@SuppressLint("NewApi")
public class AfspillerWidget extends AppWidgetProvider {


  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(this + " onReceive(" + intent);
    super.onReceive(context, intent);
  }


  /**
   * Kaldes når ikonet oprettes
   */
  @Override
  public void onUpdate(Context ctx, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    Log.d(this + " onUpdate (levende ikon oprettet) - appWidgetIds = " + Arrays.toString(appWidgetIds));
    // for sørge for at vores knapper får tilknyttet intentsne
    opdaterUdseende(ctx, appWidgetManager, appWidgetIds[0]);
  }


  public static void opdaterUdseende(Context ctx, AppWidgetManager appWidgetManager, int appWidgetId) {
    Log.d("AfspillerWidget opdaterUdseende()");

    boolean låseskærm = false;

    if (Build.VERSION.SDK_INT >= 16) {
      Bundle o = appWidgetManager.getAppWidgetOptions(appWidgetId);
      //App.langToast("opdaterUdseende opts=" + o);
      låseskærm = o.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
      //App.langToast("opdaterUdseende låseskærm=" + låseskærm);
    }

    RemoteViews remoteViews = lavRemoteViews(låseskærm, false);

    appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
  }

  public static RemoteViews lavRemoteViews(boolean låseskærm, boolean notifikation) {


    RemoteViews remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspillerwidget_binh);

    Intent startStopI = new Intent(App.instans, AfspillerReciever.class);
    PendingIntent pi = PendingIntent.getBroadcast(App.instans, 0, startStopI, PendingIntent.FLAG_UPDATE_CURRENT);
    remoteViews.setOnClickPendingIntent(R.id.startStopKnap, pi);


    Intent åbnAktivitetI = new Intent(App.instans, Hovedaktivitet.class);
    PendingIntent pi2 = PendingIntent.getActivity(App.instans, 0, åbnAktivitetI, PendingIntent.FLAG_UPDATE_CURRENT);
    remoteViews.setOnClickPendingIntent(R.id.yderstelayout, pi2);


    if (DRData.instans != null) {
      Resources res = App.instans.getResources();
      String kanalkode = DRData.instans.aktuelKanal.kode;
      // tjek om der er et billede i 'drawable' med det navn filnavn
      int id = res.getIdentifier("kanal_" + kanalkode.toLowerCase(), "drawable", App.instans.getPackageName());


      if (id != 0) {
        // Element med billede
        remoteViews.setViewVisibility(R.id.kanalnavn, View.VISIBLE/*.GONE*/);
        remoteViews.setViewVisibility(R.id.billede, View.VISIBLE);
        remoteViews.setImageViewResource(R.id.billede, id);
      } else {
        // Element uden billede
        remoteViews.setViewVisibility(R.id.kanalnavn, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.billede, View.VISIBLE/*.GONE*/);
        //remoteViews.setTextViewText(R.id.kanalnavn, DRData.instans.aktuelKanal.navn);
      }

      Lydkilde lk = DRData.instans.afspiller.getLydkilde();
      Log.d("Binh: kanal overskrift: " + lk);
      if (lk == null) {
      } else if (lk instanceof Udsendelse) {
        remoteViews.setTextViewText(R.id.udsendelseTitel, lk.getUdsendelse().titel);
        remoteViews.setTextViewText(R.id.kanalnavn, lk.kanal().navn);
      } else if (lk instanceof Kanal) {
        remoteViews.setTextViewText(R.id.udsendelseTitel, lk.getUdsendelse().titel);
        remoteViews.setTextViewText(R.id.kanalnavn, lk.kanal().navn);
      }


      Status afspillerstatus = DRData.instans.afspiller.getAfspillerstatus();

      if (afspillerstatus == Status.STOPPET) {
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_start);
        remoteViews.setViewVisibility(R.id.progressBar, View.INVISIBLE);
      } else if (afspillerstatus == Status.FORBINDER) {
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_stop);
        remoteViews.setViewVisibility(R.id.progressBar, View.VISIBLE);
      } else if (afspillerstatus == Status.SPILLER) {
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_stop);
        remoteViews.setViewVisibility(R.id.progressBar, View.INVISIBLE);
      } else {
        Log.e(new Exception("Ugyldig afspillerstatus: " + afspillerstatus));
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.kanalvalg_minus);
        remoteViews.setViewVisibility(R.id.progressBar, View.INVISIBLE);
      }
    } else {
      // Ingen instans eller service oprettet - dvs afspiller kører ikke
      remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_start);
      remoteViews.setViewVisibility(R.id.progressBar, View.INVISIBLE);
      remoteViews.setViewVisibility(R.id.kanalnavn, View.GONE);
      remoteViews.setViewVisibility(R.id.billede, View.GONE);
      // Vis P3 i mangel af info om valgt kanal??
      //remoteViews.setImageViewResource(R.id.billede, R.drawable.kanal_p3);
    }
    return remoteViews;
  }
}
