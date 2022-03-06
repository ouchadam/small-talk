package internalfixture

import app.dapk.st.settings.SettingItem

internal fun aSettingTextItem(
    id: SettingItem.Id = SettingItem.Id.Ignored,
    content: String = "text-content",
    subtitle: String? = null
) = SettingItem.Text(id, content, subtitle)

internal fun aSettingHeaderItem(
    label: String = "header-label",
) = SettingItem.Header(label)