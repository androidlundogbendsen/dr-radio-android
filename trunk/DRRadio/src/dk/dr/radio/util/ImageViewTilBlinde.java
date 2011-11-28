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
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;

/**
 * Da setContentDescription() tilsyneladende bliver ignoreret på ImageViews
 * er vi nødt til at omdefinere dispatchPopulateAccessibilityEvent
 * for at få tekst læst højt.
 * TODO: Se om vi kan erstatte ImageView med en ImageButton i kanalvalgs
 * layout så vi kan droppe klassen her igen
 * @author j
 */
public class ImageViewTilBlinde extends ImageView {
    public ImageViewTilBlinde(Context context) {
        super(context);
    }

    public ImageViewTilBlinde(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageViewTilBlinde(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public String blindetekst;

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(blindetekst);
        return true;
    }

}
