package fake

import android.app.Person
import io.mockk.mockk

class FakePersonBuilder {
    val instance = mockk<Person.Builder>(relaxed = true)
}