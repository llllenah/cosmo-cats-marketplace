package com.cosmocats.marketplace.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A cat customer of the marketplace.
 */
@Entity
@Table(name = "cosmo_cat")
@Getter
@Setter
@NoArgsConstructor
public class CosmoCatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cosmo_cat_id_seq")
    @SequenceGenerator(name = "cosmo_cat_id_seq", sequenceName = "cosmo_cat_id_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID reference;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "home_planet", nullable = false, length = 100)
    private String homePlanet;
}
