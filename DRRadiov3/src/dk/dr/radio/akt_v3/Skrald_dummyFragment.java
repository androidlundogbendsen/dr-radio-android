package dk.dr.radio.akt_v3;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import dk.dr.radio.v3.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class Skrald_dummyFragment extends Fragment {
  /**
   * The fragment argument representing the section number for this
   * fragment.
   */
  private static final String ARG_SECTION_NUMBER = "section_number";

  /**
   * Returns a new instance of this fragment for the given section
   * number.
   */
  public static Skrald_dummyFragment newInstance(int sectionNumber) {
    Skrald_dummyFragment fragment = new Skrald_dummyFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }

  public Skrald_dummyFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.skrald_dummy_frag, container, false);
    TextView textView = (TextView) rootView.findViewById(R.id.section_label);
    textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
    return rootView;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ((Navigation_akt) activity).sætTitel("dummy" + getArguments().getInt(ARG_SECTION_NUMBER));
  }
}
