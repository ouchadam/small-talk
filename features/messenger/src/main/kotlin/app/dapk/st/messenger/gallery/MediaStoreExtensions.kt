package app.dapk.st.messenger.gallery

import android.os.Build
import android.provider.MediaStore

fun isNotPending() = if (Build.VERSION.SDK_INT <= 28) MediaStore.Images.Media.DATA + " NOT NULL" else MediaStore.MediaColumns.IS_PENDING + " != 1"
