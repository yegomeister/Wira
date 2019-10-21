package com.suluhu.wira2.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Worker implements Parcelable {

    private String first_name;
    private String last_name;
    private String phone_number;
    private String date_of_birth;
    private String status;
    private String user_image_url;
    private String user_id;

    public Worker(String first_name, String last_name, String phone_number, String date_of_birth, String status, String user_image_url, String user_id) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.phone_number = phone_number;
        this.date_of_birth = date_of_birth;
        this.status = status;
        this.user_image_url = user_image_url;
        this.user_id = user_id;
    }
    public Worker() {

    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getDate_of_birth() {
        return date_of_birth;
    }

    public void setDate_of_birth(String date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUser_image_url() {
        return user_image_url;
    }

    public void setUser_image_url(String user_image_url) {
        this.user_image_url = user_image_url;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public static Creator<Worker> getCREATOR() {
        return CREATOR;
    }

    protected Worker(Parcel in) {
        first_name = in.readString();
        last_name = in.readString();
        phone_number = in.readString();
        date_of_birth = in.readString();
        status = in.readString();
        user_image_url = in.readString();
        user_id = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(first_name);
        dest.writeString(last_name);
        dest.writeString(phone_number);
        dest.writeString(date_of_birth);
        dest.writeString(status);
        dest.writeString(user_image_url);
        dest.writeString(user_id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Worker> CREATOR = new Creator<Worker>() {
        @Override
        public Worker createFromParcel(Parcel in) {
            return new Worker(in);
        }

        @Override
        public Worker[] newArray(int size) {
            return new Worker[size];
        }
    };
}
