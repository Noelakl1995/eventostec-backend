package com.eventostec.api.domain.event;

import java.util.Date;
import java.util.UUID;

import com.eventostec.api.domain.address.Address;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Table(name = "event")
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    @GeneratedValue
    @Id
    private UUID id;

    private String title;

    private String description;

    private String imgUrl;

    private String eventUrl;

    private Boolean remote;

    private Date date;

    @OneToOne(mappedBy = "event", cascade = jakarta.persistence.CascadeType.ALL)
    private Address address;

}
