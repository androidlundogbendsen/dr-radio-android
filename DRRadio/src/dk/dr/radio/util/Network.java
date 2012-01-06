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

package dk.dr.radio.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Network
{
	public static boolean testConnection( Context c )
	{
		// get the networkinfo from the connection manager
		ConnectivityManager cm = (ConnectivityManager) c.getSystemService( Context.CONNECTIVITY_SERVICE ) ;

		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for( NetworkInfo ni : netInfo )
		{
			// if we find something, return true if it's connected
			if( ni.getTypeName().equalsIgnoreCase("WIFI") || ni.getTypeName().equalsIgnoreCase("MOBILE") )
			{
				if( ni.isConnected() )
				{
					return true ;
				}
			}
		}
		return false ;
	}
}
