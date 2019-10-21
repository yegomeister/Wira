package com.suluhu.wira2.models;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class WorkerLocation implements Parcelable {

    private Worker worker;
    private GeoPoint geo_point;
    private @ServerTimestamp Date timestamp;

    public WorkerLocation() {
        //empty constructor required
    }

    public WorkerLocation(Worker worker , GeoPoint geo_point, Date timestamp) {
        this.worker = worker;
        this.geo_point = geo_point;
        this.timestamp = timestamp;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public GeoPoint getGeo_point(){
        return geo_point;
    }

    public void setGeo_point(GeoPoint geo_point) {
        this.geo_point = geo_point;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public static Creator<WorkerLocation> getCREATOR() {
        return CREATOR;
    }

    public WorkerLocation(Parcel in) {
        worker = in.readParcelable(Worker.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeParcelable(worker, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WorkerLocation> CREATOR = new Creator<WorkerLocation>() {
        @Override
        public WorkerLocation createFromParcel(Parcel in) {
            return new WorkerLocation(in);
        }

        @Override
        public WorkerLocation[] newArray(int size) {
            return new WorkerLocation[size];
        }
    };
}
