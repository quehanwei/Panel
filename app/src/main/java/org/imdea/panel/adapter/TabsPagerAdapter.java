package org.imdea.panel.adapter;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.imdea.panel.GeneralFragment;
import org.imdea.panel.TagsFragment;

public class TabsPagerAdapter extends FragmentPagerAdapter {

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                return new GeneralFragment();
            case 1:
                return new TagsFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        // get item count - equal to number of tabs
        return 2;
    }

}