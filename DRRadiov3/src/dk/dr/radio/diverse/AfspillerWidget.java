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

package dk.dr.radio.diverse;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Arrays;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.AfspillerReciever;
import dk.dr.radio.akt_v3.Hovedaktivitet;
import dk.dr.radio.data.DRData;
import dk.dr.radio.v3.R;

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

    RemoteViews remoteViews = lavRemoteViews();

    appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
  }

  public static RemoteViews lavRemoteViews() {

    RemoteViews remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspillerwidget);

    Intent startStopI = new Intent(App.instans, AfspillerReciever.class);
    startStopI.putExtra("flag", Afspiller.WIDGET_START_ELLER_STOP);
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
        remoteViews.setViewVisibility(R.id.kanalnavn, View.GONE);
        remoteViews.setViewVisibility(R.id.billede, View.VISIBLE);
        remoteViews.setImageViewResource(R.id.billede, id);
      } else {
        // Element uden billede
        remoteViews.setViewVisibility(R.id.kanalnavn, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.billede, View.GONE);
        remoteViews.setTextViewText(R.id.kanalnavn, DRData.instans.aktuelKanal.navn);
      }


      int afspillerstatus = DRData.instans.afspiller.getAfspillerstatus();

      if (afspillerstatus == Afspiller.STATUS_STOPPET) {
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_start);
        remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      } else if (afspillerstatus == Afspiller.STATUS_FORBINDER) {
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_stop);
        remoteViews.setViewVisibility(R.id.progressbar, View.VISIBLE);
      } else if (afspillerstatus == Afspiller.STATUS_SPILLER) {
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_stop);
        remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      } else {
        Log.e(new Exception("Ugyldig afspillerstatus: " + afspillerstatus));
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.kanalvalg_minus);
        remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      }
    } else {
      // Ingen instans eller service oprettet - dvs afspiller kører ikke
      remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_afspilning_start);
      remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      remoteViews.setViewVisibility(R.id.kanalnavn, View.GONE);
      remoteViews.setViewVisibility(R.id.billede, View.GONE);
      // Vis P3 i mangel af info om valgt kanal??
      //remoteViews.setImageViewResource(R.id.billede, R.drawable.kanal_p3);
    }
    return remoteViews;
  }
}
