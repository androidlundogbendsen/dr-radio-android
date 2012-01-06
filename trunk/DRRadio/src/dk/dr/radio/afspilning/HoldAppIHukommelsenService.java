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
package dk.dr.radio.afspilning;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import dk.dr.radio.util.Log;

/**
 * Sørger for at app'en holdes i hukommelsen
 * @author j
 */
public class HoldAppIHukommelsenService extends Service {
	/** Service-mekanik. Ligegyldig da vi kører i samme proces. */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.d("AfspillerService onCreate!");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("AfspillerService onStartCommand(" + intent + " " + flags + " " + startId);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d("AfspillerService onDestroy!");
	}
}
