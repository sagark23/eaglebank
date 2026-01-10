package com.eaglebank.dto.request;

import com.eaglebank.domain.Address;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AddressRequest(
        @NotBlank(message = "Address line 1 is required")
        String line1,

        String line2,

        String line3,

        @NotBlank(message = "Town is required")
        String town,

        @NotBlank(message = "County is required")
        String county,

        @NotBlank(message = "Postcode is required")
        String postcode
) {
    public Address toEntity() {
        return Address.builder()
                .line1(this.line1)
                .line2(this.line2)
                .line3(this.line3)
                .town(this.town)
                .county(this.county)
                .postcode(this.postcode)
                .build();
    }
}

