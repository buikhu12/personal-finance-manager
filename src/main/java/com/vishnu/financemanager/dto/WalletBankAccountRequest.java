package com.vishnu.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WalletBankAccountRequest(
        @NotBlank(message = "Vui lòng nhập tên ngân hàng.")
        @Size(max = 100, message = "Tên ngân hàng tối đa 100 ký tự.")
        String bankName,

        @NotBlank(message = "Vui lòng nhập 4 số cuối tài khoản.")
        @Pattern(regexp = "[0-9]{4}", message = "Vui lòng nhập đúng 4 số cuối tài khoản.")
        String lastFour
) {
}
