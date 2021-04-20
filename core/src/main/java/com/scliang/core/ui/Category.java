package com.scliang.core.ui;

import android.os.Parcel;
import android.os.Parcelable;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.Serializable;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/7.
 */
public class Category implements MultiItemEntity, Serializable, Parcelable {
    public static final int TYPE_MY             = 1;
    public static final int TYPE_OTHER          = 2;
    public static final int TYPE_MY_CATEGORY    = 3;
    public static final int TYPE_OTHER_CATEGORY = 4;
    public String title = "";
    public String titleCode = "";
    public int count = -1;
    public int itemType;

    public Category(String title, String titleCode) {
        this(TYPE_MY_CATEGORY, title, titleCode);
    }

    public Category(int itemType, String title, String titleCode) {
        this(itemType, title, titleCode, -1);
    }

    public Category(int itemType, String title, String titleCode, int count) {
        this.title = title;
        this.titleCode = titleCode;
        this.itemType = itemType;
        this.count = count;
    }

    protected Category(Parcel in) {
        title = in.readString();
        titleCode = in.readString();
        count = in.readInt();
        itemType = in.readInt();
    }

    public static final Creator<Category> CREATOR = new Creator<Category>() {
        @Override
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        @Override
        public Category[] newArray(int size) {
            return new Category[size];
        }
    };

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    @Override
    public int getItemType() {
        return itemType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(titleCode);
        dest.writeInt(count);
        dest.writeInt(itemType);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Category) {
            Category category = (Category) obj;
            return title.equals(category.title) &&
                    titleCode.equals(category.titleCode);
        }
        return false;
    }
}
