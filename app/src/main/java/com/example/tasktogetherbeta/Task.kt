package com.example.tasktogetherbeta

import android.os.Parcel
import android.os.Parcelable

data class Task(
    val taskId: String? = null,
    val title: String = "",
    val description: String? = null,
    val dueDate: String = "",
    val priority: String = "",
    val createdBy: String = "",
    val completedBy: String = "",
    val completedOn: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(taskId)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(dueDate)
        parcel.writeString(priority)
        parcel.writeString(createdBy)
        parcel.writeString(completedBy)
        parcel.writeString(completedOn)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Task> {
        override fun createFromParcel(parcel: Parcel): Task {
            return Task(parcel)
        }

        override fun newArray(size: Int): Array<Task?> {
            return arrayOfNulls(size)
        }
    }
}