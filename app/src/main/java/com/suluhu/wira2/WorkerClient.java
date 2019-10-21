package com.suluhu.wira2;


import android.app.Application;

import com.suluhu.wira2.models.Worker;

public class WorkerClient extends Application {

    private Worker worker = null;

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }
}
