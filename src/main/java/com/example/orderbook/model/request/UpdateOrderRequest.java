package com.example.orderbook.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.io.Serializable;

/**
 * Request Object for accepting orders updation
 */
public class UpdateOrderRequest implements Serializable {
	private static final long serialVersionUID = 8822833371248140397L;

	@NotNull(message = "orderId is mandatory")
	@PositiveOrZero
	private Long orderId;

	@NotNull(message = "units is mandatory")
	@Min(1)
	private Integer units;

	private Double value;

	public UpdateOrderRequest(@JsonProperty("orderId") Long orderId,
						@JsonProperty("units") Integer units,
						@JsonProperty("value") Double value){

		this.orderId = orderId;
		this.units = units;
		this.value = value;
	}


	public Long getOrderId() {
		return orderId;
	}


	public Integer getUnits() {
			return units;
	}

	public void setUnits(Integer units) {
			this.units = units;
	}


	public Double getValue() {
		return value;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((orderId == null) ? 0 : orderId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateOrderRequest other = (UpdateOrderRequest) obj;
		if (orderId == null) {
			if (other.orderId != null)
				return false;
		} else if (!orderId.equals(other.orderId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ORDERID="+ orderId
				+ " UNITS=" + units + " VALUE=" + value;
	}

}
