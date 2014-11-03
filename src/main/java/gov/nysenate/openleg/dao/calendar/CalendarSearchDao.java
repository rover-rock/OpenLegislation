package gov.nysenate.openleg.dao.calendar;

import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.model.calendar.Calendar;
import gov.nysenate.openleg.model.calendar.CalendarActiveListId;
import gov.nysenate.openleg.model.calendar.CalendarId;
import gov.nysenate.openleg.model.calendar.CalendarSupplementalId;
import gov.nysenate.openleg.model.search.SearchResults;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.Collection;

public interface CalendarSearchDao {

    /**
     * Performs a calendar search based on the given query string.
     * Results are sorted and curtailed according to the given sort string and limit offset.
     *
     * @param query
     * @param sort
     * @param limitOffset
     * @return
     */
    public SearchResults<CalendarId> searchCalendars(QueryBuilder query, String sort, LimitOffset limitOffset);

    /**
     * Performs a calendar active list search based on the given query string.
     * Results are sorted and curtailed according to the given sort string and limit offset.
     *
     * @param query
     * @param sort
     * @param limitOffset
     * @return
     */
    public SearchResults<CalendarActiveListId> searchActiveLists(QueryBuilder query, String sort, LimitOffset limitOffset);

    /**
     * Performs a floor calendar search based on the given query string.
     * Results are sorted and curtailed according to the given sort string and limit offset.
     *
     * @param query
     * @param sort
     * @param limitOffset
     * @return
     */
    public SearchResults<CalendarSupplementalId> searchFloorCalendars(QueryBuilder query, String sort, LimitOffset limitOffset);

    /**
     * Updates or inserts a single calendar into the index
     *
     * @param calendar
     */
    public void updateCalendarIndex(Calendar calendar);

    /**
     * Performs a bulk update/insert into the index for a collection of calendars
     *
     * @param calendars
     */
    public void updateCalendarIndexBulk(Collection<Calendar> calendars);

    /**
     * Deletes a calendar in the index corresponding to the given calendar id
     *
     * @param calId
     */
    public void deleteCalendarFromIndex(CalendarId calId);

    /**
     * Removes the calendar index and all indexed documents
     */
    public void deleteCalendarIndex();
}
