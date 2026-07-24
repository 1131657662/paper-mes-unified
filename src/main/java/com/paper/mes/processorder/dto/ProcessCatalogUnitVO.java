package com.paper.mes.processorder.dto;

/** A measurement unit allowed by a process catalog entry. */
public record ProcessCatalogUnitVO(String code, String name, boolean defaultUnit) { }
