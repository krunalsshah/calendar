# My Calendar
Simple event calendar, with agenda view.

## Requirements
* Android SDK 23
* Android SDK Tools 25.0.8
* Android SDK Build-tools 23.0.2
* Android Support Library 23.2.1

## Build & Test

**Build**

    ./gradlew :app:assembleDebug

**Test & Coverage**

    ./gradlew :app:lintDebug
    ./gradlew :app:testDebug
    ./gradlew :app:jacocoTestCoverage

## Discussions:
Scope and conventions:
* Assume local time zone (device time zone) for UI display
* No recurring events supported
* No attendees information
* Events can span multiple days
* All day events should end at midnight the next day, or any day after that, e.g.:
  * Mon 12:00 AM - Tue 12:00 AM for 1-day event
  * Mon 12:00 AM - Thu 12:AM for multi-day event
* A local calendar will be created if none exists, no functionality to edit or add calendars
* Simple coloring of events based on their calendar ID, from a pool of predefined colors

Challenges:

Number of items in list view (agenda view) and grid view (calendar view) is essentially unlimited, as time has no limit on past or future
* A naive solution of continuously adding dates on the fly would be inefficient, given limited resources in mobile devices
* We should come up with some sort of recycling and limit 'active' dates to a fixed number

Choice of widget:

* `android.widget.CalendarView`:
  * included in SDK
  * no event indicator
  * no month change listener
  * no state restoration
  * inconsistent look and feel across API levels
* custom `EventCalendarView`:
  * use a `RecyclerView` with `GridLayoutManager` (or `GridView`) to display a grid of days in month
  * use a `ViewPager` to allow swiping between months, make it 'circular' to minimize pages required
  * override `ViewPager.onMeasure()` to allow its height to wrap content
* `AgendaView`: a `RecyclerView` that automatically prunes its dataset once it hits a set limit

Widget implementation:

* Widgets have default adapters that serve to bind UI shells, and data adapters that supply actual data via `Cursor` to bind to those shells
* Widgets handle UI events such as scrolling, swiping by themselves and notify external parties of changes
* `Activity` supply data to widget via `Cursor`, and listens to UI events to coordinate widgets
* `Cursor` should be deactivated/closed as they are swapped

Widgets arrangement consideration:

To support different device sizes and orientations, the following arrangements are made:

* Portrait: more vertical space
  * Stack calendar view and agenda view
* Landscape: limited vertical space
  * For smaller devices, moderate horizontal space: overlap calendar view and agenda view. In this case the calendar view becomes more of a 'pop-up' picker
  * For larger devices e.g. tablets, more horizontal space: put calendar view and agenda view side-by-side

Data source:

* Use Calendar Provider, required API 14
* Pros:
  * Zero setup efforts needed for local storage
  * Can sync with other calendar apps that make use of the same provider
  * Work with a `SyncAdapter` to periodically sync to a remote data source
  * Inherit a well-defined model for calendars, events, attendees etc
* Cons:
  * Require permissions from API 23
  * Inflexible content observation, e.g. unable to set custom notification URI for content changes
  * Become much more complex when joining multiple tables, e.g. calendars, events, attendees
  * Tricky handling of all-day events, which are stored in UTC
* Some ideas (not implemented):
  * Create a custom content provider that wrap Calendar Provider
  * Abstract complex join queries, by creating SQL Views that get updated when underlying data from Calendar Provider change
  * Design a schema that is more tailored to UI design requirements

Testing: Chose to use Robolectric as I am already familiar and comfortable with it.