package internalfixture

import app.dapk.st.settings.Page
import app.dapk.state.SpiderPage

internal fun aImportRoomKeysPage(
    state: Page.ImportRoomKey = Page.ImportRoomKey()
) = SpiderPage(
    route = Page.Routes.importRoomKeys,
    label = "Import room keys",
    parent = Page.Routes.encryption,
    state = state
)

internal fun aPushProvidersPage(
    state: Page.PushProviders = Page.PushProviders()
) = SpiderPage(
    route = Page.Routes.pushProviders,
    label = "Push providers",
    parent = Page.Routes.root,
    state = state
)
