package internalfixture

import app.dapk.st.settings.SettingItem

internal fun aSettingTextItem(
    id: SettingItem.Id = SettingItem.Id.Ignored,
    content: String = "text-content",
    subtitle: String? = null,
    enabled: Boolean = true,
) = SettingItem.Text(id, content, subtitle, enabled)

internal fun aSettingHeaderItem(
    label: String = "header-label",
) = SettingItem.Header(label)