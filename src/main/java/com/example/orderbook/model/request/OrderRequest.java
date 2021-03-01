package com.example.orderbook.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;


/**
 * Request Object for accepting new buy/sell orders
 */
public class OrderRequest implements Serializable {
	private static final long serialVersionUID = 8822833371248140397L;

	private Long orderId;

	@NotNull(message = "clientId is mandatory")
	@NotBlank(message = "clientId is mandatory")
	private String clientId;

	@NotNull(message = "securityId is mandatory")
	@NotBlank(message = "securityId is mandatory")
	private String securityId;

	@NotNull(message = "units is mandatory")
	@Min(1)
	private Integer units;

	private Double value;

	@NotNull(message = "isBuying is mandatory, acceptable value is true or false")
	private Boolean isBuying;


	@NotNull(message = "orderType is mandatory, acceptable value is MARKET_ORDER or LIMIT_ORDER")
	private OrderType orderType;

	public OrderRequest(@JsonProperty("orderId") Long orderId, @JsonProperty("clientId") String clientId,
						@JsonProperty("securityId") String securityId, @JsonProperty("units") Integer units,
						@JsonProperty("value") Double value, @JsonProperty("isBuying") boolean isBuying,
						@JsonProperty("orderType") OrderType orderType){

		this.orderId = orderId;
		this.clientId = clientId;
		this.securityId = securityId;
		this.units = units;
		this.value = value;
		this.isBuying = isBuying;
		this.orderType = orderType;
	}


	public Long getOrderId() {
		return orderId;
	}

	public String getClientId() {
		return clientId;
	}

	public String getSecurityId() {
		return securityId;
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

	public boolean isBuying() {
		return isBuying;
	}

	public OrderType getOrderType() {
		return orderType;
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
		OrderRequest other = (OrderRequest) obj;
		if (orderId == null) {
			if (other.orderId != null)
				return false;
		} else if (!orderId.equals(other.orderId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ORDERID="+ orderId +" CLIENT=" + clientId + " SECURITY=" + securityId
				+ " UNITS=" + units + " VALUE=" + value + " ISBUYING="
				+ (isBuying? "YES":"NO") + " ORDERTYPE " + orderType ;
	}

}
