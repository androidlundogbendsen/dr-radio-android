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

package dk.dr.radio.data.json.spiller_nu;

import java.util.ArrayList;
import java.util.List;

public class SpillerNu implements java.io.Serializable {
  // Vigtigt: Sæt IKKE versionsnummer så objekt kan læses selvom klassen er ændret. Den bør i stedet indlæses fra server igen
	// private static final long serialVersionUID = 12345;

	public List<SpillerNuElement> liste = new ArrayList<SpillerNuElement>();

}
