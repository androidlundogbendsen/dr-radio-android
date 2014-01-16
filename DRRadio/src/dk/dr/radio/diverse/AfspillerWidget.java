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

import dk.dr.radio.Afspilning_akt;
import dk.dr.radio.R;
import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.AfspillerReciever;
import dk.dr.radio.data.DRData;
import dk.dr.radio.util.Log;

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

    try {
      // Instans indlæses så vi kender kanalen
      DRData.tjekInstansIndlæst(ctx);

      // for sørge for at vores knapper får tilknyttet intentsne
      opdaterUdseende(ctx, appWidgetManager, appWidgetIds[0]);
    } catch (Exception ex) {
      Log.rapporterFejl(ex);
    }
  }

/*
  @Override
  public void onDeleted(Context ctx, int[] appWidgetIds) {
    Log.d(this+" onDeleted( widgetId="+widgetId);
    if (widgetId != -1) try {
      Context actx = ctx.getApplicationContext();
      actx.unregisterReceiver(afspillerServiceReciever);
    } catch (Exception e) { Log.e(e); }// Er ikke set ske, men for en sikkerheds skyld
    widgetId = -1;
    super.onDeleted(ctx, appWidgetIds);
  }
*/


  public static void opdaterUdseende(Context ctx, AppWidgetManager appWidgetManager, int appWidgetId) {
    Log.d("AfspillerWidget opdaterUdseende()");
    RemoteViews updateViews = new RemoteViews(ctx.getPackageName(), R.layout.afspillerwidget);

    Intent startStopI = new Intent(ctx, AfspillerReciever.class);
    startStopI.putExtra("flag", Afspiller.WIDGET_START_ELLER_STOP);
    PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, startStopI, PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.startStopKnap, pi);


    Intent åbnAktivitetI = new Intent(ctx, Afspilning_akt.class);
    PendingIntent pi2 = PendingIntent.getActivity(ctx, 0, åbnAktivitetI, PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.yderstelayout, pi2);


    /*
    boolean visProgressbar = false;
    boolean visKanalnavn = false;
    boolean visKanaltekst = false;
    int startStopKnapResId = R.drawable.widget_radio_play;
     */

    DRData drData = DRData.instans;
    if (drData != null) {
      Resources res = ctx.getResources();
      String kanalkode = drData.aktuelKanalkode;
      // tjek om der er et billede i 'drawable' med det navn filnavn
      int id = res.getIdentifier("kanal_" + kanalkode.toLowerCase(), "drawable", ctx.getPackageName());


      if (id != 0) {
        // Element med billede
        updateViews.setViewVisibility(R.id.kanalnavn, View.GONE);
        updateViews.setViewVisibility(R.id.billede, View.VISIBLE);
        updateViews.setImageViewResource(R.id.billede, id);
      } else {
        // Element uden billede
        updateViews.setViewVisibility(R.id.kanalnavn, View.VISIBLE);
        updateViews.setViewVisibility(R.id.billede, View.GONE);
        updateViews.setTextViewText(R.id.kanalnavn, drData.aktuelKanal.longName);
      }


      int afspillerstatus = drData.afspiller.getAfspillerstatus();

      if (afspillerstatus == Afspiller.STATUS_STOPPET) {
        updateViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_radio_play);
        updateViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      } else if (afspillerstatus == Afspiller.STATUS_FORBINDER) {
        updateViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_radio_stop);
        updateViews.setViewVisibility(R.id.progressbar, View.VISIBLE);
      } else if (afspillerstatus == Afspiller.STATUS_SPILLER) {
        updateViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_radio_stop);
        updateViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      } else {
        Log.e(new Exception("Ugyldig afspillerstatus: " + afspillerstatus));
        updateViews.setImageViewResource(R.id.startStopKnap, R.drawable.icon_minus);
        updateViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      }
    } else {
      // Ingen instans eller service oprettet - dvs afspiller kører ikke
      updateViews.setImageViewResource(R.id.startStopKnap, R.drawable.widget_radio_play);
      updateViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
      updateViews.setViewVisibility(R.id.kanalnavn, View.GONE);
      updateViews.setViewVisibility(R.id.billede, View.GONE);
      // Vis P3 i mangel af info om valgt kanal??
      //updateViews.setImageViewResource(R.id.billede, R.drawable.kanal_p3);
    }

    appWidgetManager.updateAppWidget(appWidgetId, updateViews);
  }
}
