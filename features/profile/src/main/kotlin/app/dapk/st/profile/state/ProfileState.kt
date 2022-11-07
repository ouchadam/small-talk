package app.dapk.st.profile.state

import app.dapk.st.core.Lce
import app.dapk.st.core.State
import app.dapk.st.core.page.PageContainer
import app.dapk.st.design.components.Route
import app.dapk.st.engine.Me
import app.dapk.st.engine.RoomInvite
import app.dapk.state.Combined2

typealias ProfileState = State<Combined2<PageContainer<Page>, Unit>, Unit>

sealed interface Page {
    data class Profile(val content: Lce<Content>) : Page {
        data class Content(
            val me: Me,
            val invitationsCount: Int,
        )
    }

    data class Invitations(val content: Lce<List<RoomInvite>>) : Page

    object Routes {
        val profile = Route<Profile>("Profile")
        val invitation = Route<Invitations>("Invitations")
    }
}
