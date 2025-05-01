package io.github.vishalmysore;

import com.t4a.detect.ActionCallback;
import com.t4a.detect.ActionState;

public class CustomTaskCallback implements ActionCallback {
    private String status;
    private Object context;

    @Override
    public void setContext(Object obj) {
       this.context = obj;
    }

    @Override
    public Object getContext() {
        return context;
    }

    @Override
    public void sendtStatus(String status, ActionState state) {

    }
}
