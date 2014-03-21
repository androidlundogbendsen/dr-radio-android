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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Arrays;

import dk.dr.radio.afspilning.AfspillerReciever;
import dk.dr.radio.afspilning.HoldAppIHukommelsenService;
import dk.dr.radio.afspilning.Status;
import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.data.DRData;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

@SuppressLint({ "NewApi", "ResourceAsColor" })
public class AfspillerIkonOgNotifikation extends AppWidgetProvider {


  private static int statusInt;

@Override
  public void onReceive(Context context, Intent intent) {
    Log.d(this + " onReceive(" + intent);
    //App.kortToast("AfspillerWidget onReceive");
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
    //App.langToast("AfspillerWidget opdaterUdseende()");
    
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

  @SuppressLint("ResourceAsColor")
public static RemoteViews lavRemoteViews(boolean låseskærm, boolean notifikation) {


    RemoteViews remoteViews;
    if (notifikation || låseskærm) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        // Kun lille layout på en linje understøttes
        remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_notifikation_lille);
      } else {
        if (låseskærm) {
          remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_laase_skaerm);
        } else {
          // A notification's big view appears only when the notification is expanded,
          // which happens when the notification is at the top of the notification drawer,
          // or when the user expands the notification with a gesture.
          // Expanded notifications are available starting with Android 4.1.
          remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_notifikation_stor1);
        }
      }
    } else {
      remoteViews = new RemoteViews(App.instans.getPackageName(), R.layout.afspiller_levendeikon);
    }

    Intent afspillerReceiverI = new Intent(App.instans, AfspillerReciever.class); 
    

    Intent hovedAktI = new Intent(App.instans, Hovedaktivitet.class);
    hovedAktI.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent åbnAktivitetPI = PendingIntent.getActivity(App.instans, 0, hovedAktI, PendingIntent.FLAG_UPDATE_CURRENT);
    remoteViews.setOnClickPendingIntent(R.id.yderstelayout, åbnAktivitetPI);
       

    //PendingIntent lukNotifikation = PendingIntent.getActivity(App.instans, 0, new Intent(App.instans, AfspillerReciever.class), PendingIntent.FLAG_UPDATE_CURRENT);
    //remoteViews.setOnClickPendingIntent(R.id.luk, startStopPI);

    /*
    int id = DRData.instans.afspiller.getLydkilde().kanal().kanallogo_resid;

    if (id != 0) {
      // Element med billede
      remoteViews.setViewVisibility(R.id.billede, View.VISIBLE);
      remoteViews.setImageViewResource(R.id.billede, id);
    } else {
      // Element uden billede
      remoteViews.setViewVisibility(R.id.kanalnavn, View.VISIBLE);
      //remoteViews.setTextViewText(R.id.kanalnavn, DRData.instans.aktuelKanal.navn);
    }
    */

    Lydkilde lydkilde = DRData.instans.afspiller.getLydkilde();
    Kanal k = lydkilde.kanal();
    Status status = DRData.instans.afspiller.getAfspillerstatus();
    String metainfo = "";
    //boolean live =  && status != Status.STOPPET;
    if (lydkilde.erStreaming()) {
      remoteViews.setTextViewText(R.id.titel, k.navn + " Live");
      remoteViews.setTextViewText(R.id.metainformation, k.navn);
      metainfo = k.navn;
    //  App.kortToast(" Live "  + k.navn);
    } else {
      Udsendelse udsendelse = lydkilde.getUdsendelse();
      remoteViews.setTextViewText(R.id.titel, udsendelse == null ? k.navn : udsendelse.titel);
      remoteViews.setTextViewText(R.id.metainformation, udsendelse == null ? k.navn : udsendelse.titel);
      metainfo = k.navn ;
      //App.kortToast(" Udsendelse "  + udsendelse == null ? k.navn : udsendelse.titel);
    }    
        
    
    switch (status) {
      case STOPPET:    	
    	  statusInt = 1;
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.afspiller_spil);
        remoteViews.setViewVisibility(R.id.progressBar, View.INVISIBLE);
        remoteViews.setTextViewText(R.id.metainformation, k.navn);
        remoteViews.setTextColor(R.id.metainformation, App.color.grå40);
        break;
      case FORBINDER:
    	remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.afspiller_pause);
        remoteViews.setViewVisibility(R.id.progressBar, View.VISIBLE);
        int fpct = DRData.instans.afspiller.getForbinderProcent();
         //remoteViews.setTextViewText(R.id.metainformation, "Forbinder " + (fpct > 0 ? fpct : ""));
        remoteViews.setTextColor(R.id.metainformation, App.color.blå);
        remoteViews.setTextViewText(R.id.metainformation, metainfo);        
        break;
      case SPILLER:
    	  statusInt = 2;
    	//  App.kortToast("SPILLER " + k.navn);
        remoteViews.setImageViewResource(R.id.startStopKnap, R.drawable.afspiller_pause);
        remoteViews.setViewVisibility(R.id.progressBar, View.INVISIBLE);
        //metainformation.setTextColor(getResources().getColor(R.color.blå));
        remoteViews.setViewVisibility(R.id.metainformation, View.VISIBLE);
        remoteViews.setTextColor(R.id.metainformation, App.color.blå);
        remoteViews.setTextViewText(R.id.metainformation, k.navn);          
        break;
    }
    
    afspillerReceiverI.putExtra("flag", statusInt);
    PendingIntent startStopPI = PendingIntent.getBroadcast(App.instans, 0,afspillerReceiverI , PendingIntent.FLAG_UPDATE_CURRENT);
    try {
		startStopPI.send();
	} catch (CanceledException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    remoteViews.setOnClickPendingIntent(R.id.startStopKnap, startStopPI);
    
    
    
    return remoteViews;
  }
}
