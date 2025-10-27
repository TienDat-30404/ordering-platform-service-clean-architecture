package com.example.demo.application.dto.output;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailData {
    private Long id;
    private String name;
    private BigDecimal price;
   
}
