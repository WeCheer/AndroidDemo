package com.wyc.androiddemo.media

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 *作者： wyc
 * <p>
 * 创建时间： 2021/1/12 11:31
 * <p>
 * 文件名字： com.wyc.androiddemo.media
 * <p>
 * 类的介绍：
 */
data class MediaInfo(
        var index: Long,
        var title: String?,
        var filePath: String?,
        var mimeType: String?,
        var displayName: String?,
        var size: Long,
        var dirName: String?,
        var dateModified: Long,
        var duration: Long,
        var uri: Uri?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readParcelable(Uri::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(index)
        parcel.writeString(title)
        parcel.writeString(filePath)
        parcel.writeString(mimeType)
        parcel.writeString(displayName)
        parcel.writeLong(size)
        parcel.writeString(dirName)
        parcel.writeLong(dateModified)
        parcel.writeLong(duration)
        parcel.writeParcelable(uri, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<MediaInfo> {
            override fun createFromParcel(parcel: Parcel): MediaInfo {
                return MediaInfo(parcel)
            }

            override fun newArray(size: Int): Array<MediaInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}