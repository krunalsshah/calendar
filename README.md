# My Calendar
Simple event calendar, with agenda view.

## Requirements
* Android SDK 23
* Android SDK Tools 25.0.8
* Android SDK Build-tools 23.0.3
* Android Support Library 23.2.1

## Build & Test

**Build**

    ./gradlew :app:assembleDebug

**Test & Coverage**

    ./gradlew :app:lintDebug
    ./gradlew :app:testDebug
    ./gradlew :app:jacocoTestCoverage

## Screenshots

<img src="screenshot/1.png" width="200px" />
<img src="screenshot/2.png" width="200px" />
<img src="screenshot/3.png" width="200px" />
<img src="screenshot/4.png" width="600px" />
<img src="screenshot/5.png" width="600px" />

## Discussions
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
* Only sync weather for today and tomorrow (from forecast.io)
  * Sync once every 24h
  * Rely on last known location for simplicity
  * Cache using `SharedPreferences` for simplicity

Custom views:
* `EventCalendarView`: custom calendar view - extends `ViewPager`
  * mimic the look and feel of `android.widget.CalendarView`
  * each page is a custom `RecyclerView` view with a `GridLayoutManager`
  * shuffle and reuse pages as users swipe to minimize number of pages in memory
  * provide APIs for `android.provider.CalendarContract.Events` cursor binding for each page
  * provide APIs for controlling UI and interaction callbacks
* `AgendaView`: custom agenda view - extends `RecyclerView`
  * automatically prepend/append rows via scroll events
  * automatically prune adapter items to keep its size from growing infinitely
  * provide APIs for `android.provider.CalendarContract.Events` cursor binding for each row
  * provide APIs for controlling UI and interaction callbacks
* `EventEditView`: custom form view to create/edit event
* `CalendarSelectionView`: custom list view for calendar selection
* Content provider operations and coordination among views are handled by `Activity`

Layout arrangement:
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
  * Inflexible content observation, e.g. unable to set custom notification URI for individual observations
  * Become much more complex when having to join multiple tables, e.g. calendars, events, attendees
  * Tricky handling of all-day events, which are stored in UTC
* Some ideas (not implemented):
  * Create a custom content provider that wrap Calendar Provider
  * Abstract complex join queries, by creating SQL Views that get updated when underlying data from Calendar Provider change
  * Design a schema that is more tailored to UI design requirements

Testing:
* Chose Robolectric as I am already familiar and comfortable with it.
* Relax visibilities of several fields for testing convenience. These can be avoided with dependency injection but I just keep it simple here.

*Weather icons are from Meteocons set by Alessio Atzeni*