package fixture

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.message.MessageService

fun aLocalEcho(
    eventId: EventId? = anEventId(),
    message: MessageService.Message = aTextMessage(),
    state: MessageService.LocalEcho.State = MessageService.LocalEcho.State.Sending,
) = MessageService.LocalEcho(eventId, message, state)
