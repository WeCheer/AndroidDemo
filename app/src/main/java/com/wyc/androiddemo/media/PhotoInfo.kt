package com.wyc.androiddemo.media

import android.os.Parcel
import android.os.Parcelable

/**
 *作者： wyc
 * <p>
 * 创建时间： 2021/1/12 17:03
 * <p>
 * 文件名字： com.wyc.androiddemo.media
 * <p>
 * 类的介绍：
 */
data class PhotoInfo(
        var fNumber: String?,
        var dateTime: String?,
        var exposureTime: String?,
        var flash: String?,
        var focalLength: String?,
        var imageLength: String?,
        var imageWidth: String?,
        var iSOSpeedRatings: String?,
        var make: String?,
        var model: String?,
        var orientation: String?,
        var whiteBalance: String?,
        var latitude: Float,
        var longitude: Float) : Parcelable {

    constructor() : this("", "", "",
            "", "", "", "",
            "", "", "", "",
            "", 0f, 0f)

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readFloat(),
            parcel.readFloat()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fNumber)
        parcel.writeString(dateTime)
        parcel.writeString(exposureTime)
        parcel.writeString(flash)
        parcel.writeString(focalLength)
        parcel.writeString(imageLength)
        parcel.writeString(imageWidth)
        parcel.writeString(iSOSpeedRatings)
        parcel.writeString(make)
        parcel.writeString(model)
        parcel.writeString(orientation)
        parcel.writeString(whiteBalance)
        parcel.writeFloat(longitude)
        parcel.writeFloat(latitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "fNumber（光圈值）=$fNumber, " +
                "dateTime（拍摄时间）=$dateTime, " +
                "exposureTime（曝光时间）=$exposureTime, " +
                "flash（闪光灯）=$flash, " +
                "focalLength（焦距）=$focalLength, " +
                "imageLength（图片高度）=$imageLength, " +
                "imageWidth（图片宽度）=$imageWidth, " +
                "iSOSpeedRatings（ISO）=$iSOSpeedRatings, " +
                "make=（设备品牌）$make, " +
                "model（设备型号）=$model, " +
                "orientation（旋转角度）=$orientation, " +
                "whiteBalance（白平衡）=$whiteBalance, " +
                "latitude（维度）=$latitude, " +
                "longitude（经度）=$longitude"
    }


    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<PhotoInfo> {
            override fun createFromParcel(parcel: Parcel): PhotoInfo {
                return PhotoInfo(parcel)
            }

            override fun newArray(size: Int): Array<PhotoInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}
