package com.lakesidemutual.policyexpirationconstraints;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Date;

/**
 * InsuranceQuoteExpiredEvent is a domain event class that is used to notify the Customer Self-Service Backend
 * when the Insurance Quote for a specific Insurance Quote Request has expired.
 * */
public class InsuranceQuoteExpiredEvent extends InsuranceQuoteEvent {
	private Date date;

	@JsonProperty("$type")
	private final String type = "InsuranceQuoteExpiredEvent";

	public InsuranceQuoteExpiredEvent() {
	}

	public InsuranceQuoteExpiredEvent(Date date, Long insuranceQuoteRequestId) {
		super(insuranceQuoteRequestId);
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}