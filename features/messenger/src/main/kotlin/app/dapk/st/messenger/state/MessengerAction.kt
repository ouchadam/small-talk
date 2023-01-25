package app.dapk.st.messenger.state

import app.dapk.st.design.components.BubbleModel
import app.dapk.st.engine.MessengerPageState
import app.dapk.st.engine.RoomEvent
import app.dapk.st.navigator.MessageAttachment
import app.dapk.state.Action

sealed interface ScreenAction : Action {
    data class CopyToClipboard(val model: BubbleModel) : ScreenAction
    object SendMessage : ScreenAction
    object OpenGalleryPicker : ScreenAction
    object LeaveRoom : ScreenAction

    sealed interface MuteNotifications : ScreenAction {
        object Mute : MuteNotifications
        object Unmute : MuteNotifications
    }

    sealed interface ChatBubble : ScreenAction {
        object Enable: ChatBubble
        object Disable: ChatBubble
    }

    sealed interface LeaveRoomConfirmation : ScreenAction {
        object Confirm : LeaveRoomConfirmation
        object Deny : LeaveRoomConfirmation
    }

    data class UpdateDialogState(val dialogState: DialogState?) : ScreenAction
}

sealed interface ComponentLifecycle : Action {
    object Visible : ComponentLifecycle
    object Gone : ComponentLifecycle
}

sealed interface MessagesStateChange : Action {
    data class Content(val content: MessengerPageState) : MessagesStateChange
    data class MuteContent(val isMuted: Boolean) : MessagesStateChange
    data class ChatBubbleContent(val isChatBubble: Boolean) : MessagesStateChange
}

sealed interface ComposerStateChange : Action {
    data class SelectAttachmentToSend(val newValue: MessageAttachment) : ComposerStateChange
    data class TextUpdate(val newValue: String) : ComposerStateChange
    object Clear : ComposerStateChange

    sealed interface ReplyMode : ComposerStateChange {
        data class Enter(val replyingTo: RoomEvent) : ReplyMode
        object Exit : ReplyMode
    }

    sealed interface ImagePreview : ComposerStateChange {
        data class Show(val image: BubbleModel.Image) : ImagePreview
        object Hide : ImagePreview
    }
}