package ru.ximen.mesh;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.ximen.meshstack.MeshCompositionData;

public class MeshConfigurationDialog extends DialogFragment {
    TabLayout tabLayout;
    ViewPager viewPager;

    public class CustomAdapter extends FragmentPagerAdapter {
        List<Fragment> mFragmentCollection = new ArrayList<>();
        List<String> mTitleCollection = new ArrayList<>();
        public CustomAdapter(FragmentManager fm) {
            super(fm);
        }
        public void addFragment(String title, Fragment fragment)
        {
            mTitleCollection.add(title);
            mFragmentCollection.add(fragment);
        }
        //Needed for
        @Override
        public CharSequence getPageTitle(int position) {
            return mTitleCollection.get(position);
        }
        @Override
        public Fragment getItem(int position) {
            return mFragmentCollection.get(position);
        }
        @Override
        public int getCount() {
            return mFragmentCollection.size();
        }
    }

    public static class CustomFragment extends Fragment {
        private String mType = "";
        private byte[] mData;
        public static CustomFragment createInstance(String type, byte[] composition){
            CustomFragment fragment = new CustomFragment();
            fragment.mType = type;
            fragment.mData = composition;
            return fragment;
        }
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container,
                                 Bundle savedInstanceState) {
            MeshCompositionData composition = new MeshCompositionData(mData);
            View v = null;
            switch (mType){
                case("Info"):
                    v = inflater.inflate(R.layout.frag_info, container,false);
                    ((TextView) v.findViewById(R.id.textPID1)).setText(String.valueOf(composition.getPID()));
                    ((TextView) v.findViewById(R.id.textCID1)).setText(String.valueOf(composition.getCID()));
                    ((TextView) v.findViewById(R.id.textVID1)).setText(String.valueOf(composition.getVID()));
                    ((TextView) v.findViewById(R.id.textCRPL1)).setText(String.valueOf(composition.getCRPL()));
                    break;
                case("Configuration"):
                    v = inflater.inflate(R.layout.frag_conf,container,false);
                    ((Switch) v.findViewById(R.id.switchRelay)).setChecked(composition.isRelay());
                    ((Switch) v.findViewById(R.id.switchProxy)).setChecked(composition.isProxy());
                    ((Switch) v.findViewById(R.id.switchFriend)).setChecked(composition.isFriend());
                    ((Switch) v.findViewById(R.id.switchLowPower)).setChecked(composition.isLowPower());
                    break;
            }
            return v;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.dlg_conf,container,false);
        tabLayout = rootview.findViewById(R.id.tabLayout);
        viewPager = rootview.findViewById(R.id.masterViewPager);
        CustomAdapter adapter = new CustomAdapter(getChildFragmentManager());
        adapter.addFragment("Configuration",CustomFragment.createInstance("Configuration", getArguments().getByteArray("composition")));
        adapter.addFragment("Info",CustomFragment.createInstance("Info", getArguments().getByteArray("composition")));
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        return rootview;
    }
}
