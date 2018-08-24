package ru.ximen.mesh;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> application = new MutableLiveData<String>();

    public void createApplication(String name){ application.setValue(name); }
    public LiveData<String> creatingApplication(){
        return application;
    }
}
