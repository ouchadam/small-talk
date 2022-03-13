package app.dapk.st.profile

import app.dapk.st.core.Lce
import app.dapk.st.design.components.Route
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.room.ProfileService

data class ProfileScreenState(
    val page: SpiderPage<out Page>,
)

sealed interface Page {
    data class Profile(val content: Lce<Content>) : Page {
        data class Content(
            val me: ProfileService.Me,
            val invitationsCount: Int,
        )
    }

    data class Invitations(val content: Lce<List<RoomId>>): Page

    object Routes {
        val profile = Route<Profile>("Profile")
        val invitation = Route<Invitations>("Invitations")
    }
}

sealed interface ProfileEvent {

}

