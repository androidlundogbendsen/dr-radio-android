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

/**
 *
 * @author j
 */

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

// Gammelt regneark
// @ReportsCrashes(formKey = "dDVyUzgzX1Bfb3dJV0ZBMUx0akRQR3c6MQ")



// BugSense
@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=57c90f98", formKey="")

// Regneark 2.1.x
//@ReportsCrashes(formKey = "dHItUlZ0eDRzZU5WMVFfYmZJZ1FId3c6MQ")
public class AcraApplication extends Application {

  @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();

/* TODO - noget a la det her ind i stedet for
				tjek for initialisering i hver aktivitet/service/...
		try {
			drdata = DRData.tjekInstansIndlæst(this);
		} catch (Exception ex) {
			// TODO popop-advarsel til bruger om intern fejl og rapporter til udvikler-dialog
			Log.kritiskFejl(this, ex);
			return;
		}
*/
    }

}
