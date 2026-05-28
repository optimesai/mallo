package com.ssafy.demo_app.domain.partner.entity;

import com.ssafy.demo_app.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "partner_master")
@Getter
@Setter
public class PartnerMaster extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Integer partnerId;

    @Column(name = "partner_code", nullable = false, unique = true)
    private String partnerCode;

    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false)
    private PartnerType partnerType;

    @Column(name = "business_no")
    private String businessNo;

    @Column(name = "representative")
    private String representative;

    @Column(name = "contact_phone")
    private String contactPhone;

    public enum PartnerType {
        SUPPLIER,
        CUSTOMER
    }
}
