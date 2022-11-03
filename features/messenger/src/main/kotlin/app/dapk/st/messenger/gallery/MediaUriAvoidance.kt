package app.dapk.st.messenger.gallery

import android.net.Uri

class MediaUriAvoidance(
    val uriAppender: (Uri, Long) -> Uri,
    val externalContentUri: Uri,
)