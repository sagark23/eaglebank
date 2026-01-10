package com.eaglebank.dto.response;

import com.eaglebank.domain.Address;
import lombok.Builder;

@Builder
public record AddressResponse(
        String line1,
        String line2,
        String line3,
        String town,
        String county,
        String postcode
) {
    public static AddressResponse from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getLine1(),
                address.getLine2(),
                address.getLine3(),
                address.getTown(),
                address.getCounty(),
                address.getPostcode()
        );
    }
}

