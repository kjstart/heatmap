package com.com.demo.heatmap.entity;

import java.time.Instant;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "PAGE_EVENTS")
public class PageEvent {
	@SequenceGenerator(name = "alias_for_pageevent_sequence", sequenceName = "PAGEEVENT_ID_SEQ", initialValue = 1, allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alias_for_pageevent_sequence")
	@Id
	private long id;

	@Column(nullable = false, name = "USER_ID")
	private String userId;
	@Column(nullable = false, name = "URL_HASH")
	private String urlHash;
	@Column(nullable = false, name = "EVENT_TYPE")
	private String eventType;
	@Column(nullable = true, name = "PAGE_SECTION")
	private Integer pageSection;
	@Column(nullable = true, name = "STAY_TIME")
	private Integer stayTime;
	@Column(nullable = true, name = "CURSOR_X")
	private Float cursorX;
	@Column(nullable = true, name = "CURSOR_Y")
	private Float cursorY;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, name = "CREATED_AT")
	private Date createdAt = Date.from(Instant.now());

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUrlHash() {
		return urlHash;
	}

	public void setUrlHash(String urlHash) {
		this.urlHash = urlHash;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public Integer getPageSection() {
		return pageSection;
	}

	public void setPageSection(Integer pageSection) {
		this.pageSection = pageSection;
	}

	public Integer getStayTime() {
		return stayTime;
	}

	public void setStayTime(Integer stayTime) {
		this.stayTime = stayTime;
	}

	public Float getCursorX() {
		return cursorX;
	}

	public void setCursorX(Float cursorX) {
		this.cursorX = cursorX;
	}

	public Float getCursorY() {
		return cursorY;
	}

	public void setCursorY(Float cursorY) {
		this.cursorY = cursorY;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
}
