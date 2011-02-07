package gov.nysenate.openleg.model.calendar;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("section")

public class Section {

	private String id;
	
	private String name;
	
	private String type;
	
	private String cd;

	private Supplemental supplemental;
	
	private List<CalendarEntry> calendarEntries;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@JsonIgnore
	public Supplemental getSupplemental() {
		return supplemental;
	}

	public void setSupplemental(Supplemental supplemental) {
		this.supplemental = supplemental;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCd() {
		return cd;
	}

	public void setCd(String cd) {
		this.cd = cd;
	}

	public List<CalendarEntry> getCalendarEntries() {
		return calendarEntries;
	}

	public void setCalendarEntries(List<CalendarEntry> calendarEntries) {
		this.calendarEntries = calendarEntries;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (obj != null && obj instanceof Section)
		{
			if ( ((Section)obj).getId().equals(this.getId()))
				return true;
		}
		
		return false;
	}


	
}
